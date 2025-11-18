package com.nexusvoice.infrastructure.ai.model;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.infrastructure.ai.adapter.EmbeddingAdapter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 硅基流动向量模型适配器
 * 硅基流动使用OpenAI兼容API
 * 
 * <p>实现EmbeddingAdapter接口，支持：
 * - 单个文本向量化
 * - 批量文本向量化
 * - Qwen模型的自定义维度参数
 * </p>
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Slf4j
@Component
public class SiliconFlowEmbeddingAdapter implements EmbeddingAdapter {
    
    /**
     * 创建向量模型
     */
    public EmbeddingModel createEmbeddingModel(AiModel model, AiApiKey apiKey) {
        String baseUrl = apiKey.getBaseUrl() != null ? apiKey.getBaseUrl() : model.getDefaultBaseUrl();
        Integer timeout = model.getDefaultTimeoutSeconds() != null ? model.getDefaultTimeoutSeconds() : 60;
        
        log.info("创建硅基流动向量模型，模型：{}，BaseURL：{}", model.getModelCode(), baseUrl);
        
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey.getApiKey())
                .baseUrl(baseUrl)
                .modelName(model.getModelCode())
                .timeout(Duration.ofSeconds(timeout))
                .logRequests(false)
                .logResponses(false);
        
        // Qwen模型支持自定义dimensions参数
        if (model.getModelCode().toLowerCase().contains("qwen")) {
            var config = model.getConfigMap();
            if (config != null && config.containsKey("dimensions")) {
                Object dims = config.get("dimensions");
                if (dims instanceof Integer) {
                    builder.dimensions((Integer) dims);
                    log.info("为Qwen模型设置维度：{}", dims);
                }
            }
        }
        
        return builder.build();
    }
    
    /**
     * 实现EmbeddingAdapter接口：调用Embedding API生成向量
     */
    @Override
    public EmbeddingResponse embed(EmbeddingRequest request, AiModel model, AiApiKey apiKey) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 创建LangChain4j向量模型
            EmbeddingModel embeddingModel = createEmbeddingModel(model, apiKey);
            
            // 2. 调用API生成向量
            if (request.isSingleText()) {
                // 单个文本向量化
                Response<Embedding> response = embeddingModel.embed(request.getText());
                Embedding embedding = response.content();
                
                // 3. 转换为统一响应格式
                List<Float> vector = embedding.vectorAsList();
                int tokenCount = estimateTokenCount(request.getText());
                
                log.debug("单个文本向量化成功 - 模型：{}，向量维度：{}，tokens：{}", 
                        model.getModelCode(), vector.size(), tokenCount);
                
                return EmbeddingResponse.successSingle(
                        vector, 
                        model.getModelCode(), 
                        tokenCount, 
                        System.currentTimeMillis() - startTime
                );
                
            } else {
                // 批量文本向量化
                List<TextSegment> segments = request.getActualTexts().stream()
                        .map(TextSegment::from)
                        .collect(Collectors.toList());
                Response<List<Embedding>> response = embeddingModel.embedAll(segments);
                List<Embedding> embeddings = response.content();
                
                // 3. 转换为统一响应格式
                List<List<Float>> vectors = embeddings.stream()
                        .map(Embedding::vectorAsList)
                        .collect(Collectors.toList());
                
                // 估算总token数
                int totalTokens = request.getActualTexts().stream()
                        .mapToInt(this::estimateTokenCount)
                        .sum();
                
                log.debug("批量文本向量化成功 - 模型：{}，数量：{}，向量维度：{}，tokens：{}", 
                        model.getModelCode(), vectors.size(), 
                        vectors.isEmpty() ? 0 : vectors.get(0).size(), totalTokens);
                
                return EmbeddingResponse.successBatch(
                        vectors, 
                        model.getModelCode(), 
                        totalTokens, 
                        System.currentTimeMillis() - startTime
                );
            }
            
        } catch (Exception e) {
            log.error("向量化失败 - 模型：{}，错误：{}", model.getModelCode(), e.getMessage(), e);
            return EmbeddingResponse.error("向量化失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取支持的提供商代码
     */
    @Override
    public String getProviderCode() {
        return "siliconflow";
    }
}
