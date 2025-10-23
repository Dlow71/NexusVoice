package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.rag.model.entity.RagVersionDocument;
import com.nexusvoice.infrastructure.persistence.po.RagVersionDocumentPO;
import org.springframework.stereotype.Component;

/**
 * RagVersionDocument实体与RagVersionDocumentPO转换器
 * 负责领域模型与持久化模型之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Component
public class RagVersionDocumentPOConverter {

    /**
     * 将领域实体转换为持久化对象
     */
    public RagVersionDocumentPO toPO(RagVersionDocument entity) {
        if (entity == null) {
            return null;
        }
        
        RagVersionDocumentPO po = new RagVersionDocumentPO();
        
        // 基础字段
        po.setId(entity.getId());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        po.setDeleted(entity.getDeleted());
        
        // 业务字段
        po.setRagVersionId(entity.getRagVersionId());
        po.setRagVersionFileId(entity.getRagVersionFileId());
        po.setOriginalDocumentId(entity.getOriginalDocumentId());
        po.setContent(entity.getContent());
        po.setPage(entity.getPage());
        po.setVectorId(entity.getVectorId());
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public RagVersionDocument toDomain(RagVersionDocumentPO po) {
        if (po == null) {
            return null;
        }
        
        RagVersionDocument entity = new RagVersionDocument();
        
        // 基础字段
        entity.setId(po.getId());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setDeleted(po.getDeleted());
        
        // 业务字段
        entity.setRagVersionId(po.getRagVersionId());
        entity.setRagVersionFileId(po.getRagVersionFileId());
        entity.setOriginalDocumentId(po.getOriginalDocumentId());
        entity.setContent(po.getContent());
        entity.setPage(po.getPage());
        entity.setVectorId(po.getVectorId());
        
        return entity;
    }
}
