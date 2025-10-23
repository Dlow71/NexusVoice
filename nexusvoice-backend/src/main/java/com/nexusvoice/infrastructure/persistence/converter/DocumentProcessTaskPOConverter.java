package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.rag.model.entity.DocumentProcessTask;
import com.nexusvoice.domain.rag.model.enums.TaskType;
import com.nexusvoice.domain.rag.model.enums.TaskStatus;
import com.nexusvoice.infrastructure.persistence.po.DocumentProcessTaskPO;
import org.springframework.stereotype.Component;

/**
 * DocumentProcessTask实体与DocumentProcessTaskPO转换器
 * 负责领域模型与持久化模型之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Component
public class DocumentProcessTaskPOConverter {

    /**
     * 将领域实体转换为持久化对象
     */
    public DocumentProcessTaskPO toPO(DocumentProcessTask entity) {
        if (entity == null) {
            return null;
        }
        
        DocumentProcessTaskPO po = new DocumentProcessTaskPO();
        
        // 基础字段
        po.setId(entity.getId());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        po.setDeleted(entity.getDeleted());
        
        // 业务字段
        po.setFileId(entity.getFileId());
        po.setTaskType(entity.getTaskType() != null ? entity.getTaskType().name() : null);
        po.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        po.setPriority(entity.getPriority());
        po.setRetryCount(entity.getRetryCount());
        po.setMaxRetry(entity.getMaxRetry());
        po.setErrorMessage(entity.getErrorMessage());
        po.setScheduledAt(entity.getScheduledAt());
        po.setStartedAt(entity.getStartedAt());
        po.setCompletedAt(entity.getCompletedAt());
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public DocumentProcessTask toDomain(DocumentProcessTaskPO po) {
        if (po == null) {
            return null;
        }
        
        DocumentProcessTask entity = new DocumentProcessTask();
        
        // 基础字段
        entity.setId(po.getId());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setDeleted(po.getDeleted());
        
        // 业务字段
        entity.setFileId(po.getFileId());
        entity.setTaskType(po.getTaskType() != null ? TaskType.valueOf(po.getTaskType()) : null);
        entity.setStatus(po.getStatus() != null ? TaskStatus.valueOf(po.getStatus()) : null);
        entity.setPriority(po.getPriority());
        entity.setRetryCount(po.getRetryCount());
        entity.setMaxRetry(po.getMaxRetry());
        entity.setErrorMessage(po.getErrorMessage());
        entity.setScheduledAt(po.getScheduledAt());
        entity.setStartedAt(po.getStartedAt());
        entity.setCompletedAt(po.getCompletedAt());
        
        return entity;
    }
}
