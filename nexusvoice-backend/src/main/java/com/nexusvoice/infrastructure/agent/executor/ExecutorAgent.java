package com.nexusvoice.infrastructure.agent.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nexusvoice.domain.agent.model.AgentTask;
import com.nexusvoice.infrastructure.agent.registry.ToolRegistryImpl;
import com.nexusvoice.infrastructure.agent.tool.BaseTool;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 执行Agent
 * 
 * 职责：
 * - 接收Plan中的单个Task
 * - 执行任务（调用工具）
 * - 处理执行结果
 * - 支持错误重试
 * 
 * 设计：
 * - 专注单一任务执行
 * - 不处理依赖关系（由上层管理）
 * - 支持重试机制
 */
@Slf4j
@Component
public class ExecutorAgent {
    
    @Autowired
    private AiChatService aiChatService;
    
    @Autowired
    private ToolRegistryImpl toolRegistry;
    
    /**
     * 执行单个任务
     * 
     * @param task 要执行的任务
     * @return 执行结果
     */
    public String executeTask(AgentTask task) {
        log.info("开始执行任务：{} - {}", task.getTaskId(), task.getDescription());
        
        try {
            // 标记任务开始
            task.markAsRunning();
            
            // 如果任务已指定工具和参数，直接执行
            if (task.getToolName() != null && !task.getToolName().isEmpty()) {
                return executeWithSpecifiedTool(task);
            }
            
            // 否则，由LLM决定如何执行
            return executeWithLLMGuidance(task);
            
        } catch (Exception e) {
            log.error("任务执行失败：{}", task.getTaskId(), e);
            task.markAsFailed(e.getMessage());
            return "执行失败：" + e.getMessage();
        }
    }
    
    /**
     * 使用指定的工具执行任务
     */
    private String executeWithSpecifiedTool(AgentTask task) {
        String toolName = task.getToolName();
        
        // 获取工具
        BaseTool tool = toolRegistry.getBaseTool(toolName);
        if (tool == null) {
            String errorMsg = "工具不存在：" + toolName;
            task.markAsFailed(errorMsg);
            return errorMsg;
        }
        
        // 解析参数
        Map<String, Object> params;
        try {
            if (task.getToolParams() != null && !task.getToolParams().isEmpty()) {
                params = JsonUtils.fromJson(task.getToolParams(), new TypeReference<Map<String, Object>>() {});
            } else {
                params = Map.of();
            }
        } catch (Exception e) {
            String errorMsg = "参数解析失败：" + e.getMessage();
            task.markAsFailed(errorMsg);
            return errorMsg;
        }
        
        // 执行工具
        try {
            log.info("执行工具：{}，参数：{}", toolName, params);
            String result = tool.execute(params);
            
            // 标记完成
            task.markAsCompleted(result);
            log.info("任务执行成功：{}", task.getTaskId());
            
            return result;
            
        } catch (Exception e) {
            log.error("工具执行失败：{}", toolName, e);
            
            // 标记失败，不重试（避免递归死循环）
            task.markAsFailed("工具执行失败：" + e.getMessage());
            log.error("任务执行失败，已达最大重试次数：{}", task.getTaskId());
            return "执行失败：" + e.getMessage();
        }
    }
    
    /**
     * 由LLM指导执行任务
     */
    private String executeWithLLMGuidance(AgentTask task) {
        // 让LLM分析任务，决定使用什么工具
        String analysisPrompt = buildTaskAnalysisPrompt(task);
        
        // 默认使用DeepSeek V3.1（后续可从配置获取）
        String modelName = "deepseek:deepseek-v3.1";
        
        ChatRequest request = ChatRequest.builder()
            .messages(List.of(
                ChatMessage.system("""
                    你是一个任务执行助手，负责分析单个任务并选择最合适的工具执行。
                    
                    【工作流程】
                    1. 理解任务目标
                    2. 分析所需信息或操作
                    3. 选择最合适的工具
                    4. 确定工具参数
                    
                    【输出要求】
                    - 仅返回JSON格式
                    - 包含tool和params字段
                    - 参数要准确、完整
                    """),
                ChatMessage.user(analysisPrompt)
            ))
            .model(modelName)
            .temperature(0.3)
            .maxTokens(500)
            .build();
        
        ChatResponse response = aiChatService.chat(request);
        
        if (!response.getSuccess()) {
            task.markAsFailed("LLM分析失败：" + response.getErrorMessage());
            return "分析失败：" + response.getErrorMessage();
        }
        
        // 解析LLM返回的工具和参数
        try {
            String content = response.getContent();
            String json = extractJson(content);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> decision = JsonUtils.fromJson(json, Map.class);
            
            String toolName = (String) decision.get("tool");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) decision.get("params");
            
            // 更新任务信息
            task.setToolName(toolName);
            task.setToolParams(JsonUtils.toJson(params));
            
            // 执行
            return executeWithSpecifiedTool(task);
            
        } catch (Exception e) {
            log.error("解析LLM决策失败", e);
            task.markAsFailed("决策解析失败：" + e.getMessage());
            return "解析失败：" + e.getMessage();
        }
    }
    
    /**
     * 构建任务分析提示词
     */
    private String buildTaskAnalysisPrompt(AgentTask task) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请分析以下任务，并选择合适的工具执行。\n\n");
        
        prompt.append("【任务描述】\n");
        prompt.append(task.getDescription()).append("\n\n");
        
        prompt.append("【可用工具】\n");
        prompt.append(buildAvailableToolsDescription()).append("\n");
        
        prompt.append("【输出要求】\n");
        prompt.append("请以JSON格式返回：\n");
        prompt.append("{\n");
        prompt.append("  \"tool\": \"工具名称\",\n");
        prompt.append("  \"params\": {\n");
        prompt.append("    \"参数名\": \"参数值\"\n");
        prompt.append("  }\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    /**
     * 构建可用工具描述
     */
    private String buildAvailableToolsDescription() {
        StringBuilder sb = new StringBuilder();
        
        for (com.nexusvoice.domain.agent.model.Tool tool : toolRegistry.getEnabledTools()) {
            sb.append(String.format("- %s: %s\n", tool.getName(), tool.getDescription()));
        }
        
        return sb.toString();
    }
    
    /**
     * 提取JSON内容
     */
    private String extractJson(String text) {
        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        } else if (text.contains("```")) {
            int start = text.indexOf("```") + 3;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }
        
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end > start) {
            return text.substring(start, end + 1).trim();
        }
        
        return text.trim();
    }
}

