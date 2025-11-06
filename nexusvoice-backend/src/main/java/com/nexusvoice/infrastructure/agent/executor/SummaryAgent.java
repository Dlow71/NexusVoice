package com.nexusvoice.infrastructure.agent.executor;

import com.nexusvoice.domain.agent.model.AgentTask;
import com.nexusvoice.domain.agent.model.PlanExecution;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 总结Agent
 * 
 * 职责：
 * - 接收已完成的Plan
 * - 汇总所有任务结果
 * - 生成最终答案
 * - 提供结构化摘要
 * 
 * 设计：
 * - 专注结果聚合和总结
 * - 生成对用户友好的回答
 */
@Slf4j
@Component
public class SummaryAgent {
    
    @Autowired
    private AiChatService aiChatService;
    
    /**
     * 汇总计划执行结果
     * 
     * @param plan 已完成的计划
     * @return 最终答案
     */
    public String summarizePlan(PlanExecution plan) {
        log.info("开始汇总计划执行结果，计划ID：{}", plan.getPlanId());
        
        try {
            // 1. 收集所有任务结果
            String tasksSummary = buildTasksSummary(plan);
            
            // 2. 调用LLM生成最终答案
            String finalAnswer = generateFinalAnswer(plan, tasksSummary);
            
            // 3. 更新计划
            plan.setResultSummary(finalAnswer);
            
            log.info("计划汇总完成");
            
            return finalAnswer;
            
        } catch (Exception e) {
            log.error("计划汇总失败", e);
            return buildFallbackSummary(plan);
        }
    }
    
    /**
     * 构建任务结果摘要
     */
    private String buildTasksSummary(PlanExecution plan) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("【执行计划】\n");
        summary.append(plan.getDescription()).append("\n\n");
        
        summary.append("【原始查询】\n");
        summary.append(plan.getOriginalQuery()).append("\n\n");
        
        summary.append("【任务执行结果】\n");
        
        int taskNum = 1;
        for (AgentTask task : plan.getTasks()) {
            summary.append(String.format("%d. %s\n", taskNum++, task.getDescription()));
            summary.append(String.format("   状态：%s\n", getStatusText(task.getStatus())));
            
            if (task.getResult() != null && !task.getResult().isEmpty()) {
                // 限制结果长度
                String result = task.getResult();
                if (result.length() > 500) {
                    result = result.substring(0, 500) + "...";
                }
                summary.append(String.format("   结果：%s\n", result));
            }
            
            if (task.getStatus() == com.nexusvoice.domain.agent.enums.TaskStatus.FAILED && 
                task.getErrorMessage() != null) {
                summary.append(String.format("   错误：%s\n", task.getErrorMessage()));
            }
            
            summary.append("\n");
        }
        
        summary.append(String.format("【执行统计】\n"));
        summary.append(String.format("总任务数：%d\n", plan.getTasks().size()));
        summary.append(String.format("成功：%d\n", plan.getCompletedTasks().size()));
        summary.append(String.format("失败：%d\n", plan.getFailedTasks().size()));
        summary.append(String.format("总耗时：%dms\n", plan.getTotalDurationMs()));
        
        return summary.toString();
    }
    
    /**
     * 使用LLM生成最终答案
     */
    private String generateFinalAnswer(PlanExecution plan, String tasksSummary) {
        String summaryPrompt = buildSummaryPrompt(plan, tasksSummary);
        
        // 默认使用DeepSeek V3.1（后续可从配置获取）
        String modelName = "deepseek:deepseek-v3.1";
        
        ChatRequest request = ChatRequest.builder()
            .messages(List.of(
                ChatMessage.system("""
                    你是一个结果汇总专家，擅长将多个任务的执行结果整合为清晰、有价值的最终答案。
                    
                    【核心能力】
                    - 信息综合：整合多个来源的信息
                    - 逻辑推理：从结果中提炼关键结论
                    - 结构化表达：清晰、有条理地组织答案
                    
                    【汇总要求】
                    1. 直接回答用户的原始问题
                    2. 综合所有成功任务的结果
                    3. 提炼关键信息和结论
                    4. 结构清晰，使用适当的标题和段落
                    5. 如有失败任务，简要说明但不影响主要答案
                    6. 使用中文，表达流畅自然
                    """),
                ChatMessage.user(summaryPrompt)
            ))
            .model(modelName)
            .temperature(0.5)
            .maxTokens(1500)
            .build();
        
        ChatResponse response = aiChatService.chat(request);
        
        if (!response.getSuccess()) {
            log.error("LLM汇总失败：{}", response.getErrorMessage());
            return buildFallbackSummary(plan);
        }
        
        return response.getContent();
    }
    
    /**
     * 构建汇总提示词（优化版）
     */
    private String buildSummaryPrompt(PlanExecution plan, String tasksSummary) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("【任务汇总请求】\n");
        prompt.append("请基于以下任务执行结果，生成完整、有价值的最终答案。\n\n");
        
        prompt.append(tasksSummary).append("\n\n");
        
        prompt.append("【汇总要求】\n");
        prompt.append("🎯 核心目标：直接回答用户的原始问题\n\n");
        
        prompt.append("📊 内容要求：\n");
        prompt.append("  • 综合所有成功任务的结果\n");
        prompt.append("  • 提炼关键信息和结论\n");
        prompt.append("  • 保持逻辑连贯性\n");
        prompt.append("  • 如有失败任务，简要说明（不影响主要答案）\n\n");
        
        prompt.append("✨ 结构要求：\n");
        prompt.append("  • 开头直接回答核心问题\n");
        prompt.append("  • 使用适当的标题和段落\n");
        prompt.append("  • 重点突出、条理清晰\n");
        prompt.append("  • 不要重复任务编号\n");
        prompt.append("  • 使用中文，表达自然流畅\n\n");
        
        prompt.append("【请生成最终答案】\n");
        
        return prompt.toString();
    }
    
    /**
     * 构建降级摘要（当LLM失败时）
     */
    private String buildFallbackSummary(PlanExecution plan) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("根据您的查询\"").append(plan.getOriginalQuery()).append("\"，");
        summary.append("我已完成").append(plan.getCompletedTasks().size()).append("个任务");
        
        if (plan.getFailedTasks().size() > 0) {
            summary.append("（").append(plan.getFailedTasks().size()).append("个失败）");
        }
        
        summary.append("。\n\n");
        
        // 列出成功任务的结果
        for (AgentTask task : plan.getCompletedTasks()) {
            if (task.getResult() != null && !task.getResult().isEmpty()) {
                summary.append("• ").append(task.getDescription()).append("\n");
                String result = task.getResult();
                if (result.length() > 200) {
                    result = result.substring(0, 200) + "...";
                }
                summary.append("  ").append(result).append("\n\n");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * 获取状态文本
     */
    private String getStatusText(com.nexusvoice.domain.agent.enums.TaskStatus status) {
        if (status == com.nexusvoice.domain.agent.enums.TaskStatus.COMPLETED) {
            return "✅ 已完成";
        } else if (status == com.nexusvoice.domain.agent.enums.TaskStatus.FAILED) {
            return "❌ 失败";
        } else if (status == com.nexusvoice.domain.agent.enums.TaskStatus.RUNNING) {
            return "⏳ 执行中";
        } else if (status == com.nexusvoice.domain.agent.enums.TaskStatus.SKIPPED) {
            return "⏭️ 已跳过";
        }
        return "⏸️ 待执行";
    }
}

