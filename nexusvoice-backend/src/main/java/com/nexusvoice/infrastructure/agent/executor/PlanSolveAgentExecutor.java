package com.nexusvoice.infrastructure.agent.executor;

import com.nexusvoice.domain.agent.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * Plan+Solve模式Agent执行器
 * 
 * 执行流程：
 * 1. Planning阶段：拆解任务，生成计划
 * 2. Execute阶段：按依赖关系执行任务（支持并行）
 * 3. Summary阶段：汇总结果，生成最终答案
 * 
 * 适用场景：
 * - 复杂、多步骤的任务
 * - 需要任务规划和编排
 * - 可并行执行的任务
 */
@Slf4j
@Component
public class PlanSolveAgentExecutor extends BaseAgentExecutor {
    
    @Autowired
    private PlanningAgent planningAgent;
    
    @Autowired
    private ExecutorAgent executorAgent;
    
    @Autowired
    private SummaryAgent summaryAgent;
    
    /**
     * 线程池（用于并行执行任务）
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    @Override
    protected String executeStep(Agent agent, AgentContext context) {
        int currentStep = context.getCurrentStep();
        
        try {
            // 步骤1：规划阶段
            if (currentStep == 1) {
                return planningPhase(agent, context);
            }
            
            // 步骤2-N：执行阶段
            PlanExecution plan = context.getVariable("plan", PlanExecution.class);
            if (plan == null) {
                log.error("未找到执行计划，可能Planning阶段失败");
                context.setVariable("finished", true);
                return "执行失败：未生成有效计划";
            }
            
            if (!plan.isCompleted()) {
                return executionPhase(plan, context);
            }
            
            // 最后：汇总阶段
            return summaryPhase(plan, context);
            
        } catch (Exception e) {
            log.error("Plan+Solve执行失败", e);
            return "执行失败：" + e.getMessage();
        }
    }
    
    /**
     * 规划阶段
     */
    private String planningPhase(Agent agent, AgentContext context) {
        log.info("=== 规划阶段 ===");
        
        // 获取用户查询
        AgentMessage lastUserMsg = context.getLastUserMessage();
        if (lastUserMsg == null) {
            throw new RuntimeException("未找到用户查询");
        }
        
        String query = lastUserMsg.getContent();
        
        // 创建计划
        PlanExecution plan = planningAgent.createPlan(query, agent, context);
        
        // 标记计划开始
        plan.markStarted();
        
        // 保存到上下文
        context.setVariable("plan", plan);
        
        // 记录步骤
        AgentStepRecord record = AgentStepRecord.builder()
            .stepNumber(1)
            .stepType("planning")
            .description("任务规划完成")
            .output(String.format("生成%d个任务", plan.getTasks().size()))
            .success(true)
            .timestamp(System.currentTimeMillis())
            .build();
        context.addExecutionRecord(record);
        
        log.info("规划完成，共{}个任务", plan.getTasks().size());
        
        return null;  // 继续下一步
    }
    
    /**
     * 执行阶段
     */
    private String executionPhase(PlanExecution plan, AgentContext context) {
        log.info("=== 执行阶段（步骤{}）===", context.getCurrentStep());
        
        // 获取可执行的任务（依赖已满足）
        List<AgentTask> executableTasks = plan.getExecutableTasks();
        
        if (executableTasks.isEmpty()) {
            // 没有可执行任务，检查是否有pending任务（可能存在循环依赖或配置错误）
            List<AgentTask> pendingTasks = plan.getPendingTasks();
            if (!pendingTasks.isEmpty()) {
                log.error("存在待执行任务但无法执行，可能存在循环依赖");
                // 标记这些任务为失败
                for (AgentTask task : pendingTasks) {
                    task.markAsFailed("无法执行：依赖关系错误");
                }
            }
            return null;  // 继续到汇总阶段
        }
        
        // 分组：可并行和必须串行
        List<AgentTask> parallelTasks = executableTasks.stream()
            .filter(AgentTask::getCanParallel)
            .toList();
        
        List<AgentTask> serialTasks = executableTasks.stream()
            .filter(task -> !task.getCanParallel())
            .toList();
        
        // 执行串行任务
        for (AgentTask task : serialTasks) {
            executeTaskAndRecord(task, context);
        }
        
        // 并行执行可并行任务
        if (!parallelTasks.isEmpty()) {
            executeTasksInParallel(parallelTasks, context);
        }
        
        // 记录步骤
        int completedCount = plan.getCompletedTasks().size();
        int totalCount = plan.getTasks().size();
        
        AgentStepRecord record = AgentStepRecord.builder()
            .stepNumber(context.getCurrentStep())
            .stepType("execution")
            .description(String.format("执行任务（%d/%d）", completedCount, totalCount))
            .output(String.format("进度：%.1f%%", plan.getProgress() * 100))
            .success(true)
            .timestamp(System.currentTimeMillis())
            .build();
        context.addExecutionRecord(record);
        
        log.info("执行进度：{}/{}", completedCount, totalCount);
        
        return null;  // 继续下一步
    }
    
