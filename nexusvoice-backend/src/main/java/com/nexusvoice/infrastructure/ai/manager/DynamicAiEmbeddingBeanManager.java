package com.nexusvoice.infrastructure.ai.manager;

import com.nexusvoice.domain.ai.model.AiApiCallLog;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.model.AiModelType;
import com.nexusvoice.domain.ai.repository.AiApiCallLogRepository;
import com.nexusvoice.domain.ai.repository.AiModelRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.factory.LangChain4jModelFactory;
import com.nexusvoice.infrastructure.ai.model.EmbeddingRequest;
import com.nexusvoice.infrastructure.ai.model.EmbeddingResponse;
import com.nexusvoice.infrastructure.ai.pool.ApiKeyPoolManager;
import com.nexusvoice.infrastructure.ai.service.AiEmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 动态AI向量模型Bean管理器
 * 管理所有向量模型服务实例，支持动态加载和热更新
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Slf4j
@Component
public class DynamicAiEmbeddingBeanManager {
    
    private final AiModelRepository modelRepository;
    private final AiApiCallLogRepository callLogRepository;
    private final ApiKeyPoolManager apiKeyPoolManager;
    private final LangChain4jModelFactory modelFactory;
    
    /**
     * 向量模型服务映射表
     * key: provider:model, value: 该模型的服务实例
     */
    private final Map<String, DynamicAiEmbeddingService> embeddingServiceMap = new ConcurrentHashMap<>();
    
    public DynamicAiEmbeddingBeanManager(AiModelRepository modelRepository,
                                          AiApiCallLogRepository callLogRepository,
                                          ApiKeyPoolManager apiKeyPoolManager,
                                          LangChain4jModelFactory modelFactory) {
        this.modelRepository = modelRepository;
        this.callLogRepository = callLogRepository;
        this.apiKeyPoolManager = apiKeyPoolManager;
        this.modelFactory = modelFactory;
    }
    
    /**
     * 初始化所有向量模型服务
     */
    @PostConstruct
    public void init() {
        log.info("初始化动态AI向量模型Bean管理器...");
        loadAllEmbeddingServices();
    }
    
    /**
     * 加载所有向量模型服务
     */
    public void loadAllEmbeddingServices() {
        try {
            List<AiModel> embeddingModels = modelRepository.findByTypeEnabled(AiModelType.EMBEDDING.getCode());
            
            for (AiModel model : embeddingModels) {
                String modelKey = model.getModelKey();
                DynamicAiEmbeddingService service = new DynamicAiEmbeddingService(model);
                embeddingServiceMap.put(modelKey, service);
                log.info("加载向量模型服务：{}，名称：{}", modelKey, model.getModelName());
            }
            
            log.info("成功加载{}个向量模型服务", embeddingServiceMap.size());
            
        } catch (Exception e) {
            log.error("加载向量模型服务失败", e);
        }
    }
    
    /**
     * 获取向量模型服务
     */
    public AiEmbeddingService getService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        DynamicAiEmbeddingService service = embeddingServiceMap.get(modelKey);
        
        if (service == null) {
            // 尝试动态加载
            Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
            if (modelOpt.isEmpty()) {
                throw new BizException(ErrorCodeEnum.DATA_NOT_FOUND, 
                    String.format("向量模型%s不存在", modelKey));
            }
            
            AiModel model = modelOpt.get();
            if (!model.isEmbeddingModel()) {
                throw new BizException(ErrorCodeEnum.PARAM_ERROR, 
                    String.format("模型%s不是向量模型", modelKey));
            }
            if (!model.isEnabled()) {
                throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                    String.format("向量模型%s已禁用", modelKey));
            }
            
