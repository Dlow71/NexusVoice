package com.nexusvoice.infrastructure.ai.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 硅基流动重排序模型适配器
 * 由于LangChain4j暂无原生Rerank支持，使用HTTP客户端直接调用API
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Slf4j
@Component
public class SiliconFlowRerankAdapter {
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public SiliconFlowRerankAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    /**
     * 调用重排序API
     * 
     * @param model AI模型配置
     * @param apiKey API密钥
     * @param query 查询文本
     * @param documents 文档列表
     * @param topN 返回前N个结果
     * @return 重排序结果
     */
    public RerankResponse.RerankResult[] rerank(AiModel model, AiApiKey apiKey, 
                                                  String query, List<String> documents, Integer topN) {
        return rerank(model, apiKey, query, documents, topN, null);
    }
    
    /**
     * 调用重排序API（带指令）
     * 
     * @param model AI模型配置
     * @param apiKey API密钥
     * @param query 查询文本
     * @param documents 文档列表
     * @param topN 返回前N个结果
     * @param instruction 重排序指令（仅Qwen模型支持）
     * @return 重排序结果
     */
    public RerankResponse.RerankResult[] rerank(AiModel model, AiApiKey apiKey, 
                                                  String query, List<String> documents, 
                                                  Integer topN, String instruction) {
        try {
            String baseUrl = apiKey.getBaseUrl() != null ? apiKey.getBaseUrl() : model.getDefaultBaseUrl();
            String url = baseUrl + "/rerank";
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model.getModelCode());
            requestBody.put("query", query);
            requestBody.put("documents", documents);
            if (topN != null && topN > 0) {
                requestBody.put("top_n", topN);
            }
            requestBody.put("return_documents", false); // 不返回文档内容，减少流量
            
            // 处理模型特定参数
            String modelCode = model.getModelCode().toLowerCase();
            
            // Qwen模型支持instruction
            if (modelCode.contains("qwen") && instruction != null && !instruction.isEmpty()) {
                requestBody.put("instruction", instruction);
            }
            
            // BGE和netease-youdao模型支持长文档分块
            if (modelCode.contains("bge-reranker") || modelCode.contains("bce-reranker")) {
                // 从配置中获取分块参数
                Map<String, Object> config = model.getConfigMap();
                if (config != null) {
                    if (config.containsKey("maxChunksPerDoc")) {
                        requestBody.put("max_chunks_per_doc", config.get("maxChunksPerDoc"));
                    }
                    if (config.containsKey("overlapTokens")) {
                        requestBody.put("overlap_tokens", config.get("overlapTokens"));
                    }
                }
            }
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            // 构建HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey.getApiKey())
                    .timeout(Duration.ofSeconds(model.getDefaultTimeoutSeconds() != null ? 
                            model.getDefaultTimeoutSeconds() : 60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            log.info("调用重排序API，模型：{}，查询：{}，文档数：{}", 
                    model.getModelCode(), query, documents.size());
            
            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                log.error("重排序API调用失败，状态码：{}，响应：{}", response.statusCode(), response.body());
                throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                        "重排序API调用失败，状态码：" + response.statusCode());
            }
            
            // 解析响应
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode resultsNode = root.get("results");
            
            if (resultsNode == null || !resultsNode.isArray()) {
                throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, "重排序API响应格式错误");
            }
            
            // 构建结果列表
            List<RerankResponse.RerankResult> results = new ArrayList<>();
            for (int i = 0; i < resultsNode.size(); i++) {
                JsonNode resultNode = resultsNode.get(i);
                RerankResponse.RerankResult result = RerankResponse.RerankResult.builder()
                        .index(resultNode.get("index").asInt())
                        .score(resultNode.get("relevance_score").asDouble())
                        .rank(i)
                        .document(documents.get(resultNode.get("index").asInt()))
                        .build();
                results.add(result);
            }
            
            log.info("重排序完成，返回{}个结果", results.size());
            
            return results.toArray(new RerankResponse.RerankResult[0]);
            
        } catch (Exception e) {
            log.error("重排序请求失败", e);
            throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, "重排序请求失败：" + e.getMessage());
        }
    }
    
    /**
     * 估算token数量
     */
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：平均3-4个字符一个token
        return (int) Math.ceil(text.length() / 3.5);
    }
}