    /**
     * 执行单个任务并记录
     */
    private void executeTaskAndRecord(AgentTask task, AgentContext context) {
        log.info("执行任务：{}", task.getTaskId());
        
        long startTime = System.currentTimeMillis();
        executorAgent.executeTask(task);
        long duration = System.currentTimeMillis() - startTime;
        
        // 记录到上下文
        context.addMessage(AgentMessage.assistant(
            String.format("任务%s完成：%s", task.getTaskId(), task.getDescription())
        ));
        
        log.info("任务{}完成，耗时{}ms", task.getTaskId(), duration);
    }
    
    /**
     * 并行执行多个任务
     */
    private void executeTasksInParallel(List<AgentTask> tasks, AgentContext context) {
        log.info("开始并行执行{}个任务", tasks.size());
        
        CountDownLatch latch = new CountDownLatch(tasks.size());
        
        for (AgentTask task : tasks) {
            executorService.submit(() -> {
                try {
                    executeTaskAndRecord(task, context);
                } catch (Exception e) {
                    log.error("并行任务执行失败：{}", task.getTaskId(), e);
                    task.markAsFailed("执行异常：" + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有任务完成（最多等待2分钟）
        try {
            if (!latch.await(120, TimeUnit.SECONDS)) {
                log.warn("并行任务执行超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("并行任务等待被中断", e);
        }
        
        log.info("并行任务执行完成");
    }
    
    /**
     * 汇总阶段
     */
    private String summaryPhase(PlanExecution plan, AgentContext context) {
        log.info("=== 汇总阶段 ===");
        
        // 标记计划完成
        plan.markCompleted();
        
        // 生成最终答案
        String finalAnswer = summaryAgent.summarizePlan(plan);
        
        // 记录步骤
        AgentStepRecord record = AgentStepRecord.builder()
            .stepNumber(context.getCurrentStep())
            .stepType("summary")
            .description("结果汇总完成")
            .output(finalAnswer)
            .success(true)
            .timestamp(System.currentTimeMillis())
            .build();
        context.addExecutionRecord(record);
        
        // 添加到记忆
        context.addMessage(AgentMessage.assistant(finalAnswer));
        
        // 标记完成
        context.setVariable("finished", true);
        
        log.info("Plan+Solve执行完成");
        
        return finalAnswer;
    }
    
    @Override
    protected boolean isFinished(String result, AgentContext context) {
        // 检查是否已标记完成
        if (Boolean.TRUE.equals(context.getVariable("finished", Boolean.class))) {
            return true;
        }
        
        // 检查计划是否完成
        PlanExecution plan = context.getVariable("plan", PlanExecution.class);
        if (plan != null && plan.isCompleted()) {
            // 如果计划完成但还没汇总，继续执行汇总步骤
            return context.getExecutionHistory().stream()
                .anyMatch(r -> "summary".equals(r.getStepType()));
        }
        
        return false;
    }
    
    /**
     * 清理资源
     */
    public void shutdown() {
        log.info("关闭PlanSolve执行器线程池");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

