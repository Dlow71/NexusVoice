package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.ai.model.AiProvider;
import com.nexusvoice.infrastructure.persistence.po.AiProviderPO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI服务提供商PO与Domain实体转换器
 * 负责PO和领域实体之间的相互转换
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
@Component
public class AiProviderPOConverter {
    
    /**
     * PO转Domain实体
     *
     * @param po 持久化对象
     * @return 领域实体
     */
    public AiProvider toDomain(AiProviderPO po) {
        if (po == null) {
            return null;
        }
        
        AiProvider provider = new AiProvider();
        
        // 基础字段
        provider.setId(po.getId());
        provider.setCreatedAt(po.getCreatedAt());
        provider.setUpdatedAt(po.getUpdatedAt());
        provider.setDeleted(po.getDeleted());
        
        // 业务字段
        provider.setProviderCode(po.getProviderCode());
        provider.setProviderName(po.getProviderName());
        provider.setProtocol(po.getProtocol());
        provider.setDescription(po.getDescription());
        provider.setDefaultBaseUrl(po.getDefaultBaseUrl());
        provider.setConfigJson(po.getConfigJson());
        provider.setIsOfficial(po.getIsOfficial());
        provider.setUserId(po.getUserId());
        provider.setStatus(po.getStatus());
        provider.setPriority(po.getPriority());
        
        return provider;
    }
    
    /**
     * Domain实体转PO
     *
     * @param provider 领域实体
     * @return 持久化对象
     */
    public AiProviderPO toPO(AiProvider provider) {
        if (provider == null) {
            return null;
        }
        
        AiProviderPO po = new AiProviderPO();
        
        // 基础字段
        po.setId(provider.getId());
        po.setCreatedAt(provider.getCreatedAt());
        po.setUpdatedAt(provider.getUpdatedAt());
        po.setDeleted(provider.getDeleted());
        
        // 业务字段
        po.setProviderCode(provider.getProviderCode());
        po.setProviderName(provider.getProviderName());
        po.setProtocol(provider.getProtocol());
        po.setDescription(provider.getDescription());
        po.setDefaultBaseUrl(provider.getDefaultBaseUrl());
        po.setConfigJson(provider.getConfigJson());
        po.setIsOfficial(provider.getIsOfficial());
        po.setUserId(provider.getUserId());
        po.setStatus(provider.getStatus());
        po.setPriority(provider.getPriority());
        
        return po;
    }
    
    /**
     * PO列表转Domain实体列表
     *
     * @param poList PO列表
     * @return 领域实体列表
     */
    public List<AiProvider> toDomainList(List<AiProviderPO> poList) {
        if (poList == null || poList.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<AiProvider> providers = new ArrayList<>(poList.size());
        for (AiProviderPO po : poList) {
            providers.add(toDomain(po));
        }
        return providers;
    }
    
    /**
     * Domain实体列表转PO列表
     *
     * @param providers 领域实体列表
     * @return PO列表
     */
    public List<AiProviderPO> toPOList(List<AiProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<AiProviderPO> poList = new ArrayList<>(providers.size());
        for (AiProvider provider : providers) {
            poList.add(toPO(provider));
        }
        return poList;
    }
}
