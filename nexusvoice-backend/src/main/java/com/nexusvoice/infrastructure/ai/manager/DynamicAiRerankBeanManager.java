package com.nexusvoice.infrastructure.ai.manager;

import com.nexusvoice.domain.ai.model.AiApiCallLog;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.model.AiModelType;
import com.nexusvoice.domain.ai.repository.AiApiCallLogRepository;
import com.nexusvoice.domain.ai.repository.AiModelRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.model.RerankRequest;
import com.nexusvoice.infrastructure.ai.model.RerankResponse;
import com.nexusvoice.infrastructure.ai.model.SiliconFlowRerankAdapter;
import com.nexusvoice.infrastructure.ai.pool.ApiKeyPoolManager;
import com.nexusvoice.infrastructure.ai.service.AiRerankService;
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
 * 动态AI重排序模型Bean管理器
 * 管理所有重排序模型服务实例，支持动态加载和热更新
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Slf4j
@Component
public class DynamicAiRerankBeanManager {
    
    private final AiModelRepository modelRepository;
    private final AiApiCallLogRepository callLogRepository;
    private final ApiKeyPoolManager apiKeyPoolManager;
    private final SiliconFlowRerankAdapter rerankAdapter;
    
    /**
     * 重排序模型服务映射表
     */
    private final Map<String, DynamicAiRerankService> rerankServiceMap = new ConcurrentHashMap<>();
    
    public DynamicAiRerankBeanManager(AiModelRepository modelRepository,
                                       AiApiCallLogRepository callLogRepository,
                                       ApiKeyPoolManager apiKeyPoolManager,
                                       SiliconFlowRerankAdapter rerankAdapter) {
        this.modelRepository = modelRepository;
        this.callLogRepository = callLogRepository;
        this.apiKeyPoolManager = apiKeyPoolManager;
        this.rerankAdapter = rerankAdapter;
    }
    
    /**
     * 初始化所有重排序模型服务
     */
    @PostConstruct
    public void init() {
        log.info("初始化动态AI重排序模型Bean管理器...");
        loadAllRerankServices();
    }
    
    /**
     * 加载所有重排序模型服务
     */
    public void loadAllRerankServices() {
        try {
            List<AiModel> rerankModels = modelRepository.findByTypeEnabled(AiModelType.RERANK.getCode());
            
            for (AiModel model : rerankModels) {
                String modelKey = model.getModelKey();
                DynamicAiRerankService service = new DynamicAiRerankService(model);
                rerankServiceMap.put(modelKey, service);
                log.info("加载重排序模型服务：{}，名称：{}", modelKey, model.getModelName());
            }
            
            log.info("成功加载{}个重排序模型服务", rerankServiceMap.size());
            
        } catch (Exception e) {
            log.error("加载重排序模型服务失败", e);
        }
    }
    
    /**
     * 获取重排序模型服务
     */
    public AiRerankService getService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        DynamicAiRerankService service = rerankServiceMap.get(modelKey);
        
        if (service == null) {
            // 尝试动态加载
            Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
            if (modelOpt.isEmpty()) {
                throw new BizException(ErrorCodeEnum.DATA_NOT_FOUND, 
                    String.format("重排序模型%s不存在", modelKey));
            }
            
            AiModel model = modelOpt.get();
            if (!model.isRerankModel()) {
                throw new BizException(ErrorCodeEnum.PARAM_ERROR, 
                    String.format("模型%s不是重排序模型", modelKey));
            }
            if (!model.isEnabled()) {
                throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                    String.format("重排序模型%s已禁用", modelKey));
            }
            
