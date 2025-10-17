package com.nexusvoice.infrastructure.ai.pool;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.repository.AiApiKeyRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * API密钥池管理器
 * 负责管理各个模型的API密钥池，支持轮询、加权、健康检查等
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Component
public class ApiKeyPoolManager {
    
    @Autowired
    private AiApiKeyRepository apiKeyRepository;
    
    /**
     * 模型密钥池映射
     * key: provider:model, value: 该模型的密钥池
     */
    private final Map<String, ModelApiKeyPool> modelPoolMap = new ConcurrentHashMap<>();
    
    /**
     * 初始化所有模型的密钥池
     */
    @PostConstruct
    public void init() {
        log.info("初始化API密钥池管理器...");
        loadAllApiKeys();
    }
    
    /**
     * 加载所有API密钥
     */
    public void loadAllApiKeys() {
        try {
            // 获取所有模型的密钥分组
            Map<String, List<AiApiKey>> groupedKeys = new HashMap<>();
            
            // 这里应该查询所有可用的密钥，按模型分组
            // 简化处理：假设有方法获取所有密钥
            List<AiApiKey> allKeys = apiKeyRepository.findAllByModel(null, null);
            
            for (AiApiKey key : allKeys) {
                String modelKey = key.getProviderCode() + ":" + key.getModelCode();
                groupedKeys.computeIfAbsent(modelKey, k -> new ArrayList<>()).add(key);
            }
            
            // 为每个模型创建密钥池
            for (Map.Entry<String, List<AiApiKey>> entry : groupedKeys.entrySet()) {
                String modelKey = entry.getKey();
                List<AiApiKey> keys = entry.getValue();
                
                ModelApiKeyPool pool = new ModelApiKeyPool(modelKey, keys);
                modelPoolMap.put(modelKey, pool);
                
                log.info("初始化模型{}的API密钥池，密钥数量：{}", modelKey, keys.size());
            }
            
        } catch (Exception e) {
            log.error("加载API密钥失败", e);
        }
    }
    
    /**
     * 获取下一个可用的API密钥
     * 
     * @param providerCode 厂商代码
     * @param modelCode 模型代码
     * @return API密钥配置
     */
    public AiApiKey getNextApiKey(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        ModelApiKeyPool pool = modelPoolMap.get(modelKey);
        
        if (pool == null) {
            // 尝试动态加载
            List<AiApiKey> keys = apiKeyRepository.findAvailableByModel(providerCode, modelCode);
            if (keys.isEmpty()) {
                throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                    String.format("模型%s没有可用的API密钥", modelKey));
            }
            pool = new ModelApiKeyPool(modelKey, keys);
            modelPoolMap.put(modelKey, pool);
        }
        
