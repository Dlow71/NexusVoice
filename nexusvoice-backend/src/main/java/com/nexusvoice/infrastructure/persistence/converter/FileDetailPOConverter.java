package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.rag.model.entity.FileDetail;
import com.nexusvoice.domain.rag.model.enums.FileType;
import com.nexusvoice.domain.rag.model.enums.ParseStrategy;
import com.nexusvoice.domain.rag.model.enums.ProcessStatus;
import com.nexusvoice.infrastructure.persistence.po.FileDetailPO;
import org.springframework.stereotype.Component;

/**
 * FileDetail实体与FileDetailPO转换器
 * 负责领域模型与持久化模型之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Component
public class FileDetailPOConverter {

    /**
     * 将领域实体转换为持久化对象
     */
    public FileDetailPO toPO(FileDetail entity) {
        if (entity == null) {
            return null;
        }
        
        FileDetailPO po = new FileDetailPO();
        
        // 基础字段
        po.setId(entity.getId());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        po.setDeleted(entity.getDeleted());
        
        // 业务字段
        po.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        po.setFileName(entity.getFileName());
        po.setFileSize(entity.getFileSize());
        po.setFilePageSize(entity.getFilePageSize());
        po.setFileType(entity.getFileType() != null ? entity.getFileType().name() : null);
        po.setFilePath(entity.getFilePath());
        po.setFileHash(entity.getFileHash());
        po.setParseStrategy(entity.getParseStrategy() != null ? entity.getParseStrategy().name() : null);
        po.setProcessStatus(entity.getProcessStatus() != null ? entity.getProcessStatus().getValue() : null);
        po.setErrorMessage(entity.getErrorMessage());
        po.setMetadata(null); // FileDetail没有metadata字段
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public FileDetail toDomain(FileDetailPO po) {
        if (po == null) {
            return null;
        }
        
        FileDetail entity = new FileDetail();
        
        // 基础字段
        entity.setId(po.getId());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setDeleted(po.getDeleted());
        
        // 业务字段
        entity.setKnowledgeBaseId(po.getKnowledgeBaseId());
        entity.setFileName(po.getFileName());
        entity.setFileSize(po.getFileSize());
        entity.setFilePageSize(po.getFilePageSize());
        entity.setFileType(po.getFileType() != null ? FileType.valueOf(po.getFileType()) : null);
        entity.setFilePath(po.getFilePath());
        entity.setFileHash(po.getFileHash());
        entity.setParseStrategy(po.getParseStrategy() != null ? ParseStrategy.valueOf(po.getParseStrategy()) : null);
        entity.setProcessStatus(po.getProcessStatus() != null ? ProcessStatus.fromValue(po.getProcessStatus()) : null);
        entity.setErrorMessage(po.getErrorMessage());
        // metadata字段忽略，FileDetail没有此字段
        
        return entity;
    }
}
