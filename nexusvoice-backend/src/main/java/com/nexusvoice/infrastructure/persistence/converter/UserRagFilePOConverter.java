package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.rag.model.entity.UserRagFile;
import com.nexusvoice.infrastructure.persistence.po.UserRagFilePO;
import org.springframework.stereotype.Component;

/**
 * UserRagFile实体与UserRagFilePO转换器
 * 负责领域模型与持久化模型之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Component
public class UserRagFilePOConverter {

    /**
     * 将领域实体转换为持久化对象
     */
    public UserRagFilePO toPO(UserRagFile entity) {
        if (entity == null) {
            return null;
        }
        
        UserRagFilePO po = new UserRagFilePO();
        
        // 基础字段
        po.setId(entity.getId());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        po.setDeleted(entity.getDeleted());
        
        // 业务字段
        po.setUserRagId(entity.getUserRagId());
        po.setOriginalFileId(entity.getOriginalFileId());
        po.setFileName(entity.getFileName());
        po.setFileSize(entity.getFileSize());
        po.setFilePageSize(entity.getFilePageSize());
        po.setFileType(entity.getFileType());
        po.setFilePath(entity.getFilePath());
        po.setProcessStatus(entity.getProcessStatus());
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public UserRagFile toDomain(UserRagFilePO po) {
        if (po == null) {
            return null;
        }
        
        UserRagFile entity = new UserRagFile();
        
        // 基础字段
        entity.setId(po.getId());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setDeleted(po.getDeleted());
        
        // 业务字段
        entity.setUserRagId(po.getUserRagId());
        entity.setOriginalFileId(po.getOriginalFileId());
        entity.setFileName(po.getFileName());
        entity.setFileSize(po.getFileSize());
        entity.setFilePageSize(po.getFilePageSize());
        entity.setFileType(po.getFileType());
        entity.setFilePath(po.getFilePath());
        entity.setProcessStatus(po.getProcessStatus());
        
        return entity;
    }
}