            service = new DynamicAiRerankService(model);
            rerankServiceMap.put(modelKey, service);
            log.info("动态加载重排序模型服务：{}", modelKey);
        }
        
        return service;
    }
    
    /**
     * 根据模型键获取服务
     */
    public AiRerankService getServiceByModelKey(String modelKey) {
        if (modelKey == null || !modelKey.contains(":")) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "无效的模型键：" + modelKey);
        }
        
        String[] parts = modelKey.split(":", 2);
        return getService(parts[0], parts[1]);
    }
    
    /**
     * 刷新模型服务（热更新）
     */
    public void refreshRerankService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        
        Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
        if (modelOpt.isEmpty() || !modelOpt.get().isRerankModel()) {
            rerankServiceMap.remove(modelKey);
            log.info("移除重排序模型服务：{}", modelKey);
            return;
        }
        
        AiModel model = modelOpt.get();
        if (!model.isEnabled()) {
            rerankServiceMap.remove(modelKey);
            log.info("禁用重排序模型服务：{}", modelKey);
            return;
        }
        
        DynamicAiRerankService newService = new DynamicAiRerankService(model);
        rerankServiceMap.put(modelKey, newService);
        apiKeyPoolManager.refreshModelPool(providerCode, modelCode);
        
        log.info("刷新重排序模型服务：{}，名称：{}", modelKey, model.getModelName());
    }
    
    /**
     * 定时刷新任务（每30分钟执行）
     */
    @Scheduled(fixedDelay = 1800000)
    public void scheduledRefresh() {
        log.info("执行定时重排序模型服务刷新任务");
        loadAllRerankServices();
    }
    
    /**
     * 获取所有可用的重排序模型列表
     */
    public List<AiModel> getAvailableModels() {
        return rerankServiceMap.values().stream()
                .map(service -> service.model)
                .collect(Collectors.toList());
    }
    
    /**
     * 动态AI重排序服务内部类
     */
    private class DynamicAiRerankService implements AiRerankService {
        private final AiModel model;
        
        public DynamicAiRerankService(AiModel model) {
            this.model = model;
        }
        
        @Override
        public RerankResponse rerank(RerankRequest request) {
            long startTime = System.currentTimeMillis();
            LocalDateTime requestTime = LocalDateTime.now();
            String requestId = UUID.randomUUID().toString();
            
            AiApiKey apiKey = null;
            
            try {
                // 1. 获取API密钥
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());
                
                // 2. 调用重排序API
                RerankResponse.RerankResult[] results = rerankAdapter.rerank(
                        model, apiKey, 
                        request.getQuery(), 
                        request.getDocuments(), 
                        request.getActualTopN()
                );
                
                // 3. 计算token和费用
                int totalTokens = estimateTokenCount(request.getQuery());
                for (String doc : request.getDocuments()) {
                    totalTokens += estimateTokenCount(doc);
                }
                BigDecimal cost = model.calculateCost(totalTokens, 0);
                
                // 4. 更新密钥使用统计
                apiKeyPoolManager.markSuccess(apiKey.getId(), totalTokens, cost);
                
                // 5. 记录调用日志（Rerank模型没有conversationId，传null）
                AiApiCallLog callLog = AiApiCallLog.success(
                        apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                        request.getUserId(), null,
                        requestId, requestTime,
                        (int)(System.currentTimeMillis() - startTime),
                        totalTokens, 0, cost
                );
                callLogRepository.save(callLog);
                
                // 6. 构建响应
                long duration = System.currentTimeMillis() - startTime;
                return RerankResponse.success(Arrays.asList(results), model.getModelKey(), totalTokens, duration);
                
            } catch (Exception e) {
                log.error("重排序请求失败，模型：{}，错误：{}", model.getModelKey(), e.getMessage(), e);
                
                // 标记密钥失败
                if (apiKey != null) {
                    apiKeyPoolManager.markFailed(apiKey.getId());
                    
                    // 记录失败日志（Rerank模型没有conversationId，传null）
                    AiApiCallLog callLog = AiApiCallLog.failure(
                            apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                            request.getUserId(), null,
                            requestId, requestTime, e.getMessage()
                    );
                    callLogRepository.save(callLog);
                }
                
                return RerankResponse.error("重排序请求失败：" + e.getMessage());
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
            return rerankAdapter.estimateTokenCount(text);
        }
    }
}
