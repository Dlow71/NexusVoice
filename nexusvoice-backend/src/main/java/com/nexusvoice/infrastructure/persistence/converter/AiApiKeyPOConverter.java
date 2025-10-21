package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.infrastructure.persistence.po.AiApiKeyPO;
import org.springframework.stereotype.Component;

/**
 * AiApiKey实体与AiApiKeyPO转换器
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Component
public class AiApiKeyPOConverter {

    public AiApiKeyPO toPO(AiApiKey apiKey) {
        if (apiKey == null) {
            return null;
        }

        AiApiKeyPO po = new AiApiKeyPO();
        
        // 基础字段
        po.setId(apiKey.getId());
        po.setCreatedAt(apiKey.getCreatedAt());
        po.setUpdatedAt(apiKey.getUpdatedAt());
        po.setDeleted(apiKey.getDeleted());
        
        // 业务字段
        po.setProviderCode(apiKey.getProviderCode());
        po.setModelCode(apiKey.getModelCode());
        po.setApiKey(apiKey.getApiKey());
        po.setApiSecret(apiKey.getApiSecret());
        po.setBaseUrl(apiKey.getBaseUrl());
        po.setProxyUrl(apiKey.getProxyUrl());
        po.setWeight(apiKey.getWeight());
        po.setRateLimit(apiKey.getRateLimit());
        po.setConcurrentLimit(apiKey.getConcurrentLimit());
        po.setStatus(apiKey.getStatus());
        po.setFailCount(apiKey.getFailCount());
        po.setLastFailTime(apiKey.getLastFailTime());
        po.setLastSuccessTime(apiKey.getLastSuccessTime());
        po.setHealthCheckTime(apiKey.getHealthCheckTime());
        po.setTotalRequests(apiKey.getTotalRequests());
        po.setTotalTokensUsed(apiKey.getTotalTokensUsed());
        po.setTotalCost(apiKey.getTotalCost());
        po.setLastUsedAt(apiKey.getLastUsedAt());
        po.setMonthlyRequests(apiKey.getMonthlyRequests());
        po.setMonthlyTokensUsed(apiKey.getMonthlyTokensUsed());
        po.setMonthlyCost(apiKey.getMonthlyCost());
        po.setMonthlyResetDate(apiKey.getMonthlyResetDate());
        po.setDailyQuotaLimit(apiKey.getDailyQuotaLimit());
        po.setMonthlyQuotaLimit(apiKey.getMonthlyQuotaLimit());
        po.setDailyTokensUsed(apiKey.getDailyTokensUsed());
        
        return po;
    }

    public AiApiKey toDomain(AiApiKeyPO po) {
        if (po == null) {
            return null;
        }

        AiApiKey apiKey = new AiApiKey();
        
        // 基础字段
        apiKey.setId(po.getId());
        apiKey.setCreatedAt(po.getCreatedAt());
        apiKey.setUpdatedAt(po.getUpdatedAt());
        apiKey.setDeleted(po.getDeleted());
        
        // 业务字段
        apiKey.setProviderCode(po.getProviderCode());
        apiKey.setModelCode(po.getModelCode());
        apiKey.setApiKey(po.getApiKey());
        apiKey.setApiSecret(po.getApiSecret());
        apiKey.setBaseUrl(po.getBaseUrl());
        apiKey.setProxyUrl(po.getProxyUrl());
        apiKey.setWeight(po.getWeight());
        apiKey.setRateLimit(po.getRateLimit());
        apiKey.setConcurrentLimit(po.getConcurrentLimit());
        apiKey.setStatus(po.getStatus());
        apiKey.setFailCount(po.getFailCount());
        apiKey.setLastFailTime(po.getLastFailTime());
        apiKey.setLastSuccessTime(po.getLastSuccessTime());
        apiKey.setHealthCheckTime(po.getHealthCheckTime());
        apiKey.setTotalRequests(po.getTotalRequests());
        apiKey.setTotalTokensUsed(po.getTotalTokensUsed());
        apiKey.setTotalCost(po.getTotalCost());
        apiKey.setLastUsedAt(po.getLastUsedAt());
        apiKey.setMonthlyRequests(po.getMonthlyRequests());
        apiKey.setMonthlyTokensUsed(po.getMonthlyTokensUsed());
        apiKey.setMonthlyCost(po.getMonthlyCost());
        apiKey.setMonthlyResetDate(po.getMonthlyResetDate());
        apiKey.setDailyQuotaLimit(po.getDailyQuotaLimit());
        apiKey.setMonthlyQuotaLimit(po.getMonthlyQuotaLimit());
        apiKey.setDailyTokensUsed(po.getDailyTokensUsed());
        
        return apiKey;
    }
}
