package com.nexusvoice.interfaces.api.test;

import com.nexusvoice.common.Result;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiEmbeddingBeanManager;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiRerankBeanManager;
import com.nexusvoice.infrastructure.ai.model.EmbeddingRequest;
import com.nexusvoice.infrastructure.ai.model.EmbeddingResponse;
import com.nexusvoice.infrastructure.ai.model.RerankRequest;
import com.nexusvoice.infrastructure.ai.model.RerankResponse;
import com.nexusvoice.infrastructure.ai.service.AiEmbeddingService;
import com.nexusvoice.infrastructure.ai.service.AiRerankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI模型开发测试控制器
 * 仅在开发环境可用，用于测试AI模型调用功能
 *
 * @author NexusVoice
 * @since 2025-10-27
 */
@Slf4j
@RestController
@RequestMapping("/api/dev/ai-models")
@Profile({"local", "dev", "test"})
@Tag(name = "开发测试-AI模型测试", description = "AI模型测试接口（仅开发环境）")
public class AiModelDevController {
    
    @Autowired
    private DynamicAiModelBeanManager modelBeanManager;
    
    @Autowired(required = false)
    private DynamicAiEmbeddingBeanManager embeddingBeanManager;
    
    @Autowired(required = false)
    private DynamicAiRerankBeanManager rerankBeanManager;
    
    /**
     * 测试模型调用
     */
    @PostMapping("/test-call")
    @Operation(summary = "测试模型调用", description = "测试指定AI模型的聊天功能")
    public Result<Map<String, Object>> testModelCall(
            @Parameter(description = "提供商代码", required = true) @RequestParam String providerCode,
            @Parameter(description = "模型代码", required = true) @RequestParam String modelCode,
            @Parameter(description = "测试消息") @RequestParam(defaultValue = "你好，请自我介绍一下") String message) {
        try {
            // 构建简单的测试请求
            com.nexusvoice.infrastructure.ai.model.ChatRequest request = 
                    com.nexusvoice.infrastructure.ai.model.ChatRequest.builder()
                    .model(providerCode + ":" + modelCode)
                    .messages(List.of(
                        com.nexusvoice.infrastructure.ai.model.ChatMessage.user(message)
                    ))
                    .temperature(0.7)
                    .maxTokens(500)
                    .userId(1L) // 测试用户ID
                    .build();
            
            // 获取服务并调用
            com.nexusvoice.infrastructure.ai.service.AiChatService service = 
                    modelBeanManager.getService(providerCode, modelCode);
            com.nexusvoice.infrastructure.ai.model.ChatResponse response = service.chat(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.getSuccess());
            result.put("content", response.getContent());
            result.put("model", response.getModel());
            result.put("usage", response.getUsage());
            result.put("responseTimeMs", response.getResponseTimeMs());
            result.put("errorMessage", response.getErrorMessage());
            
            log.info("测试模型调用成功，模型：{}:{}，耗时：{}ms", 
                    providerCode, modelCode, response.getResponseTimeMs());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("测试模型调用失败，模型：{}:{}", providerCode, modelCode, e);
            return Result.error("调用失败：" + e.getMessage());
        }
    }
    
    /**
     * 测试向量模型
     */
    @PostMapping("/test-embedding")
    @Operation(summary = "测试向量模型", description = "测试指定向量模型的文本向量化功能")
    public Result<Map<String, Object>> testEmbedding(
            @Parameter(description = "提供商代码", required = true) @RequestParam String providerCode,
            @Parameter(description = "模型代码", required = true) @RequestParam String modelCode,
            @Parameter(description = "测试文本") @RequestParam(defaultValue = "人工智能技术") String text) {
        try {
            if (embeddingBeanManager == null) {
                return Result.error("向量模型管理器未初始化");
            }
            
            // 构建请求
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .text(text)
                    .userId(1L)
                    .bizId("test")
                    .build();
            
            // 获取服务并调用
            AiEmbeddingService service = embeddingBeanManager.getService(providerCode, modelCode);
            EmbeddingResponse response = service.embed(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.getSuccess());
            result.put("model", response.getModel());
            result.put("vectorDimension", response.getVector() != null ? response.getVector().size() : 0);
            result.put("vectorSample", response.getVector() != null && response.getVector().size() > 5 ? 
                    response.getVector().subList(0, 5) : response.getVector());
            result.put("usage", response.getTokenUsage());
            result.put("duration", response.getDuration());
            result.put("errorMessage", response.getErrorMessage());
            
            log.info("测试向量模型成功，模型：{}:{}，维度：{}，耗时：{}ms", 
                    providerCode, modelCode, result.get("vectorDimension"), response.getDuration());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("测试向量模型失败，模型：{}:{}", providerCode, modelCode, e);
            return Result.error("调用失败：" + e.getMessage());
        }
    }
    
    /**
     * 测试重排序模型
     */
    @PostMapping("/test-rerank")
    @Operation(summary = "测试重排序模型", description = "测试指定重排序模型的文档重排序功能")
    public Result<Map<String, Object>> testRerank(
            @Parameter(description = "提供商代码", required = true) @RequestParam String providerCode,
            @Parameter(description = "模型代码", required = true) @RequestParam String modelCode,
            @Parameter(description = "查询文本") @RequestParam(defaultValue = "什么是人工智能") String query,
            @Parameter(description = "文档列表（逗号分隔）") @RequestParam(defaultValue = "人工智能是计算机科学的一个分支,机器学习是AI的核心技术,深度学习推动了AI的发展") String documents,
            @Parameter(description = "返回Top N结果") @RequestParam(defaultValue = "3") Integer topN) {
        try {
            if (rerankBeanManager == null) {
                return Result.error("重排序模型管理器未初始化");
            }
            
            // 将逗号分隔的文档转为列表
            List<String> docList = List.of(documents.split(","));
            
            // 构建请求
            RerankRequest request = RerankRequest.builder()
                    .query(query)
                    .documents(docList)
                    .topN(topN)
                    .userId(1L)
                    .bizId("test")
                    .returnScore(true)
                    .build();
            
            // 获取服务并调用
            AiRerankService service = rerankBeanManager.getService(providerCode, modelCode);
            RerankResponse response = service.rerank(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.getSuccess());
            result.put("model", response.getModel());
            result.put("results", response.getResults());
            result.put("usage", response.getTokenUsage());
            result.put("duration", response.getDuration());
            result.put("errorMessage", response.getErrorMessage());
            
            log.info("测试重排序模型成功，模型：{}:{}，结果数：{}，耗时：{}ms", 
                    providerCode, modelCode, 
                    response.getResults() != null ? response.getResults().size() : 0, 
                    response.getDuration());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("测试重排序模型失败，模型：{}:{}", providerCode, modelCode, e);
            return Result.error("调用失败：" + e.getMessage());
        }
    }
}
