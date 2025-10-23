package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.rag.model.entity.RagVersion;
import com.nexusvoice.infrastructure.persistence.po.RagVersionPO;
import org.springframework.stereotype.Component;

/**
 * RagVersion实体与RagVersionPO转换器
 * 负责领域模型与持久化模型之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Component
public class RagVersionPOConverter {

    /**
     * 将领域实体转换为持久化对象
     */
    public RagVersionPO toPO(RagVersion entity) {
        if (entity == null) {
            return null;
        }
        
        RagVersionPO po = new RagVersionPO();
        
        // 基础字段
        po.setId(entity.getId());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        po.setDeleted(entity.getDeleted());
        
        // 业务字段
        po.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        po.setUserId(entity.getUserId());
        po.setName(entity.getName());
        po.setIcon(entity.getIcon());
        po.setDescription(entity.getDescription());
        po.setVersion(entity.getVersion());
        po.setChangeLog(entity.getChangeLog());
        po.setLabels(entity.getLabels());
        po.setFileCount(entity.getFileCount());
        po.setTotalSize(entity.getTotalSize());
        po.setDocumentCount(entity.getDocumentCount());
        po.setPublishedAt(entity.getPublishedAt());
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public RagVersion toDomain(RagVersionPO po) {
        if (po == null) {
            return null;
        }
        
        RagVersion entity = new RagVersion();
        
        // 基础字段
        entity.setId(po.getId());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setDeleted(po.getDeleted());
        
        // 业务字段
        entity.setKnowledgeBaseId(po.getKnowledgeBaseId());
        entity.setUserId(po.getUserId());
        entity.setName(po.getName());
        entity.setIcon(po.getIcon());
        entity.setDescription(po.getDescription());
        entity.setVersion(po.getVersion());
        entity.setChangeLog(po.getChangeLog());
        entity.setLabels(po.getLabels());
        entity.setFileCount(po.getFileCount());
        entity.setTotalSize(po.getTotalSize());
        entity.setDocumentCount(po.getDocumentCount());
        entity.setPublishedAt(po.getPublishedAt());
        
        return entity;
    }
}
