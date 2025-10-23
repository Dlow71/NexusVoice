package com.nexusvoice.infrastructure.repository.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rag.model.entity.DocumentProcessTask;
import com.nexusvoice.domain.rag.model.enums.TaskStatus;
import com.nexusvoice.domain.rag.model.enums.TaskType;
import com.nexusvoice.domain.rag.repository.DocumentProcessTaskRepository;
import com.nexusvoice.infrastructure.persistence.converter.DocumentProcessTaskPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.DocumentProcessTaskPOMapper;
import com.nexusvoice.infrastructure.persistence.po.DocumentProcessTaskPO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文档处理任务仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Repository
public class DocumentProcessTaskRepositoryImpl implements DocumentProcessTaskRepository {

    private final DocumentProcessTaskPOMapper mapper;
    private final DocumentProcessTaskPOConverter converter;

    public DocumentProcessTaskRepositoryImpl(DocumentProcessTaskPOMapper mapper, DocumentProcessTaskPOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public DocumentProcessTask save(DocumentProcessTask task) {
        DocumentProcessTaskPO po = converter.toPO(task);
        if (task.getId() == null) {
            mapper.insert(po);
            task.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return task;
    }

    @Override
    public Optional<DocumentProcessTask> findById(Long id) {
        DocumentProcessTaskPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<DocumentProcessTask> findByFileId(Long fileId) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentProcessTaskPO::getFileId, fileId)
               .eq(DocumentProcessTaskPO::getDeleted, 0)
               .orderByDesc(DocumentProcessTaskPO::getCreatedAt);
        
        List<DocumentProcessTaskPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }


    @Override
    public List<DocumentProcessTask> findByStatus(TaskStatus status, int limit) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentProcessTaskPO::getStatus, status.name())
               .eq(DocumentProcessTaskPO::getDeleted, 0)
               .orderByAsc(DocumentProcessTaskPO::getCreatedAt)
               .last("LIMIT " + limit);
        
