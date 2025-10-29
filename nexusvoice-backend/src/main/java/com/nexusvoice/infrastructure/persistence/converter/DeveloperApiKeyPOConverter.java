package com.nexusvoice.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.developer.constant.ApiKeyStatus;
import com.nexusvoice.domain.developer.model.DeveloperApiKey;
import com.nexusvoice.infrastructure.persistence.po.DeveloperApiKeyPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DeveloperApiKey领域对象与PO转换器
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Slf4j
@Component
public class DeveloperApiKeyPOConverter {
    
    private final ObjectMapper objectMapper;
    
    public DeveloperApiKeyPOConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * PO转Domain
     */
    public DeveloperApiKey toDomain(DeveloperApiKeyPO po) {
        if (po == null) {
            return null;
        }
        
        DeveloperApiKey domain = new DeveloperApiKey();
        domain.setId(po.getId());
        domain.setKeyName(po.getKeyName());
        domain.setKeyValueHash(po.getKeyValueHash());
        domain.setKeyPrefix(po.getKeyPrefix());
        domain.setUserId(po.getUserId());
        domain.setAppName(po.getAppName());
        
        // JSONB字段反序列化
        domain.setScopes(parseJsonArray(po.getScopes()));
        domain.setAllowedModels(parseJsonArray(po.getAllowedModels()));
        domain.setIpWhitelist(parseJsonArray(po.getIpWhitelist()));
        
        domain.setRateLimitPerMinute(po.getRateLimitPerMinute());
        domain.setRateLimitPerDay(po.getRateLimitPerDay());
        domain.setTotalRequestCount(po.getTotalRequestCount());
        domain.setTodayRequestCount(po.getTodayRequestCount());
        domain.setLastRequestAt(po.getLastRequestAt());
        
        // Token统计字段
        domain.setTotalInputTokens(po.getTotalInputTokens());
        domain.setTotalOutputTokens(po.getTotalOutputTokens());
        domain.setTotalTokens(po.getTotalTokens());
        domain.setTodayTokens(po.getTodayTokens());
        
        domain.setDailyCostLimit(po.getDailyCostLimit());
        domain.setMonthlyCostLimit(po.getMonthlyCostLimit());
        domain.setTotalCost(po.getTotalCost());
        
        // Token限额字段
        domain.setDailyTokenLimit(po.getDailyTokenLimit());
        domain.setMonthlyTokenLimit(po.getMonthlyTokenLimit());
        
        // 状态枚举转换
        if (po.getStatus() != null) {
            try {
                domain.setStatus(ApiKeyStatus.valueOf(po.getStatus()));
            } catch (IllegalArgumentException e) {
                log.warn("未知的API Key状态: {}", po.getStatus());
                domain.setStatus(ApiKeyStatus.DISABLED);
            }
        }
        
        domain.setExpireAt(po.getExpireAt());
        domain.setLastUsedIp(po.getLastUsedIp());
        
        // 审计字段
        domain.setCreatedAt(po.getCreatedAt());
        domain.setUpdatedAt(po.getUpdatedAt());
        domain.setDeleted(po.getDeleted());
        
        return domain;
    }
    
    /**
     * Domain转PO
     */
    public DeveloperApiKeyPO toPO(DeveloperApiKey domain) {
        if (domain == null) {
            return null;
        }
        
        DeveloperApiKeyPO po = new DeveloperApiKeyPO();
        po.setId(domain.getId());
        po.setKeyName(domain.getKeyName());
        po.setKeyValueHash(domain.getKeyValueHash());
        po.setKeyPrefix(domain.getKeyPrefix());
        po.setUserId(domain.getUserId());
        po.setAppName(domain.getAppName());
        
        // JSONB字段序列化
        po.setScopes(toJsonString(domain.getScopes()));
        po.setAllowedModels(toJsonString(domain.getAllowedModels()));
        po.setIpWhitelist(toJsonString(domain.getIpWhitelist()));
        
        po.setRateLimitPerMinute(domain.getRateLimitPerMinute());
        po.setRateLimitPerDay(domain.getRateLimitPerDay());
        po.setTotalRequestCount(domain.getTotalRequestCount());
        po.setTodayRequestCount(domain.getTodayRequestCount());
        po.setLastRequestAt(domain.getLastRequestAt());
        
        // Token统计字段
        po.setTotalInputTokens(domain.getTotalInputTokens());
        po.setTotalOutputTokens(domain.getTotalOutputTokens());
        po.setTotalTokens(domain.getTotalTokens());
        po.setTodayTokens(domain.getTodayTokens());
        
        po.setDailyCostLimit(domain.getDailyCostLimit());
        po.setMonthlyCostLimit(domain.getMonthlyCostLimit());
        po.setTotalCost(domain.getTotalCost());
        
        // Token限额字段
        po.setDailyTokenLimit(domain.getDailyTokenLimit());
        po.setMonthlyTokenLimit(domain.getMonthlyTokenLimit());
        
        // 状态枚举转换
        if (domain.getStatus() != null) {
            po.setStatus(domain.getStatus().name());
        }
        
        po.setExpireAt(domain.getExpireAt());
        po.setLastUsedIp(domain.getLastUsedIp());
        
        // 审计字段
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setDeleted(domain.getDeleted());
        
        return po;
    }
    
    /**
     * 解析JSONB数组为List<String>
     */
    private List<String> parseJsonArray(String json) {
        // 空值守护：null、空串、空白符都返回null
        if (json == null || json.isEmpty() || json.isBlank()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("JSONB解析失败: {}", json, e);
            return null;
        }
    }
    
    /**
     * 将List<String>序列化为JSON字符串
     */
    private String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            return null;
        }
    }
}
