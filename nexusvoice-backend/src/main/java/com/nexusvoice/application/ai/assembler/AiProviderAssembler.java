package com.nexusvoice.application.ai.assembler;

import com.nexusvoice.application.ai.dto.provider.AiProviderDTO;
import com.nexusvoice.application.ai.dto.provider.CreateAiProviderDTO;
import com.nexusvoice.application.ai.dto.provider.UpdateAiProviderDTO;
import com.nexusvoice.domain.ai.model.AiProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI服务提供商Assembler
 * 负责DTO与Domain实体之间的转换
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
@Component
public class AiProviderAssembler {
    
    /**
     * Domain实体转DTO
     *
     * @param provider 领域实体
     * @return DTO
     */
    public AiProviderDTO toDTO(AiProvider provider) {
        if (provider == null) {
            return null;
        }
        
        AiProviderDTO dto = new AiProviderDTO();
        dto.setId(provider.getId());
        dto.setProviderCode(provider.getProviderCode());
        dto.setProviderName(provider.getProviderName());
        dto.setProtocol(provider.getProtocol());
        dto.setProtocolName(provider.getProtocolEnum().getName());
        dto.setDescription(provider.getDescription());
        dto.setDefaultBaseUrl(provider.getDefaultBaseUrl());
        dto.setIsOfficial(provider.getIsOfficial());
        dto.setStatus(provider.getStatus());
        dto.setStatusName(getStatusName(provider.getStatus()));
        dto.setPriority(provider.getPriority());
        dto.setUserId(provider.getUserId());
        dto.setCreatedAt(provider.getCreatedAt());
        dto.setUpdatedAt(provider.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * CreateDTO转Domain实体
     *
     * @param dto 创建DTO
     * @param userId 用户ID（用于自定义服务商）
     * @return 领域实体
     */
    public AiProvider toDomain(CreateAiProviderDTO dto, Long userId) {
        if (dto == null) {
            return null;
        }
        
        AiProvider provider = new AiProvider();
        provider.setProviderCode(dto.getProviderCode());
        provider.setProviderName(dto.getProviderName());
        provider.setProtocol(dto.getProtocol());
        provider.setDescription(dto.getDescription());
        provider.setDefaultBaseUrl(dto.getDefaultBaseUrl());
        provider.setConfigJson(dto.getConfigJson());
        provider.setPriority(dto.getPriority() != null ? dto.getPriority() : 100);
        
        // 用户创建的服务商为自定义服务商
        provider.setIsOfficial(false);
        provider.setUserId(userId);
        provider.setStatus(1); // 默认启用
        
        return provider;
    }
    
    /**
     * 创建官方服务商实体
     *
     * @param dto 创建DTO
     * @return 领域实体
     */
    public AiProvider toOfficialDomain(CreateAiProviderDTO dto) {
        if (dto == null) {
            return null;
        }
        
        AiProvider provider = new AiProvider();
        provider.setProviderCode(dto.getProviderCode());
        provider.setProviderName(dto.getProviderName());
        provider.setProtocol(dto.getProtocol());
        provider.setDescription(dto.getDescription());
        provider.setDefaultBaseUrl(dto.getDefaultBaseUrl());
        provider.setConfigJson(dto.getConfigJson());
        provider.setPriority(dto.getPriority() != null ? dto.getPriority() : 100);
        
        // 官方服务商
        provider.setIsOfficial(true);
        provider.setUserId(null);
        provider.setStatus(1); // 默认启用
        
        return provider;
    }
    
    /**
     * 更新Domain实体（从UpdateDTO）
     *
     * @param provider 原领域实体
     * @param dto 更新DTO
     */
    public void updateDomain(AiProvider provider, UpdateAiProviderDTO dto) {
        if (provider == null || dto == null) {
            return;
        }
        
        if (dto.getProviderName() != null) {
            provider.setProviderName(dto.getProviderName());
        }
        
        if (dto.getDescription() != null) {
            provider.setDescription(dto.getDescription());
        }
        
        if (dto.getDefaultBaseUrl() != null) {
            provider.setDefaultBaseUrl(dto.getDefaultBaseUrl());
        }
        
        if (dto.getConfigJson() != null) {
            provider.setConfigJson(dto.getConfigJson());
        }
        
        if (dto.getPriority() != null) {
            provider.setPriority(dto.getPriority());
        }
    }
    
    /**
     * Domain实体列表转DTO列表
     *
     * @param providers 领域实体列表
     * @return DTO列表
     */
    public List<AiProviderDTO> toDTOList(List<AiProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<AiProviderDTO> dtoList = new ArrayList<>(providers.size());
        for (AiProvider provider : providers) {
            dtoList.add(toDTO(provider));
        }
        return dtoList;
    }
    
    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "启用" : "禁用";
    }
}
