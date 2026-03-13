package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.rag.model.entity.DocumentUnit;
import com.nexusvoice.infrastructure.persistence.po.DocumentUnitPO;
import org.springframework.stereotype.Component;

/**
 * DocumentUnit实体与DocumentUnitPO转换器
 * 负责领域模型与持久化模型之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Component
public class DocumentUnitPOConverter {

    /**
     * 将领域实体转换为持久化对象
     */
    public DocumentUnitPO toPO(DocumentUnit entity) {
        if (entity == null) {
            return null;
        }
        
        DocumentUnitPO po = new DocumentUnitPO();
        
        // 基础字段
        po.setId(entity.getId());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        po.setDeleted(entity.getDeleted());
        
        // 业务字段（尽量与表字段完整对齐）
        po.setFileId(entity.getFileId());
        po.setContent(entity.getContent());
        po.setPage(entity.getPage());
        po.setIsOcr(entity.getIsOcr());
        po.setIsVector(entity.getIsVector());
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public DocumentUnit toDomain(DocumentUnitPO po) {
        if (po == null) {
            return null;
        }
        
        DocumentUnit entity = new DocumentUnit();
        
        // 基础字段
        entity.setId(po.getId());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setDeleted(po.getDeleted());
        
        // 业务字段
        entity.setFileId(po.getFileId());
        entity.setContent(po.getContent());
        entity.setPage(po.getPage());
        entity.setIsOcr(po.getIsOcr());
        entity.setIsVector(po.getIsVector());
        
        return entity;
    }
}
