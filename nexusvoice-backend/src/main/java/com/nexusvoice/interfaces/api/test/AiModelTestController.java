package com.nexusvoice.interfaces.api.test;

import com.nexusvoice.common.Result;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.repository.AiApiKeyRepository;
import com.nexusvoice.domain.ai.repository.AiModelRepository;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiEmbeddingBeanManager;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiRerankBeanManager;
import com.nexusvoice.infrastructure.ai.model.EmbeddingRequest;
import com.nexusvoice.infrastructure.ai.model.EmbeddingResponse;
import com.nexusvoice.infrastructure.ai.model.RerankRequest;
import com.nexusvoice.infrastructure.ai.model.RerankResponse;
import com.nexusvoice.infrastructure.ai.pool.ApiKeyPoolManager;
import com.nexusvoice.infrastructure.ai.service.AiEmbeddingService;
import com.nexusvoice.infrastructure.ai.service.AiRerankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI模型管理测试控制器
 * 用于测试动态AI模型管理功能
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/ai-model")
@Tag(name = "AI模型管理测试", description = "测试动态AI模型管理功能")
public class AiModelTestController {
    
    @Autowired
    private AiModelRepository modelRepository;
    
    @Autowired
    private AiApiKeyRepository apiKeyRepository;
    
    @Autowired
    private DynamicAiModelBeanManager modelBeanManager;
    
    @Autowired
    private ApiKeyPoolManager apiKeyPoolManager;
    
    @Autowired(required = false)
    private DynamicAiEmbeddingBeanManager embeddingBeanManager;
    
    @Autowired(required = false)
    private DynamicAiRerankBeanManager rerankBeanManager;
    
    /**
     * 获取所有模型配置
     */
    @GetMapping("/models")
    @Operation(summary = "获取所有模型配置")
    public Result<List<AiModel>> getAllModels() {
        List<AiModel> models = modelRepository.findAllEnabled();
        log.info("查询到{}个启用的模型", models.size());
        return Result.success(models);
    }
    
    /**
     * 获取可用的模型列表
     */
    @GetMapping("/available")
    @Operation(summary = "获取可用的模型列表")
    public Result<List<String>> getAvailableModels() {
        List<String> modelKeys = modelBeanManager.getAvailableModelKeys();
        log.info("当前可用模型：{}", modelKeys);
        return Result.success(modelKeys);
    }
    
    /**
     * 添加API密钥
     */
    @PostMapping("/api-key")
    @Operation(summary = "添加API密钥")
    public Result<AiApiKey> addApiKey(@RequestParam String providerCode,
                                      @RequestParam String modelCode,
                                      @RequestParam String apiKey,
                                      @RequestParam(required = false) String baseUrl) {
        // 检查模型是否存在
        if (!modelRepository.exists(providerCode, modelCode)) {
            return Result.error("模型不存在：" + providerCode + ":" + modelCode);
        }
        
        AiApiKey apiKeyEntity = new AiApiKey();
        apiKeyEntity.setProviderCode(providerCode);
        apiKeyEntity.setModelCode(modelCode);
        apiKeyEntity.setApiKey(apiKey);
        apiKeyEntity.setBaseUrl(baseUrl);
        apiKeyEntity.setStatus(1);
        apiKeyEntity.setWeight(1);
        
        apiKeyEntity = apiKeyRepository.save(apiKeyEntity);
        
        // 刷新密钥池
        apiKeyPoolManager.refreshModelPool(providerCode, modelCode);
        
        log.info("添加API密钥成功，模型：{}:{}，密钥ID：{}", 
                providerCode, modelCode, apiKeyEntity.getId());
        
        return Result.success(apiKeyEntity);
    }
    
    /**
     * 获取指定模型的API密钥列表
     */
    @GetMapping("/api-keys/{providerCode}/{modelCode}")
    @Operation(summary = "获取指定模型的API密钥列表")
    public Result<List<AiApiKey>> getApiKeys(@PathVariable String providerCode,
                                             @PathVariable String modelCode) {
        List<AiApiKey> keys = apiKeyRepository.findAllByModel(providerCode, modelCode);
        log.info("查询模型{}:{}的API密钥，数量：{}", providerCode, modelCode, keys.size());
        return Result.success(keys);
    }
    
    /**
     * 刷新模型服务
     */
    @PostMapping("/refresh/{providerCode}/{modelCode}")
    @Operation(summary = "刷新模型服务")
    public Result<String> refreshModelService(@PathVariable String providerCode,
                                              @PathVariable String modelCode) {
        modelBeanManager.refreshModelService(providerCode, modelCode);
        log.info("刷新模型服务：{}:{}", providerCode, modelCode);
        return Result.success("刷新成功");
    }
    
