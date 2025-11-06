package com.nexusvoice.infrastructure.agent.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nexusvoice.domain.agent.model.Agent;
import com.nexusvoice.domain.agent.model.AgentContext;
import com.nexusvoice.domain.agent.model.AgentMessage;
import com.nexusvoice.domain.agent.model.AgentStepRecord;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * ReAct模式Agent执行器
 * 
 * 执行流程：
 * 1. Think：LLM思考下一步动作
 * 2. Act：执行动作（工具调用或直接回答）
 * 3. Observe：观察结果，判断是否继续
 * 
 * 适用场景：
 * - 简单、直接的任务
 * - 单步或少量步骤可完成
 * - 不需要复杂规划
 */
@Slf4j
@Component
public class ReactAgentExecutor extends BaseAgentExecutor {
    
    @Override
    protected String executeStep(Agent agent, AgentContext context) {
        long stepStartTime = System.currentTimeMillis();
        
        try {
            // 1. Think：LLM决策下一步
            String decision = think(agent, context);
            
            // 记录Think步骤
            AgentStepRecord thinkRecord = AgentStepRecord.think(
                context.getCurrentStep(),
                getMemorySummary(context, 5),
                decision
            );
            context.addExecutionRecord(thinkRecord);
            
            // 2. Act：执行动作
            String actionResult = act(decision, context);
            
            // 3. Observe：观察并返回结果
            String result = observe(actionResult, context);
            
            long stepDuration = System.currentTimeMillis() - stepStartTime;
            log.info("步骤执行完成，耗时：{}ms", stepDuration);
            
            return result;
            
        } catch (Exception e) {
            log.error("ReAct步骤执行失败", e);
            return null;  // 继续下一步
        }
    }
    
    /**
     * Think阶段：LLM思考决策
     */
    private String think(Agent agent, AgentContext context) {
        log.debug("Think阶段：分析当前状态，决定下一步动作");
        
        // 构造Prompt
        String prompt = buildThinkPrompt(agent, context);
        
        // 调用LLM
        // 安全获取配置（防止NPE）
        Double temperature = agent.getConfig() != null ? agent.getConfig().getTemperature() : 0.7;
        Integer maxTokens = agent.getConfig() != null ? agent.getConfig().getMaxTokens() : 2000;
        String modelName = agent.getConfig() != null && agent.getConfig().getModelName() != null 
            ? agent.getConfig().getModelName() 
            : "deepseek:deepseek-v3.1";  // 默认使用DeepSeek V3.1
        
        ChatRequest request = ChatRequest.builder()
            .messages(convertToMessages(prompt, agent.getSystemPrompt()))
            .model(modelName)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .build();
        
        ChatResponse response = aiChatService.chat(request);
        
        if (!response.getSuccess()) {
            log.error("LLM调用失败：{}", response.getErrorMessage());
            return "FINAL_ANSWER: 抱歉，AI服务暂时不可用";
        }
        
        String decision = response.getContent();
        log.debug("LLM决策：{}", decision);
        
        // 保存到记忆
        context.addMessage(AgentMessage.assistant(decision));
        
        return decision;
    }
    
    /**
     * Act阶段：执行动作
     */
    private String act(String decision, AgentContext context) {
        log.debug("Act阶段：执行决策中的动作");
        
        // 解析决策，提取动作
        Action action = parseAction(decision);
        
        if (action.isFinalAnswer()) {
            // 直接返回最终答案
            log.info("Agent返回最终答案");
            return action.content;
        }
        
        if (action.toolCall != null && action.toolCall.name != null) {
            // 执行工具调用
            log.info("执行工具：{}", action.toolCall.name);
            
            // 确保params不为null
            Map<String, Object> params = action.toolCall.params != null 
                ? action.toolCall.params 
                : new java.util.HashMap<>();
            
            String toolResult = executeTool(
                action.toolCall.name, 
                params, 
                context
            );
            
            // 保存工具结果到记忆
            context.addMessage(AgentMessage.tool(toolResult));
            
            return toolResult;
        }
        
        // 没有明确动作，返回决策内容
        return decision;
    }
    
    /**
     * Observe阶段：观察结果并判断
     */
    private String observe(String actionResult, AgentContext context) {
        log.debug("Observe阶段：观察结果，判断是否继续");
        
        // 记录Observe步骤
        AgentStepRecord observeRecord = AgentStepRecord.observe(
            context.getCurrentStep(),
            actionResult,
            "继续执行"
        );
        context.addExecutionRecord(observeRecord);
        
        // 检查是否包含最终答案标记
        if (actionResult.contains("FINAL_ANSWER:") || 
            actionResult.contains("Final Answer:") ||
            actionResult.contains("最终答案：")) {
            // 提取最终答案
            context.setVariable("finished", true);
            return extractFinalAnswer(actionResult);
        }
        
        // 继续下一步
        return null;
    }
    
    @Override
    protected boolean isFinished(String result, AgentContext context) {
        return Boolean.TRUE.equals(context.getVariable("finished", Boolean.class));
    }
    
