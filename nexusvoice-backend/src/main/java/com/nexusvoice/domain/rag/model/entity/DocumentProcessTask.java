package com.nexusvoice.domain.rag.model.entity;

import com.nexusvoice.domain.common.BaseDomainEntity;
import com.nexusvoice.domain.rag.model.enums.TaskStatus;
import com.nexusvoice.domain.rag.model.enums.TaskType;

import java.time.LocalDateTime;

/**
 * 文档处理任务领域实体
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public class DocumentProcessTask extends BaseDomainEntity {
    
    /**
     * 关联文件ID
     */
    private Long fileId;
    
    /**
     * 任务类型
     */
    private TaskType taskType;
    
    /**
     * 任务状态
     */
    private TaskStatus status;
    
    /**
     * 优先级（数值越大优先级越高）
     */
    private Integer priority;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetry;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 计划执行时间
     */
    private LocalDateTime scheduledAt;
    
    /**
     * 开始执行时间
     */
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
    
    // 构造函数
    public DocumentProcessTask() {
        super();
        this.status = TaskStatus.PENDING;
        this.priority = 0;
        this.retryCount = 0;
        this.maxRetry = 3;
    }
    
    public DocumentProcessTask(Long fileId, TaskType taskType) {
        this();
        this.fileId = fileId;
        this.taskType = taskType;
    }
    
    // 业务方法
    
    /**
     * 开始执行任务
     */
    public void start() {
        if (this.status == TaskStatus.PENDING) {
            this.status = TaskStatus.RUNNING;
            this.startedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 完成任务
     */
    public void complete() {
        if (this.status == TaskStatus.RUNNING) {
            this.status = TaskStatus.SUCCESS;
            this.completedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 任务失败
     */
    public void fail(String errorMessage) {
        if (this.status == TaskStatus.RUNNING) {
            this.status = TaskStatus.FAILED;
            this.errorMessage = errorMessage;
            this.completedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 取消任务
     */
    public void cancel() {
        if (!this.status.isFinalStatus()) {
            this.status = TaskStatus.CANCELLED;
            this.completedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 重试任务
     */
    public boolean retry() {
        if (canRetry()) {
            this.retryCount = this.retryCount + 1;
            this.status = TaskStatus.PENDING;
            this.errorMessage = null;
            this.startedAt = null;
            this.completedAt = null;
            // 延迟重试（指数退避）
            this.scheduledAt = LocalDateTime.now().plusSeconds(calculateRetryDelay());
            return true;
        }
        return false;
    }
    
    /**
     * 计算重试延迟（指数退避）
     */
    private long calculateRetryDelay() {
        // 2^retryCount * 10秒，最大延迟5分钟
        long delay = (long) Math.pow(2, this.retryCount) * 10;
        return Math.min(delay, 300);
    }
    
    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return this.status.isRetryable() && this.retryCount < this.maxRetry;
    }
    
    /**
     * 是否已完成（成功或失败或取消）
     */
    public boolean isCompleted() {
        return this.status.isFinalStatus();
    }
    
    /**
     * 是否执行成功
     */
    public boolean isSuccess() {
        return this.status == TaskStatus.SUCCESS;
    }
    
    /**
     * 是否执行失败
     */
    public boolean isFailed() {
        return this.status == TaskStatus.FAILED;
    }
    
    /**
     * 是否正在执行
     */
    public boolean isRunning() {
        return this.status == TaskStatus.RUNNING;
    }
    
    /**
     * 是否待执行
     */
    public boolean isPending() {
        return this.status == TaskStatus.PENDING;
    }
    
    /**
     * 是否应该执行（到达计划时间）
     */
    public boolean shouldExecute() {
        if (this.status != TaskStatus.PENDING) {
            return false;
        }
        if (this.scheduledAt == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(this.scheduledAt);
    }
    
    /**
     * 提高优先级
     */
    public void increasePriority(int delta) {
        this.priority = this.priority + delta;
    }
    
    /**
     * 降低优先级
     */
    public void decreasePriority(int delta) {
        this.priority = Math.max(0, this.priority - delta);
    }
    
    /**
     * 设置为高优先级
     */
    public void setHighPriority() {
        this.priority = 100;
    }
    
    /**
     * 设置为低优先级
     */
    public void setLowPriority() {
        this.priority = 0;
    }
    
    /**
     * 获取执行耗时（毫秒）
     */
    public Long getExecutionTime() {
        if (this.startedAt == null || this.completedAt == null) {
            return null;
        }
        return java.time.Duration.between(this.startedAt, this.completedAt).toMillis();
    }
    
    // 领域规则验证
    
    /**
     * 验证是否可以执行
     */
    public boolean canExecute() {
        return this.status == TaskStatus.PENDING && shouldExecute() && !isDeleted();
    }
    
    /**
     * 验证是否可以取消
     */
    public boolean canCancel() {
        return !this.status.isFinalStatus();
    }
    
    // Getters and Setters
    public Long getFileId() {
        return fileId;
    }
    
    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }
    
    public TaskType getTaskType() {
        return taskType;
    }
    
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public Integer getMaxRetry() {
        return maxRetry;
    }
    
    public void setMaxRetry(Integer maxRetry) {
        this.maxRetry = maxRetry;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }
    
    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
