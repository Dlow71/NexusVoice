package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.rag.model.entity.UserRagDocument;
import com.nexusvoice.infrastructure.persistence.po.UserRagDocumentPO;
import org.springframework.stereotype.Component;

/**
 * UserRagDocument实体与UserRagDocumentPO转换器
 * 负责领域模型与持久化模型之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Component
public class UserRagDocumentPOConverter {

    /**
     * 将领域实体转换为持久化对象
     */
    public UserRagDocumentPO toPO(UserRagDocument entity) {
        if (entity == null) {
            return null;
        }
        
        UserRagDocumentPO po = new UserRagDocumentPO();
        
        // 基础字段
        po.setId(entity.getId());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        po.setDeleted(entity.getDeleted());
        
        // 业务字段
        po.setUserRagId(entity.getUserRagId());
        po.setUserRagFileId(entity.getUserRagFileId());
        po.setOriginalDocumentId(entity.getOriginalDocumentId());
        po.setContent(entity.getContent());
        po.setPage(entity.getPage());
        po.setVectorId(entity.getVectorId());
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public UserRagDocument toDomain(UserRagDocumentPO po) {
        if (po == null) {
            return null;
        }
        
        UserRagDocument entity = new UserRagDocument();
        
        // 基础字段
        entity.setId(po.getId());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setDeleted(po.getDeleted());
        
        // 业务字段
        entity.setUserRagId(po.getUserRagId());
        entity.setUserRagFileId(po.getUserRagFileId());
        entity.setOriginalDocumentId(po.getOriginalDocumentId());
        entity.setContent(po.getContent());
        entity.setPage(po.getPage());
        entity.setVectorId(po.getVectorId());
        
        return entity;
    }
}
