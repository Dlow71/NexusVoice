package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.rag.model.entity.RagVersionFile;
import com.nexusvoice.infrastructure.persistence.po.RagVersionFilePO;
import org.springframework.stereotype.Component;

/**
 * RagVersionFile实体与RagVersionFilePO转换器
 * 负责领域模型与持久化模型之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Component
public class RagVersionFilePOConverter {

    /**
     * 将领域实体转换为持久化对象
     */
    public RagVersionFilePO toPO(RagVersionFile entity) {
        if (entity == null) {
            return null;
        }
        
        RagVersionFilePO po = new RagVersionFilePO();
        
        // 基础字段
        po.setId(entity.getId());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        po.setDeleted(entity.getDeleted());
        
        // 业务字段
        po.setRagVersionId(entity.getRagVersionId());
        po.setOriginalFileId(entity.getOriginalFileId());
        po.setFileName(entity.getFileName());
        po.setFileSize(entity.getFileSize());
        po.setFilePageSize(entity.getFilePageSize());
        po.setFileType(entity.getFileType()); // 快照数据，直接保存String
        po.setFilePath(entity.getFilePath());
        po.setProcessStatus(entity.getProcessStatus()); // 快照数据，直接保存Integer
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public RagVersionFile toDomain(RagVersionFilePO po) {
        if (po == null) {
            return null;
        }
        
        RagVersionFile entity = new RagVersionFile();
        
        // 基础字段
        entity.setId(po.getId());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setDeleted(po.getDeleted());
        
        // 业务字段
        entity.setRagVersionId(po.getRagVersionId());
        entity.setOriginalFileId(po.getOriginalFileId());
        entity.setFileName(po.getFileName());
        entity.setFileSize(po.getFileSize());
        entity.setFilePageSize(po.getFilePageSize());
        entity.setFileType(po.getFileType()); // 快照数据，直接使用String
        entity.setFilePath(po.getFilePath());
        entity.setProcessStatus(po.getProcessStatus()); // 快照数据，直接使用Integer
        
        return entity;
    }
}