        AiApiKey apiKey = pool.getNext();
        if (apiKey == null) {
            throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                String.format("模型%s的所有API密钥都不可用", modelKey));
        }
        
        // 更新最后使用时间
        apiKeyRepository.updateLastUsedTime(apiKey.getId(), LocalDateTime.now());
        
        return apiKey;
    }
    
    /**
     * 标记API密钥调用成功
     */
    public void markSuccess(Long apiKeyId, int tokensUsed, BigDecimal cost) {
        apiKeyRepository.updateUsageStats(apiKeyId, tokensUsed, cost);
        
        // 更新内存中的状态
        modelPoolMap.values().forEach(pool -> pool.markSuccess(apiKeyId));
    }
    
    /**
     * 标记API密钥调用失败
     */
    public void markFailed(Long apiKeyId) {
        apiKeyRepository.markFailed(apiKeyId);
        
        // 更新内存中的状态
        modelPoolMap.values().forEach(pool -> pool.markFailed(apiKeyId));
    }
    
    /**
     * 刷新指定模型的密钥池
     */
    public void refreshModelPool(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        List<AiApiKey> keys = apiKeyRepository.findAllByModel(providerCode, modelCode);
        
        if (!keys.isEmpty()) {
            ModelApiKeyPool pool = new ModelApiKeyPool(modelKey, keys);
            modelPoolMap.put(modelKey, pool);
            log.info("刷新模型{}的API密钥池，密钥数量：{}", modelKey, keys.size());
        } else {
            modelPoolMap.remove(modelKey);
            log.warn("模型{}没有可用的API密钥，移除密钥池", modelKey);
        }
    }
    
    /**
     * 每日重置任务（每天凌晨执行）
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyReset() {
        log.info("执行API密钥每日重置任务");
        apiKeyRepository.resetAllDailyQuota();
        
        // 重新加载所有密钥池
        loadAllApiKeys();
    }
    
    /**
     * 月度重置任务（每天凌晨检查）
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void monthlyReset() {
        log.info("检查API密钥月度重置");
        LocalDate today = LocalDate.now();
        apiKeyRepository.resetMonthlyQuotaByDate(today);
    }
    
    /**
     * 健康检查任务（每5分钟执行）
     */
    @Scheduled(fixedDelay = 300000)
    public void healthCheck() {
        log.debug("执行API密钥健康检查");
        
        // 查找需要健康检查的密钥
        List<AiApiKey> needCheck = apiKeyRepository.findNeedHealthCheck(30);
        
        for (AiApiKey key : needCheck) {
            try {
                // 这里可以调用实际的健康检查逻辑
                // 比如发送一个简单的请求测试密钥是否有效
                performHealthCheck(key);
                apiKeyRepository.updateHealthCheckTime(key.getId(), LocalDateTime.now());
            } catch (Exception e) {
                log.error("健康检查失败，密钥ID：{}", key.getId(), e);
            }
        }
        
        // 检查是否有密钥需要从熔断中恢复
        List<AiApiKey> needRecovery = apiKeyRepository.findNeedCircuitBreakerRecovery(30);
        for (AiApiKey key : needRecovery) {
            log.info("尝试恢复熔断的API密钥，ID：{}", key.getId());
            apiKeyRepository.updateStatus(key.getId(), 1);
            
            // 刷新对应模型的密钥池
            refreshModelPool(key.getProviderCode(), key.getModelCode());
        }
    }
    
    /**
     * 执行健康检查（需要根据实际情况实现）
     */
    private void performHealthCheck(AiApiKey apiKey) {
        // TODO: 实现具体的健康检查逻辑
        // 例如：调用模型的简单API验证密钥是否有效
    }
    
    /**
     * 获取模型的可用密钥数量
     */
    public int getAvailableKeyCount(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        ModelApiKeyPool pool = modelPoolMap.get(modelKey);
        return pool != null ? pool.getAvailableCount() : 0;
    }
    
    /**
     * 单个模型的API密钥池
     */
    private static class ModelApiKeyPool {
        @SuppressWarnings("unused")
        private final String modelKey;  // 保留用于调试和日志
        private final List<AiApiKey> allKeys;
        private final List<AiApiKey> availableKeys;
        private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
        
        public ModelApiKeyPool(String modelKey, List<AiApiKey> keys) {
            this.modelKey = modelKey;
            this.allKeys = new ArrayList<>(keys);
            this.availableKeys = keys.stream()
                    .filter(AiApiKey::isAvailable)
                    .collect(Collectors.toList());
        }
        
        /**
         * 获取下一个可用密钥（加权轮询）
         */
        public synchronized AiApiKey getNext() {
            if (availableKeys.isEmpty()) {
                return null;
            }
            
            // 计算总权重
            int totalWeight = availableKeys.stream()
                    .mapToInt(k -> k.getWeight() != null ? k.getWeight() : 1)
                    .sum();
            
            if (totalWeight == 0) {
                // 简单轮询
                int index = roundRobinIndex.getAndIncrement() % availableKeys.size();
                return availableKeys.get(index);
            }
            
            // 加权随机选择
            int random = ThreadLocalRandom.current().nextInt(totalWeight);
            int weightSum = 0;
            
            for (AiApiKey key : availableKeys) {
                weightSum += (key.getWeight() != null ? key.getWeight() : 1);
                if (random < weightSum) {
                    return key;
                }
            }
            
            // 兜底返回第一个
            return availableKeys.get(0);
        }
        
        /**
         * 标记密钥成功
         */
        public synchronized void markSuccess(Long apiKeyId) {
            // 如果密钥之前不可用，现在恢复了，重新加入可用列表
            allKeys.stream()
                    .filter(k -> k.getId().equals(apiKeyId))
                    .findFirst()
                    .ifPresent(key -> {
                        if (!availableKeys.contains(key) && key.isAvailable()) {
                            availableKeys.add(key);
                        }
                    });
        }
        
        /**
         * 标记密钥失败
         */
        public synchronized void markFailed(Long apiKeyId) {
            // 从可用列表中移除失败的密钥
            availableKeys.removeIf(k -> k.getId().equals(apiKeyId) && !k.isAvailable());
        }
        
        /**
         * 获取可用密钥数量
         */
        public int getAvailableCount() {
            return availableKeys.size();
        }
    }
}
