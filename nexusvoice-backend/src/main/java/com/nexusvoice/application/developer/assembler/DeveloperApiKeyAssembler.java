package com.nexusvoice.application.developer.assembler;

import com.nexusvoice.application.developer.dto.*;
import com.nexusvoice.domain.developer.constant.ApiKeyScope;
import com.nexusvoice.domain.developer.constant.ApiKeyStatus;
import com.nexusvoice.domain.developer.model.DeveloperApiKey;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 开发者API密钥DTO转换器
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Component
public class DeveloperApiKeyAssembler {
    
    /**
     * 创建请求DTO转领域模型
     * 
     * @param requestDTO 创建请求DTO
     * @param userId 用户ID
     * @param apiKey 生成的API密钥
     * @param keyValueHash 密钥哈希值
     * @param keyPrefix 密钥前缀
     * @return 领域模型
     */
    public DeveloperApiKey toDomain(CreateApiKeyRequestDTO requestDTO, Long userId, 
                                     String apiKey, String keyValueHash, String keyPrefix) {
        if (requestDTO == null) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "创建API密钥请求不能为空");
        }
        
        DeveloperApiKey domain = new DeveloperApiKey();
        domain.setKeyName(requestDTO.getKeyName());
        domain.setKeyValueHash(keyValueHash);
        domain.setKeyPrefix(keyPrefix);
        domain.setUserId(userId);
        domain.setAppName(requestDTO.getAppName());
        
        // 验证并设置权限范围
        domain.setScopes(validateAndConvertScopes(requestDTO.getScopes()));
        
        // 设置其他字段
        domain.setAllowedModels(requestDTO.getAllowedModels());
        domain.setIpWhitelist(requestDTO.getIpWhitelist());
        domain.setRateLimitPerMinute(requestDTO.getRateLimitPerMinute());
        domain.setRateLimitPerDay(requestDTO.getRateLimitPerDay());
        domain.setDailyCostLimit(requestDTO.getDailyCostLimit());
        domain.setMonthlyCostLimit(requestDTO.getMonthlyCostLimit());
        domain.setDailyTokenLimit(requestDTO.getDailyTokenLimit());
        domain.setMonthlyTokenLimit(requestDTO.getMonthlyTokenLimit());
        domain.setExpireAt(requestDTO.getExpireAt());
        
        // 初始化状态和统计字段
        domain.setStatus(ApiKeyStatus.ACTIVE);
        domain.setTotalRequestCount(0L);
        domain.setTodayRequestCount(0);
        domain.setTotalInputTokens(0L);
        domain.setTotalOutputTokens(0L);
        domain.setTotalTokens(0L);
        domain.setTodayTokens(0L);
        
        return domain;
    }
    
    /**
     * 更新请求DTO转领域模型（部分更新）
     * 
     * @param requestDTO 更新请求DTO
     * @param existing 现有领域模型
     */
    public void updateDomain(UpdateApiKeyRequestDTO requestDTO, DeveloperApiKey existing) {
        if (requestDTO == null || existing == null) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "更新API密钥请求或现有密钥不能为空");
        }
        
        // 只更新非null字段
        if (requestDTO.getKeyName() != null) {
            existing.setKeyName(requestDTO.getKeyName());
        }
        if (requestDTO.getAppName() != null) {
            existing.setAppName(requestDTO.getAppName());
        }
        if (requestDTO.getScopes() != null) {
            existing.setScopes(validateAndConvertScopes(requestDTO.getScopes()));
        }
        if (requestDTO.getAllowedModels() != null) {
            existing.setAllowedModels(requestDTO.getAllowedModels());
        }
        if (requestDTO.getIpWhitelist() != null) {
            existing.setIpWhitelist(requestDTO.getIpWhitelist());
        }
        if (requestDTO.getRateLimitPerMinute() != null) {
            existing.setRateLimitPerMinute(requestDTO.getRateLimitPerMinute());
        }
        if (requestDTO.getRateLimitPerDay() != null) {
            existing.setRateLimitPerDay(requestDTO.getRateLimitPerDay());
        }
        if (requestDTO.getDailyCostLimit() != null) {
            existing.setDailyCostLimit(requestDTO.getDailyCostLimit());
        }
        if (requestDTO.getMonthlyCostLimit() != null) {
            existing.setMonthlyCostLimit(requestDTO.getMonthlyCostLimit());
        }
        if (requestDTO.getDailyTokenLimit() != null) {
            existing.setDailyTokenLimit(requestDTO.getDailyTokenLimit());
        }
        if (requestDTO.getMonthlyTokenLimit() != null) {
            existing.setMonthlyTokenLimit(requestDTO.getMonthlyTokenLimit());
        }
        if (requestDTO.getExpireAt() != null) {
            existing.setExpireAt(requestDTO.getExpireAt());
        }
        
        // 触发更新时间
        existing.onUpdate();
    }
    
    /**
     * 领域模型转响应DTO
     * 
     * @param domain 领域模型
     * @return 响应DTO
     */
    public ApiKeyResponseDTO toResponseDTO(DeveloperApiKey domain) {
        if (domain == null) {
            return null;
        }
        
        ApiKeyResponseDTO dto = new ApiKeyResponseDTO();
        dto.setId(String.valueOf(domain.getId()));
        dto.setKeyName(domain.getKeyName());
        dto.setKeyPrefix(domain.getKeyPrefix());
        dto.setAppName(domain.getAppName());
        dto.setScopes(domain.getScopes());
        dto.setAllowedModels(domain.getAllowedModels());
        dto.setIpWhitelist(domain.getIpWhitelist());
        dto.setRateLimitPerMinute(domain.getRateLimitPerMinute());
        dto.setRateLimitPerDay(domain.getRateLimitPerDay());
        
        // Long类型统计字段转String（避免前端精度丢失）
        dto.setTotalRequestCount(String.valueOf(domain.getTotalRequestCount() != null ? domain.getTotalRequestCount() : 0));
        dto.setTodayRequestCount(domain.getTodayRequestCount() != null ? domain.getTodayRequestCount() : 0);
        dto.setLastRequestAt(domain.getLastRequestAt());
        
        // Token统计字段
        dto.setTotalInputTokens(String.valueOf(domain.getTotalInputTokens() != null ? domain.getTotalInputTokens() : 0));
        dto.setTotalOutputTokens(String.valueOf(domain.getTotalOutputTokens() != null ? domain.getTotalOutputTokens() : 0));
        dto.setTotalTokens(String.valueOf(domain.getTotalTokens() != null ? domain.getTotalTokens() : 0));
        dto.setTodayTokens(String.valueOf(domain.getTodayTokens() != null ? domain.getTodayTokens() : 0));
        
        dto.setDailyCostLimit(domain.getDailyCostLimit());
        dto.setMonthlyCostLimit(domain.getMonthlyCostLimit());
        dto.setTotalCost(domain.getTotalCost());
        
        // Token限额字段
        dto.setDailyTokenLimit(String.valueOf(domain.getDailyTokenLimit() != null ? domain.getDailyTokenLimit() : 0));
        dto.setMonthlyTokenLimit(String.valueOf(domain.getMonthlyTokenLimit() != null ? domain.getMonthlyTokenLimit() : 0));
        
        dto.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        dto.setExpireAt(domain.getExpireAt());
        dto.setLastUsedIp(domain.getLastUsedIp());
        dto.setCreatedAt(domain.getCreatedAt());
        dto.setUpdatedAt(domain.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * 领域模型转创建响应DTO（包含完整密钥）
     * 
     * @param domain 领域模型
     * @param apiKey 完整API密钥
     * @return 创建响应DTO
     */
    public CreateApiKeyResponseDTO toCreateResponseDTO(DeveloperApiKey domain, String apiKey) {
        if (domain == null) {
            return null;
        }
        
        CreateApiKeyResponseDTO dto = new CreateApiKeyResponseDTO();
        
        // 复制基础字段
        ApiKeyResponseDTO baseDto = toResponseDTO(domain);
        dto.setId(baseDto.getId());
        dto.setKeyName(baseDto.getKeyName());
        dto.setKeyPrefix(baseDto.getKeyPrefix());
        dto.setAppName(baseDto.getAppName());
        dto.setScopes(baseDto.getScopes());
        dto.setAllowedModels(baseDto.getAllowedModels());
        dto.setIpWhitelist(baseDto.getIpWhitelist());
        dto.setRateLimitPerMinute(baseDto.getRateLimitPerMinute());
        dto.setRateLimitPerDay(baseDto.getRateLimitPerDay());
        dto.setTotalRequestCount(baseDto.getTotalRequestCount());
        dto.setTodayRequestCount(baseDto.getTodayRequestCount());
        dto.setLastRequestAt(baseDto.getLastRequestAt());
        dto.setTotalInputTokens(baseDto.getTotalInputTokens());
        dto.setTotalOutputTokens(baseDto.getTotalOutputTokens());
        dto.setTotalTokens(baseDto.getTotalTokens());
        dto.setTodayTokens(baseDto.getTodayTokens());
        dto.setDailyCostLimit(baseDto.getDailyCostLimit());
        dto.setMonthlyCostLimit(baseDto.getMonthlyCostLimit());
        dto.setTotalCost(baseDto.getTotalCost());
        dto.setDailyTokenLimit(baseDto.getDailyTokenLimit());
        dto.setMonthlyTokenLimit(baseDto.getMonthlyTokenLimit());
        dto.setStatus(baseDto.getStatus());
        dto.setExpireAt(baseDto.getExpireAt());
        dto.setLastUsedIp(baseDto.getLastUsedIp());
        dto.setCreatedAt(baseDto.getCreatedAt());
        dto.setUpdatedAt(baseDto.getUpdatedAt());
        
        // 设置完整密钥
        dto.setApiKey(apiKey);
        
        return dto;
    }
    
    /**
     * 领域模型列表转响应DTO列表
     * 
     * @param domainList 领域模型列表
     * @return 响应DTO列表
     */
    public List<ApiKeyResponseDTO> toResponseDTOList(List<DeveloperApiKey> domainList) {
        if (domainList == null) {
            return new ArrayList<>();
        }
        
        return domainList.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 验证并转换权限范围
     * 
     * @param scopes 权限范围字符串列表
     * @return 验证后的权限范围列表
     */
    private List<String> validateAndConvertScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "权限范围不能为空");
        }
        
        List<String> validatedScopes = new ArrayList<>();
        for (String scope : scopes) {
            ApiKeyScope scopeEnum = ApiKeyScope.fromCode(scope);
            if (scopeEnum == null) {
                throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "无效的权限范围: " + scope);
            }
            validatedScopes.add(scopeEnum.getCode());
        }
        
        return validatedScopes;
    }
}