            service = new DynamicAiEmbeddingService(model);
            embeddingServiceMap.put(modelKey, service);
            log.info("动态加载向量模型服务：{}", modelKey);
        }
        
        return service;
    }
    
    /**
     * 根据模型键获取服务
     */
    public AiEmbeddingService getServiceByModelKey(String modelKey) {
        if (modelKey == null || !modelKey.contains(":")) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "无效的模型键：" + modelKey);
        }
        
        String[] parts = modelKey.split(":", 2);
        return getService(parts[0], parts[1]);
    }
    
    /**
     * 刷新模型服务（热更新）
     */
    public void refreshEmbeddingService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        
        Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
        if (modelOpt.isEmpty() || !modelOpt.get().isEmbeddingModel()) {
            embeddingServiceMap.remove(modelKey);
            modelFactory.clearModelCache(modelKey);
            log.info("移除向量模型服务：{}", modelKey);
            return;
        }
        
        AiModel model = modelOpt.get();
        if (!model.isEnabled()) {
            embeddingServiceMap.remove(modelKey);
            modelFactory.clearModelCache(modelKey);
            log.info("禁用向量模型服务：{}", modelKey);
            return;
        }
        
        DynamicAiEmbeddingService newService = new DynamicAiEmbeddingService(model);
        embeddingServiceMap.put(modelKey, newService);
        modelFactory.clearModelCache(modelKey);
        apiKeyPoolManager.refreshModelPool(providerCode, modelCode);
        
        log.info("刷新向量模型服务：{}，名称：{}", modelKey, model.getModelName());
    }
    
    /**
     * 定时刷新任务（每30分钟执行）
     */
    @Scheduled(fixedDelay = 1800000)
    public void scheduledRefresh() {
        log.info("执行定时向量模型服务刷新任务");
        loadAllEmbeddingServices();
    }
    
    /**
     * 获取所有可用的向量模型列表
     */
    public List<AiModel> getAvailableModels() {
        return embeddingServiceMap.values().stream()
                .map(service -> service.model)
                .collect(Collectors.toList());
    }
    
    /**
     * 动态AI向量服务内部类
     */
    private class DynamicAiEmbeddingService implements AiEmbeddingService {
        private final AiModel model;
        
        public DynamicAiEmbeddingService(AiModel model) {
            this.model = model;
        }
        
        @Override
        public EmbeddingResponse embed(EmbeddingRequest request) {
            long startTime = System.currentTimeMillis();
            LocalDateTime requestTime = LocalDateTime.now();
            String requestId = UUID.randomUUID().toString();
            
            AiApiKey apiKey = null;
            
            try {
                // 1. 获取API密钥
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());
                
                // 2. 创建向量模型实例
                EmbeddingModel embeddingModel = modelFactory.createEmbeddingModel(model, apiKey);
                
                // 3. 获取文本列表并转为TextSegment
                List<String> texts = request.getActualTexts();
                List<TextSegment> segments = texts.stream()
                        .map(TextSegment::from)
                        .collect(Collectors.toList());
                
                // 4. 执行向量化
                Response<List<Embedding>> response = embeddingModel.embedAll(segments);
                List<Embedding> embeddings = response.content();
                
                // 5. 提取向量数据
                List<List<Float>> vectors = embeddings.stream()
                        .map(Embedding::vectorAsList)
                        .collect(Collectors.toList());
                
                // 6. 计算token和费用
                int totalTokens = texts.stream()
                        .mapToInt(this::estimateTokenCount)
                        .sum();
                BigDecimal cost = model.calculateCost(totalTokens, 0);
                
                // 7. 更新密钥使用统计
                apiKeyPoolManager.markSuccess(apiKey.getId(), totalTokens, cost);
                
                // 8. 记录调用日志（Embedding模型没有conversationId，传null）
                AiApiCallLog callLog = AiApiCallLog.success(
                        apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                        request.getUserId(), null,
                        requestId, requestTime,
                        (int)(System.currentTimeMillis() - startTime),
                        totalTokens, 0, cost
                );
                callLogRepository.save(callLog);
                
                // 9. 构建响应
                long duration = System.currentTimeMillis() - startTime;
                if (request.isSingleText()) {
                    return EmbeddingResponse.successSingle(vectors.get(0), model.getModelKey(), totalTokens, duration);
                } else {
                    return EmbeddingResponse.successBatch(vectors, model.getModelKey(), totalTokens, duration);
                }
                
            } catch (Exception e) {
                log.error("向量化请求失败，模型：{}，错误：{}", model.getModelKey(), e.getMessage(), e);
                
                // 标记密钥失败
                if (apiKey != null) {
                    apiKeyPoolManager.markFailed(apiKey.getId());
                    
                    // 记录失败日志（Embedding模型没有conversationId，传null）
                    AiApiCallLog callLog = AiApiCallLog.failure(
                            apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                            request.getUserId(), null,
                            requestId, requestTime, e.getMessage()
                    );
                    callLogRepository.save(callLog);
                }
                
                return EmbeddingResponse.error("向量化请求失败：" + e.getMessage());
            }
        }
        
        @Override
        public String getModelName() {
            return model.getModelName();
        }
        
        @Override
        public boolean isModelAvailable() {
            return model.isEnabled() && 
                   apiKeyPoolManager.getAvailableKeyCount(model.getProviderCode(), model.getModelCode()) > 0;
        }
        
        @Override
        public int estimateTokenCount(String text) {
            if (text == null || text.isEmpty()) {
                return 0;
            }
            // 简单估算：平均3-4个字符一个token
            return (int) Math.ceil(text.length() / 3.5);
        }
    }
}