    /**
     * 构造Think阶段的Prompt
     */
    private String buildThinkPrompt(Agent agent, AgentContext context) {
        StringBuilder prompt = new StringBuilder();
        
        // 系统提示词
        if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isEmpty()) {
            prompt.append("系统指令：\n").append(agent.getSystemPrompt()).append("\n\n");
        }
        
        // 可用工具
        if (agent.getAvailableTools() != null && !agent.getAvailableTools().isEmpty()) {
            try {
                String toolDesc = buildToolDescriptions(agent);
                if (toolDesc != null && !toolDesc.isEmpty()) {
                    prompt.append(toolDesc).append("\n");
                }
            } catch (Exception e) {
                log.warn("构建工具描述失败", e);
            }
        }
        
        // 对话历史
        prompt.append("对话历史：\n");
        String memorySummary = getMemorySummary(context, 10);
        if (memorySummary != null && !memorySummary.isEmpty()) {
            prompt.append(memorySummary).append("\n");
        }
        
        // 下一步引导
        String nextStepPrompt = agent.getNextStepPrompt();
        if (nextStepPrompt == null || nextStepPrompt.isEmpty()) {
            nextStepPrompt = getDefaultNextStepPrompt(agent);
        }
        prompt.append(nextStepPrompt);
        
        return prompt.toString();
    }
    
    /**
     * 默认的下一步引导提示词（优化版）
     */
    private String getDefaultNextStepPrompt(Agent agent) {
        if (agent.getAvailableTools() != null && !agent.getAvailableTools().isEmpty()) {
            return """
                
                【ReAct推理循环 - 当前步骤】
                请分析当前状态并决策：
                
                🤔 思考（Think）：
                - 用户的核心需求是什么？
                - 我已经知道什么信息？
                - 还缺少什么关键信息？
                
                🛠️ 行动（Act）：
                选项1 - 使用工具：
                  格式：TOOL_CALL: {"name": "工具名", "params": {"参数名": "参数值"}}
                  何时使用：需要外部信息或实时数据
                
                选项2 - 给出答案：
                  格式：FINAL_ANSWER: [完整答案]
                  何时使用：已有足够信息可准确回答
                
                📊 决定：
                （请直接输出TOOL_CALL或FINAL_ANSWER，不需要额外解释）
                """;
        } else {
            return """
                
                【生成回答】
                请基于以上对话历史，给出完整、准确的回答。
                
                输出格式：
                FINAL_ANSWER: [你的回答内容]
                
                要求：
                - 回答要完整、准确
                - 结构清晰、条理分明
                - 使用中文
                """;
        }
    }
    
    /**
     * 转换为ChatMessage列表
     */
    private List<ChatMessage> convertToMessages(String userPrompt, String systemPrompt) {
        List<ChatMessage> messages = new ArrayList<>();
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(ChatMessage.system(systemPrompt));
        }
        
        messages.add(ChatMessage.user(userPrompt));
        
        return messages;
    }
    
    /**
     * 解析LLM决策中的动作
     */
    private Action parseAction(String decision) {
        Action action = new Action();
        
        // 检查是否是最终答案
        if (decision.contains("FINAL_ANSWER:") || decision.contains("Final Answer:")) {
            action.isFinalAnswer = true;
            action.content = extractFinalAnswer(decision);
            return action;
        }
        
        // 检查是否是工具调用
        if (decision.contains("TOOL_CALL:")) {
            try {
                // 提取JSON部分
                int startIdx = decision.indexOf("{");
                int endIdx = decision.lastIndexOf("}") + 1;
                
                if (startIdx != -1 && endIdx > startIdx) {
                    String jsonStr = decision.substring(startIdx, endIdx);
                    Map<String, Object> toolCallMap = JsonUtils.fromJson(
                        jsonStr, 
                        new TypeReference<Map<String, Object>>() {}
                    );
                    
                    ToolCallInfo info = new ToolCallInfo();
                    info.name = (String) toolCallMap.get("name");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = (Map<String, Object>) toolCallMap.get("params");
                    info.params = params;
                    
                    action.toolCall = info;
                    log.debug("解析到工具调用：{}", info.name);
                }
            } catch (Exception e) {
                log.error("解析工具调用失败", e);
            }
        }
        
        // 默认：继续思考
        action.content = decision;
        return action;
    }
    
    /**
     * 提取最终答案
     */
    private String extractFinalAnswer(String text) {
        String[] markers = {"FINAL_ANSWER:", "Final Answer:", "最终答案："};
        
        for (String marker : markers) {
            if (text.contains(marker)) {
                int idx = text.indexOf(marker) + marker.length();
                return text.substring(idx).trim();
            }
        }
        
        return text;
    }
    
    /**
     * 动作结构
     */
    private static class Action {
        boolean isFinalAnswer = false;
        String content;
        ToolCallInfo toolCall;
        
        boolean isFinalAnswer() {
            return isFinalAnswer;
        }
    }
    
    /**
     * 工具调用信息
     */
    private static class ToolCallInfo {
        String name;
        Map<String, Object> params;
    }
}