    /**
     * 更新模型状态
     */
    @PutMapping("/model/{id}/status")
    @Operation(summary = "更新模型状态")
    public Result<String> updateModelStatus(@PathVariable Long id,
                                           @RequestParam Integer status) {
        modelRepository.updateStatus(id, status);
        
        // 获取模型信息并刷新
        modelRepository.findById(id).ifPresent(model -> {
            modelBeanManager.refreshModelService(model.getProviderCode(), model.getModelCode());
        });
        
        log.info("更新模型状态，ID：{}，状态：{}", id, status);
        return Result.success("更新成功");
    }
    
    /**
     * 获取密钥池状态
     */
    @GetMapping("/pool-status")
    @Operation(summary = "获取密钥池状态")
    public Result<Map<String, Object>> getPoolStatus() {
        Map<String, Object> status = new HashMap<>();
        
        List<String> modelKeys = modelBeanManager.getAvailableModelKeys();
        for (String modelKey : modelKeys) {
            String[] parts = modelKey.split(":", 2);
            if (parts.length == 2) {
                int availableCount = apiKeyPoolManager.getAvailableKeyCount(parts[0], parts[1]);
                status.put(modelKey, Map.of(
                    "availableKeys", availableCount,
                    "status", availableCount > 0 ? "正常" : "无可用密钥"
                ));
            }
        }
        
        return Result.success(status);
    }
    
    /**
     * 测试模型调用
     */
    @PostMapping("/test-call")
    @Operation(summary = "测试模型调用")
    public Result<Map<String, Object>> testModelCall(@RequestParam String providerCode,
                                                     @RequestParam String modelCode,
                                                     @RequestParam(defaultValue = "你好，请自我介绍一下") String message) {
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
            log.error("测试模型调用失败", e);
            return Result.error("调用失败：" + e.getMessage());
        }
    }
    
    /**
     * 添加新模型配置
     */
    @PostMapping("/model")
    @Operation(summary = "添加新模型配置")
    public Result<AiModel> addModel(@RequestBody AiModel model) {
        // 检查是否已存在
        if (modelRepository.exists(model.getProviderCode(), model.getModelCode())) {
            return Result.error("模型已存在");
        }
        
        // 设置默认值
        if (model.getStatus() == null) {
            model.setStatus(1);
        }
        if (model.getPriority() == null) {
            model.setPriority(100);
        }
        if (model.getDefaultTemperature() == null) {
            model.setDefaultTemperature(new BigDecimal("0.7"));
        }
        if (model.getDefaultMaxTokens() == null) {
            model.setDefaultMaxTokens(2000);
        }
        if (model.getDefaultTimeoutSeconds() == null) {
            model.setDefaultTimeoutSeconds(60);
        }
        
        model = modelRepository.save(model);
        
        // 刷新服务
        modelBeanManager.refreshModelService(model.getProviderCode(), model.getModelCode());
        
        log.info("添加新模型配置：{}:{}", model.getProviderCode(), model.getModelCode());
        
        return Result.success(model);
    }
    
    /**
     * 测试向量模型
     */
    @PostMapping("/test-embedding")
    @Operation(summary = "测试向量模型")
    public Result<Map<String, Object>> testEmbedding(@RequestParam String providerCode,
                                                     @RequestParam String modelCode,
                                                     @RequestParam(defaultValue = "人工智能技术") String text) {
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
            log.error("测试向量模型失败", e);
            return Result.error("调用失败：" + e.getMessage());
        }
    }
    
    /**
     * 测试重排序模型
     */
    @PostMapping("/test-rerank")
    @Operation(summary = "测试重排序模型")
    public Result<Map<String, Object>> testRerank(@RequestParam String providerCode,
                                                   @RequestParam String modelCode,
                                                   @RequestParam(defaultValue = "什么是人工智能") String query,
                                                   @RequestParam(defaultValue = "人工智能是计算机科学的一个分支,机器学习是AI的核心技术,深度学习推动了AI的发展") String documents,
                                                   @RequestParam(defaultValue = "3") Integer topN) {
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
            log.error("测试重排序模型失败", e);
            return Result.error("调用失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取向量模型列表
     */
    @GetMapping("/embedding-models")
    @Operation(summary = "获取向量模型列表")
    public Result<List<AiModel>> getEmbeddingModels() {
        if (embeddingBeanManager == null) {
            return Result.error("向量模型管理器未初始化");
        }
        List<AiModel> models = embeddingBeanManager.getAvailableModels();
        log.info("查询到{}个可用的向量模型", models.size());
        return Result.success(models);
    }
    
    /**
     * 获取重排序模型列表
     */
    @GetMapping("/rerank-models")
    @Operation(summary = "获取重排序模型列表")
    public Result<List<AiModel>> getRerankModels() {
        if (rerankBeanManager == null) {
            return Result.error("重排序模型管理器未初始化");
        }
        List<AiModel> models = rerankBeanManager.getAvailableModels();
        log.info("查询到{}个可用的重排序模型", models.size());
        return Result.success(models);
    }
}
