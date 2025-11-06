package com.nexusvoice.domain.agent.model;

import com.nexusvoice.domain.agent.enums.PlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 计划执行领域模型（纯POJO）
 * 
 * 职责：
 * - 表示一个完整的任务执行计划
 * - 管理任务列表和依赖关系
 * - 跟踪整体执行状态
 * 
 * 使用场景：
 * - Plan+Solve模式的核心数据结构
 * - 复杂任务的分解和编排
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanExecution {
    
    /**
     * 计划ID（唯一标识）
     */
    private String planId;
    
    /**
     * 计划描述
     */
    private String description;
    
    /**
     * 原始用户查询
     */
    private String originalQuery;
    
    /**
     * 任务列表（按规划顺序）
     */
    @Builder.Default
    private List<AgentTask> tasks = new ArrayList<>();
    
    /**
     * 计划状态
     */
    @Builder.Default
    private PlanStatus status = PlanStatus.PENDING;
    
    /**
     * 上下文变量（用于任务间传递数据）
     */
    @Builder.Default
    private Map<String, Object> context = new java.util.HashMap<>();
    
    /**
     * 创建时间戳
     */
    private Long createdAt;
    
    /**
     * 开始执行时间戳
     */
    private Long startedAt;
    
    /**
     * 完成时间戳
     */
    private Long completedAt;
    
    /**
     * 总耗时（毫秒）
     */
    private Long totalDurationMs;
    
    /**
     * 执行结果摘要
     */
    private String resultSummary;
    
    /**
     * 获取待执行的任务
     */
    public List<AgentTask> getPendingTasks() {
        if (tasks == null) {
            return new ArrayList<>();
        }
        return tasks.stream()
            .filter(task -> task.getStatus() == com.nexusvoice.domain.agent.enums.TaskStatus.PENDING)
            .toList();
    }
    
    /**
     * 获取可执行的任务（依赖已满足）
     */
    public List<AgentTask> getExecutableTasks() {
        if (tasks == null) {
            return new ArrayList<>();
        }
        return tasks.stream()
            .filter(task -> task.canExecute(tasks))
            .toList();
    }
    
    /**
     * 获取已完成的任务
     */
    public List<AgentTask> getCompletedTasks() {
        if (tasks == null) {
            return new ArrayList<>();
        }
        return tasks.stream()
            .filter(task -> task.getStatus() == com.nexusvoice.domain.agent.enums.TaskStatus.COMPLETED)
            .toList();
    }
    
    /**
     * 获取失败的任务
     */
    public List<AgentTask> getFailedTasks() {
        if (tasks == null) {
            return new ArrayList<>();
        }
        return tasks.stream()
            .filter(task -> task.getStatus() == com.nexusvoice.domain.agent.enums.TaskStatus.FAILED)
            .toList();
    }
    
    /**
     * 检查计划是否完成（所有任务都完成或失败）
     */
    public boolean isCompleted() {
        if (tasks == null || tasks.isEmpty()) {
            return false;
        }
        return tasks.stream()
            .allMatch(task -> 
                task.getStatus() == com.nexusvoice.domain.agent.enums.TaskStatus.COMPLETED ||
                task.getStatus() == com.nexusvoice.domain.agent.enums.TaskStatus.FAILED ||
                task.getStatus() == com.nexusvoice.domain.agent.enums.TaskStatus.SKIPPED
            );
    }
    
    /**
     * 检查计划是否成功（所有任务都完成）
     */
    public boolean isSuccessful() {
        if (tasks == null || tasks.isEmpty()) {
            return false;
        }
        return tasks.stream()
            .allMatch(task -> task.getStatus() == com.nexusvoice.domain.agent.enums.TaskStatus.COMPLETED);
    }
    
    /**
     * 获取执行进度（0.0-1.0）
     */
    public double getProgress() {
        if (tasks == null || tasks.isEmpty()) {
            return 0.0;
        }
        long completedCount = tasks.stream()
            .filter(task -> 
                task.getStatus() == com.nexusvoice.domain.agent.enums.TaskStatus.COMPLETED ||
                task.getStatus() == com.nexusvoice.domain.agent.enums.TaskStatus.FAILED
            )
            .count();
        return (double) completedCount / tasks.size();
    }
    
    /**
     * 添加任务
     */
    public void addTask(AgentTask task) {
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        tasks.add(task);
    }
    
    /**
     * 根据ID查找任务
     */
    public AgentTask getTaskById(String taskId) {
        if (tasks == null) {
            return null;
        }
        return tasks.stream()
            .filter(task -> taskId.equals(task.getTaskId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 标记计划开始执行
     */
    public void markStarted() {
        this.status = PlanStatus.RUNNING;
        this.startedAt = System.currentTimeMillis();
    }
    
    /**
     * 标记计划完成
     */
    public void markCompleted() {
        this.status = isSuccessful() ? PlanStatus.COMPLETED : PlanStatus.FAILED;
        this.completedAt = System.currentTimeMillis();
        if (startedAt != null) {
            this.totalDurationMs = completedAt - startedAt;
        }
    }
}

