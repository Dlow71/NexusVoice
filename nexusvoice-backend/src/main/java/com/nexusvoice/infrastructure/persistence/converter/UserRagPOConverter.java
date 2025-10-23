package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.rag.model.entity.UserRag;
import com.nexusvoice.domain.rag.model.enums.InstallType;
import com.nexusvoice.infrastructure.persistence.po.UserRagPO;
import org.springframework.stereotype.Component;

/**
 * UserRag实体与UserRagPO转换器
 * 负责领域模型与持久化模型之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Component
public class UserRagPOConverter {

    /**
     * 将领域实体转换为持久化对象
     */
    public UserRagPO toPO(UserRag entity) {
        if (entity == null) {
            return null;
        }
        
        UserRagPO po = new UserRagPO();
        
        // 基础字段
        po.setId(entity.getId());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        po.setDeleted(entity.getDeleted());
        
        // 业务字段
        po.setUserId(entity.getUserId());
        po.setRagVersionId(entity.getRagVersionId());
        po.setOriginalKnowledgeBaseId(entity.getOriginalKnowledgeBaseId());
        po.setName(entity.getName());
        po.setIcon(entity.getIcon());
        po.setDescription(entity.getDescription());
        po.setVersion(entity.getVersion());
        po.setInstallType(entity.getInstallType() != null ? entity.getInstallType().name() : null);
        po.setInstalledAt(entity.getInstalledAt());
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public UserRag toDomain(UserRagPO po) {
        if (po == null) {
            return null;
        }
        
        UserRag entity = new UserRag();
        
        // 基础字段
        entity.setId(po.getId());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setDeleted(po.getDeleted());
        
        // 业务字段
        entity.setUserId(po.getUserId());
        entity.setRagVersionId(po.getRagVersionId());
        entity.setOriginalKnowledgeBaseId(po.getOriginalKnowledgeBaseId());
        entity.setName(po.getName());
        entity.setIcon(po.getIcon());
        entity.setDescription(po.getDescription());
        entity.setVersion(po.getVersion());
        entity.setInstallType(po.getInstallType() != null ? InstallType.valueOf(po.getInstallType()) : null);
        entity.setInstalledAt(po.getInstalledAt());
        
        return entity;
    }
}
