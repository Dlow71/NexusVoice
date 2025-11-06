package com.nexusvoice.domain.agent.model;

import com.nexusvoice.domain.agent.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent任务实体（纯POJO）
 * 
 * 职责：
 * - 表示Agent执行的单个任务
 * - 管理任务依赖关系
 * - 跟踪任务执行状态和结果
 * 
 * 使用场景：
 * - Plan+Execute模式中的子任务
 * - 多步骤任务的单个步骤
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTask {
    
    /**
     * 任务ID（唯一标识）
     */
    private String taskId;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 依赖的任务ID列表（必须先完成这些任务）
     */
    @Builder.Default
    private List<String> dependencies = new ArrayList<>();
    
    /**
     * 任务状态
     */
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;
    
    /**
     * 是否可以并行执行（与其他任务）
     */
    @Builder.Default
    private Boolean canParallel = false;
    
    /**
     * 执行结果
     */
    private String result;
    
    /**
     * 使用的工具名称
     */
    private String toolName;
    
    /**
     * 工具参数（JSON格式）
     */
    private String toolParams;
    
    /**
     * 执行顺序（规划时确定）
     */
    private Integer executionOrder;
    
    /**
     * 重试次数
     */
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * 最大重试次数
     */
    @Builder.Default
    private Integer maxRetries = 3;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 检查任务是否可以执行（依赖已满足）
     */
    public boolean canExecute(List<AgentTask> allTasks) {
        if (status != TaskStatus.PENDING) {
            return false;
        }
        
        if (dependencies == null || dependencies.isEmpty()) {
            return true;
        }
        
        // 检查所有依赖任务是否已完成
        return allTasks.stream()
            .filter(t -> dependencies.contains(t.getTaskId()))
            .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);
    }
    
    /**
     * 标记任务开始执行
     */
    public void markAsRunning() {
        this.status = TaskStatus.RUNNING;
    }
    
    /**
     * 标记任务完成
     */
    public void markAsCompleted(String result) {
        this.status = TaskStatus.COMPLETED;
        this.result = result;
    }
    
    /**
     * 标记任务失败
     */
    public void markAsFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
    }
    
    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount++;
        this.status = TaskStatus.PENDING;
    }
}

