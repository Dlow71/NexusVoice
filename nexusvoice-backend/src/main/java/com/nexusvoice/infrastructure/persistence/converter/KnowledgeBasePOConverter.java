package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.rag.model.entity.KnowledgeBase;
import com.nexusvoice.domain.rag.model.enums.KnowledgeBaseStatus;
import com.nexusvoice.infrastructure.persistence.po.KnowledgeBasePO;
import org.springframework.stereotype.Component;

/**
 * KnowledgeBase实体与KnowledgeBasePO转换器
 * 负责领域模型与持久化模型之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Component
public class KnowledgeBasePOConverter {

    /**
     * 将领域实体转换为持久化对象
     */
    public KnowledgeBasePO toPO(KnowledgeBase entity) {
        if (entity == null) {
            return null;
        }
        
        KnowledgeBasePO po = new KnowledgeBasePO();
        
        // 基础字段
        po.setId(entity.getId());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        po.setDeleted(entity.getDeleted());
        
        // 业务字段
        po.setUserId(entity.getUserId());
        po.setName(entity.getName());
        po.setDescription(entity.getDescription());
        po.setIcon(entity.getIcon());
        po.setLabels(entity.getLabels());
        po.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        po.setFileCount(entity.getFileCount());
        po.setTotalSize(entity.getTotalSize());
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public KnowledgeBase toDomain(KnowledgeBasePO po) {
        if (po == null) {
            return null;
        }
        
        KnowledgeBase entity = new KnowledgeBase();
        
        // 基础字段
        entity.setId(po.getId());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setDeleted(po.getDeleted());
        
        // 业务字段
        entity.setUserId(po.getUserId());
        entity.setName(po.getName());
        entity.setDescription(po.getDescription());
        entity.setIcon(po.getIcon());
        entity.setLabels(po.getLabels());
        entity.setStatus(po.getStatus() != null ? KnowledgeBaseStatus.valueOf(po.getStatus()) : null);
        entity.setFileCount(po.getFileCount());
        entity.setTotalSize(po.getTotalSize());
        entity.setDocumentCount(po.getDocumentCount() != null ? po.getDocumentCount() : 0);
        
        return entity;
    }
}