        List<DocumentProcessTaskPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentProcessTask> findByTaskType(TaskType taskType, int limit) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentProcessTaskPO::getTaskType, taskType.name())
               .eq(DocumentProcessTaskPO::getDeleted, 0)
               .orderByDesc(DocumentProcessTaskPO::getPriority)
               .orderByAsc(DocumentProcessTaskPO::getScheduledAt)
               .last("LIMIT " + limit);
        
        List<DocumentProcessTaskPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentProcessTask> findPendingTasks(int limit) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentProcessTaskPO::getStatus, TaskStatus.PENDING.name())
               .eq(DocumentProcessTaskPO::getDeleted, 0)
               .and(w -> w.isNull(DocumentProcessTaskPO::getScheduledAt)
                       .or()
                       .le(DocumentProcessTaskPO::getScheduledAt, LocalDateTime.now()))
               .orderByDesc(DocumentProcessTaskPO::getPriority)
               .orderByAsc(DocumentProcessTaskPO::getScheduledAt)
               .last("LIMIT " + limit);
        
        List<DocumentProcessTaskPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentProcessTask> findRetryableTasks(int limit) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentProcessTaskPO::getStatus, TaskStatus.FAILED.name())
               .apply("retry_count < max_retry")
               .eq(DocumentProcessTaskPO::getDeleted, 0)
               .orderByAsc(DocumentProcessTaskPO::getCreatedAt)
               .last("LIMIT " + limit);
        
        List<DocumentProcessTaskPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentProcessTask> findTimeoutTasks(int timeoutMinutes) {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentProcessTaskPO::getStatus, TaskStatus.RUNNING.name())
               .le(DocumentProcessTaskPO::getStartedAt, timeoutThreshold)
               .eq(DocumentProcessTaskPO::getDeleted, 0)
               .orderByAsc(DocumentProcessTaskPO::getStartedAt);
        
        List<DocumentProcessTaskPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentProcessTask> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(DocumentProcessTaskPO::getCreatedAt, startTime, endTime)
               .eq(DocumentProcessTaskPO::getDeleted, 0)
               .orderByDesc(DocumentProcessTaskPO::getCreatedAt);
        
        List<DocumentProcessTaskPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentProcessTask> saveAll(List<DocumentProcessTask> tasks) {
        tasks.forEach(this::save);
        return tasks;
    }

    @Override
    public DocumentProcessTask update(DocumentProcessTask task) {
        return save(task);
    }

    @Override
    public void updateStatus(Long id, TaskStatus status) {
        DocumentProcessTaskPO po = new DocumentProcessTaskPO();
        po.setId(id);
        po.setStatus(status.name());
        mapper.updateById(po);
    }

    @Override
    public void updateProgress(Long id, TaskStatus status, LocalDateTime startedAt) {
        DocumentProcessTaskPO po = new DocumentProcessTaskPO();
        po.setId(id);
        po.setStatus(status.name());
        po.setStartedAt(startedAt);
        mapper.updateById(po);
    }

    @Override
    public void completeTask(Long id, LocalDateTime completedAt) {
        DocumentProcessTaskPO po = new DocumentProcessTaskPO();
        po.setId(id);
        po.setStatus(TaskStatus.SUCCESS.name());
        po.setCompletedAt(completedAt);
        mapper.updateById(po);
    }

    @Override
    public void failTask(Long id, String errorMessage) {
        DocumentProcessTaskPO po = new DocumentProcessTaskPO();
        po.setId(id);
        po.setStatus(TaskStatus.FAILED.name());
        po.setCompletedAt(LocalDateTime.now());
        po.setErrorMessage(errorMessage);
        mapper.updateById(po);
    }

    @Override
    public void incrementRetryCount(Long id) {
        DocumentProcessTaskPO existingPo = mapper.selectById(id);
        if (existingPo != null) {
            DocumentProcessTaskPO updatePo = new DocumentProcessTaskPO();
            updatePo.setId(id);
            updatePo.setRetryCount(existingPo.getRetryCount() + 1);
            mapper.updateById(updatePo);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        DocumentProcessTaskPO po = new DocumentProcessTaskPO();
        po.setId(id);
        po.setDeleted(1);
        return mapper.updateById(po) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DocumentProcessTaskPO::getId, ids)
               .eq(DocumentProcessTaskPO::getDeleted, 0);
        
        DocumentProcessTaskPO updatePo = new DocumentProcessTaskPO();
        updatePo.setDeleted(1);
        return mapper.update(updatePo, wrapper);
    }

    @Override
    public int deleteByFileId(Long fileId) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentProcessTaskPO::getFileId, fileId)
               .eq(DocumentProcessTaskPO::getDeleted, 0);
        
        DocumentProcessTaskPO updatePo = new DocumentProcessTaskPO();
        updatePo.setDeleted(1);
        return mapper.update(updatePo, wrapper);
    }

    @Override
    public int cancelByFileId(Long fileId) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentProcessTaskPO::getFileId, fileId)
               .in(DocumentProcessTaskPO::getStatus, TaskStatus.PENDING.name(), TaskStatus.RUNNING.name())
               .eq(DocumentProcessTaskPO::getDeleted, 0);
        
        DocumentProcessTaskPO updatePo = new DocumentProcessTaskPO();
        updatePo.setStatus(TaskStatus.CANCELLED.name());
        updatePo.setCompletedAt(LocalDateTime.now());
        return mapper.update(updatePo, wrapper);
    }

    @Override
    public int countByStatus(TaskStatus status) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentProcessTaskPO::getStatus, status.name())
               .eq(DocumentProcessTaskPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public int countByFileIdAndType(Long fileId, TaskType taskType) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentProcessTaskPO::getFileId, fileId)
               .eq(DocumentProcessTaskPO::getTaskType, taskType.name())
               .eq(DocumentProcessTaskPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public boolean hasRunningTasks(Long fileId) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentProcessTaskPO::getFileId, fileId)
               .eq(DocumentProcessTaskPO::getStatus, TaskStatus.RUNNING.name())
               .eq(DocumentProcessTaskPO::getDeleted, 0)
               .last("LIMIT 1");
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean hasPendingTasks(Long fileId) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentProcessTaskPO::getFileId, fileId)
               .eq(DocumentProcessTaskPO::getStatus, TaskStatus.PENDING.name())
               .eq(DocumentProcessTaskPO::getDeleted, 0)
               .last("LIMIT 1");
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public DocumentProcessTaskRepository.TaskStatistics getStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<DocumentProcessTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(DocumentProcessTaskPO::getCreatedAt, startTime, endTime)
               .eq(DocumentProcessTaskPO::getDeleted, 0);
        
        List<DocumentProcessTaskPO> poList = mapper.selectList(wrapper);
        
        DocumentProcessTaskRepository.TaskStatistics statistics = new DocumentProcessTaskRepository.TaskStatistics();
        statistics.setTotalTasks(poList.size());
        
        int successCount = 0;
        int failedCount = 0;
        int pendingCount = 0;
        int runningCount = 0;
        long totalExecutionTime = 0;
        int completedCount = 0;
        
        for (DocumentProcessTaskPO po : poList) {
            String status = po.getStatus();
            if (TaskStatus.SUCCESS.name().equals(status)) {
                successCount++;
                if (po.getStartedAt() != null && po.getCompletedAt() != null) {
                    totalExecutionTime += java.time.Duration.between(po.getStartedAt(), po.getCompletedAt()).toMillis();
                    completedCount++;
                }
            } else if (TaskStatus.FAILED.name().equals(status)) {
                failedCount++;
            } else if (TaskStatus.PENDING.name().equals(status)) {
                pendingCount++;
            } else if (TaskStatus.RUNNING.name().equals(status)) {
                runningCount++;
            }
        }
        
        statistics.setSuccessTasks(successCount);
        statistics.setFailedTasks(failedCount);
        statistics.setPendingTasks(pendingCount);
        statistics.setRunningTasks(runningCount);
        statistics.setAvgExecutionTime(completedCount > 0 ? totalExecutionTime / completedCount : 0);
        
        return statistics;
    }
}
