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
        po.setUserId(entity.getUserId());
        po.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        po.setFilename(entity.getFileName());
        po.setOriginalName(entity.getOriginalName());
        po.setFileSize(entity.getFileSize());
        po.setFilePageCount(entity.getFilePageSize());
        po.setFileType(entity.getFileType() != null ? entity.getFileType().name() : null);
        po.setMimeType(entity.getMimeType());
        po.setStorageProvider(entity.getStorageProvider());
        po.setStorageKey(entity.getStorageKey() != null ? entity.getStorageKey() : entity.getFilePath());
        po.setStorageUrl(entity.getFilePath());
        po.setFileHash(entity.getFileHash());
        po.setParseStrategy(entity.getParseStrategy() != null ? entity.getParseStrategy().name() : null);
        po.setStatus(entity.getProcessStatus() != null ? entity.getProcessStatus().name() : null);
        po.setCurrentProcessPage(entity.getCurrentProcessPage());
        po.setProcessProgress(entity.getProcessProgress());
        po.setErrorCode(entity.getErrorCode());
        po.setErrorMessage(entity.getErrorMessage());
        po.setProcessedAt(entity.getProcessedAt());
        
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
        entity.setUserId(po.getUserId());
        entity.setKnowledgeBaseId(po.getKnowledgeBaseId());
        entity.setFileName(po.getFilename());
        entity.setOriginalName(po.getOriginalName());
        entity.setFileSize(po.getFileSize());
        entity.setFilePageSize(po.getFilePageCount());
        entity.setFileType(po.getFileType() != null ? FileType.valueOf(po.getFileType()) : null);
        entity.setMimeType(po.getMimeType());
        entity.setStorageProvider(po.getStorageProvider());
        entity.setStorageKey(po.getStorageKey());
        entity.setFilePath(po.getStorageKey() != null ? po.getStorageKey() : po.getStorageUrl());
        entity.setFileHash(po.getFileHash());
        entity.setParseStrategy(po.getParseStrategy() != null ? ParseStrategy.valueOf(po.getParseStrategy()) : null);
        entity.setProcessStatus(po.getStatus() != null ? ProcessStatus.valueOf(po.getStatus()) : null);
        entity.setCurrentProcessPage(po.getCurrentProcessPage());
        entity.setProcessProgress(po.getProcessProgress());
        entity.setErrorCode(po.getErrorCode());
        entity.setErrorMessage(po.getErrorMessage());
        entity.setProcessedAt(po.getProcessedAt());
        
        return entity;
    }
}
