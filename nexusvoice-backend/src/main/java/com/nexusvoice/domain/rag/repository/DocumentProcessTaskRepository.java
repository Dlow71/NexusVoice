package com.nexusvoice.domain.rag.repository;

import com.nexusvoice.domain.rag.model.entity.DocumentProcessTask;
import com.nexusvoice.domain.rag.model.enums.TaskStatus;
import com.nexusvoice.domain.rag.model.enums.TaskType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文档处理任务仓储接口
 * 纯领域层接口，不依赖任何基础设施
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface DocumentProcessTaskRepository {
    
    /**
     * 保存任务
     * @param task 任务实体
     * @return 保存后的任务
     */
    DocumentProcessTask save(DocumentProcessTask task);
    
    /**
     * 批量保存任务
     * @param tasks 任务列表
     * @return 保存后的任务列表
     */
    List<DocumentProcessTask> saveAll(List<DocumentProcessTask> tasks);
    
    /**
     * 根据ID查找任务
     * @param id 任务ID
     * @return 任务实体
     */
    Optional<DocumentProcessTask> findById(Long id);
    
    /**
     * 根据文件ID查找任务列表
     * @param fileId 文件ID
     * @return 任务列表
     */
    List<DocumentProcessTask> findByFileId(Long fileId);
    
    /**
     * 根据状态查找任务列表
     * @param status 任务状态
     * @param limit 限制数量
     * @return 任务列表
     */
    List<DocumentProcessTask> findByStatus(TaskStatus status, int limit);
    
    /**
     * 根据任务类型查找任务列表
     * @param taskType 任务类型
     * @param limit 限制数量
     * @return 任务列表
     */
    List<DocumentProcessTask> findByTaskType(TaskType taskType, int limit);
    
    /**
     * 查找待执行的任务（按优先级和计划时间排序）
     * @param limit 限制数量
     * @return 任务列表
     */
    List<DocumentProcessTask> findPendingTasks(int limit);
    
    /**
     * 查找需要重试的任务
     * @param limit 限制数量
     * @return 任务列表
     */
    List<DocumentProcessTask> findRetryableTasks(int limit);
    
    /**
     * 查找超时的任务
     * @param timeoutMinutes 超时分钟数
     * @return 任务列表
     */
    List<DocumentProcessTask> findTimeoutTasks(int timeoutMinutes);
    
    /**
     * 查找指定时间范围内的任务
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 任务列表
     */
    List<DocumentProcessTask> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 更新任务
     * @param task 任务实体
     * @return 更新后的任务
     */
    DocumentProcessTask update(DocumentProcessTask task);
    
    /**
     * 更新任务状态
     * @param id 任务ID
     * @param status 新状态
     */
    void updateStatus(Long id, TaskStatus status);
    
    /**
     * 更新任务进度
     * @param id 任务ID
     * @param status 任务状态
     * @param startedAt 开始时间
     */
    void updateProgress(Long id, TaskStatus status, LocalDateTime startedAt);
    
    /**
     * 完成任务
     * @param id 任务ID
     * @param completedAt 完成时间
     */
    void completeTask(Long id, LocalDateTime completedAt);
    
    /**
     * 失败任务
     * @param id 任务ID
     * @param errorMessage 错误消息
     */
    void failTask(Long id, String errorMessage);
    
    /**
     * 增加重试次数
     * @param id 任务ID
     */
    void incrementRetryCount(Long id);
    
    /**
     * 删除任务（逻辑删除）
     * @param id 任务ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);
    
    /**
     * 批量删除任务
     * @param ids 任务ID列表
     * @return 删除的数量
     */
    int deleteByIds(List<Long> ids);
    
    /**
     * 根据文件ID删除所有任务
     * @param fileId 文件ID
     * @return 删除的数量
     */
    int deleteByFileId(Long fileId);
    
    /**
     * 取消文件的所有任务
     * @param fileId 文件ID
     * @return 取消的数量
     */
    int cancelByFileId(Long fileId);
    
    /**
     * 统计任务数量
     * @param status 任务状态
     * @return 任务数量
     */
    int countByStatus(TaskStatus status);
    
    /**
     * 统计文件的任务数量
     * @param fileId 文件ID
     * @param taskType 任务类型
     * @return 任务数量
     */
    int countByFileIdAndType(Long fileId, TaskType taskType);
    
    /**
     * 检查文件是否有正在执行的任务
     * @param fileId 文件ID
     * @return 是否有执行中的任务
     */
    boolean hasRunningTasks(Long fileId);
    
    /**
     * 检查文件是否有待处理的任务
     * @param fileId 文件ID
     * @return 是否有待处理的任务
     */
    boolean hasPendingTasks(Long fileId);
    
    /**
     * 获取任务执行统计
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息（成功数、失败数、平均执行时间等）
     */
    TaskStatistics getStatistics(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 任务统计信息
     */
    class TaskStatistics {
        private int totalTasks;
        private int successTasks;
        private int failedTasks;
        private int pendingTasks;
        private int runningTasks;
        private long avgExecutionTime;
        
        // Getters and Setters
        public int getTotalTasks() {
            return totalTasks;
        }
        
        public void setTotalTasks(int totalTasks) {
            this.totalTasks = totalTasks;
        }
        
        public int getSuccessTasks() {
            return successTasks;
        }
        
        public void setSuccessTasks(int successTasks) {
            this.successTasks = successTasks;
        }
        
        public int getFailedTasks() {
            return failedTasks;
        }
        
        public void setFailedTasks(int failedTasks) {
            this.failedTasks = failedTasks;
        }
        
        public int getPendingTasks() {
            return pendingTasks;
        }
        
        public void setPendingTasks(int pendingTasks) {
            this.pendingTasks = pendingTasks;
        }
        
        public int getRunningTasks() {
            return runningTasks;
        }
        
        public void setRunningTasks(int runningTasks) {
            this.runningTasks = runningTasks;
        }
        
        public long getAvgExecutionTime() {
            return avgExecutionTime;
        }
        
        public void setAvgExecutionTime(long avgExecutionTime) {
            this.avgExecutionTime = avgExecutionTime;
        }
    }
}
