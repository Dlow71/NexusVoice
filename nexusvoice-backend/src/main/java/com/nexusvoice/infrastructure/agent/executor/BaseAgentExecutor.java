package com.nexusvoice.infrastructure.agent.executor;

import com.nexusvoice.domain.agent.enums.AgentState;
import com.nexusvoice.domain.agent.model.*;
import com.nexusvoice.infrastructure.agent.registry.ToolRegistryImpl;
import com.nexusvoice.infrastructure.agent.tool.BaseTool;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Agent执行器基类（模板方法模式）
 * 
 * 职责：
 * - 定义Agent执行的标准流程
 * - 管理执行上下文和状态
 * - 提供工具调用能力
 * 
 * 设计模式：
 * - 模板方法：execute()定义固定流程，子类实现executeStep()
 * - 策略模式：不同子类实现不同执行策略
 */
@Slf4j
public abstract class BaseAgentExecutor {
    
    @Autowired
    protected AiChatService aiChatService;
    
    @Autowired
    protected ToolRegistryImpl toolRegistry;
    
    /**
     * 模板方法：统一执行流程（final，不可重写）
     * 
     * @param agent Agent实体
     * @param query 用户查询
     * @param context 执行上下文
     * @return 执行结果
     */
    public final String execute(Agent agent, String query, AgentContext context) {
        log.info("=== Agent开始执行 ===");
        log.info("Agent: {}, 查询: {}", agent.getName(), query);
        
        try {
            // 1. 初始化上下文
            initializeContext(agent, query, context);
            
            // 2. 执行循环
            String result = null;
            while (!context.hasReachedMaxSteps() && context.getState() == AgentState.RUNNING) {
                context.incrementStep();
                log.info("--- 步骤 {} ---", context.getCurrentStep());
                
                try {
                    // 调用子类实现的单步执行
                    result = executeStep(agent, context);
                    
                    // 检查是否完成
                    if (isFinished(result, context)) {
                        context.markFinished();
                        log.info("Agent执行完成，总步数：{}", context.getCurrentStep());
                        return result;
                    }
                    
                } catch (Exception e) {
                    log.error("步骤 {} 执行失败", context.getCurrentStep(), e);
                    handleStepError(e, context);
                }
            }
            
            // 3. 超过最大步数
            if (context.hasReachedMaxSteps()) {
                log.warn("Agent执行超时，已达到最大步数：{}", context.getMaxSteps());
                context.markFailed();
                return "抱歉，任务执行超时。请简化问题或分步提问。";
            }
            
            // 4. 返回结果
            return result != null ? result : "执行完成";
            
        } catch (Exception e) {
            log.error("Agent执行异常", e);
            context.markFailed();
            throw BizException.of(ErrorCodeEnum.SYSTEM_ERROR, "Agent执行失败：" + e.getMessage());
            
        } finally {
            log.info("=== Agent执行结束 ===");
            log.info("总步数：{}，总耗时：{}ms", 
                context.getCurrentStep(), context.getTotalTimeMs());
        }
    }
    
    /**
     * 抽象方法：单步执行逻辑（子类必须实现）
     * 
     * @param agent Agent实体
     * @param context 执行上下文
     * @return 步骤结果（如果完成则返回最终答案，否则返回null继续）
     */
    protected abstract String executeStep(Agent agent, AgentContext context);
    
    /**
     * 抽象方法：判断是否完成（子类实现）
     * 
     * @param result 当前步骤结果
     * @param context 执行上下文
     * @return 是否完成
     */
    protected abstract boolean isFinished(String result, AgentContext context);
    
    /**
     * 初始化上下文
     */
    protected void initializeContext(Agent agent, String query, AgentContext context) {
        context.markStart();
        
        // 安全获取最大步数（防止NPE）
        Integer maxSteps = 10;  // 默认值
        if (agent.getConfig() != null && agent.getConfig().getMaxSteps() != null) {
            maxSteps = agent.getConfig().getMaxSteps();
        }
        context.setMaxSteps(maxSteps);
        context.addMessage(AgentMessage.user(query));
        
        log.debug("上下文初始化完成，最大步数：{}", context.getMaxSteps());
    }
    
    /**
     * 执行单个工具
     * 
     * @param toolName 工具名称
     * @param params 工具参数
     * @param context 执行上下文
     * @return 工具执行结果
     */
    protected String executeTool(String toolName, Map<String, Object> params, AgentContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 参数验证
            if (toolName == null || toolName.trim().isEmpty()) {
                String errorMsg = "工具名称为空";
                log.error(errorMsg);
                return errorMsg;
            }
            
            if (params == null) {
                params = new java.util.HashMap<>();
            }
            
            // 获取工具
            BaseTool tool = toolRegistry.getBaseTool(toolName);
            if (tool == null) {
                String errorMsg = "工具不存在: " + toolName;
                log.error(errorMsg);
                return errorMsg;
            }
            
            log.info("执行工具：{}", toolName);
            log.debug("工具参数：{}", params);
            
            // 执行工具
            String result = tool.execute(params);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("工具执行完成，耗时：{}ms", duration);
            
            // 记录执行历史
            AgentStepRecord record = AgentStepRecord.act(
                context.getCurrentStep(),
                toolName,
                params.toString(),
                result
            );
            record.setDurationMs(duration);
            context.addExecutionRecord(record);
            
            return result;
            
        } catch (BizException e) {
            log.error("工具执行失败：{}", toolName, e);
            return String.format("工具执行失败：%s - %s", toolName, e.getMessage());
            
        } catch (Exception e) {
            log.error("工具执行异常：{}", toolName, e);
            return String.format("工具执行异常：%s - %s", toolName, e.getMessage());
        }
    }
    
    /**
     * 处理步骤执行错误
     */
    protected void handleStepError(Exception e, AgentContext context) {
        String errorMsg = e.getMessage();
        log.error("步骤执行出错：{}", errorMsg);
        
        // 添加错误消息到记忆
        context.addMessage(AgentMessage.system("执行出错：" + errorMsg));
        
        // 如果是致命错误，标记为失败
        if (e instanceof BizException) {
            // BizException都视为可恢复错误，继续执行
            // 如果连续3步失败，由外层超时机制处理
        }
    }
    
    /**
     * 构建工具描述（给LLM看的）
     */
    protected String buildToolDescriptions(Agent agent) {
        if (agent.getAvailableTools() == null || agent.getAvailableTools().isEmpty()) {
            return "无可用工具";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("可用工具列表：\n\n");
        
        for (String toolName : agent.getAvailableTools()) {
            BaseTool tool = toolRegistry.getBaseTool(toolName);
            if (tool == null) continue;
            
            sb.append(String.format("工具名称：%s\n", tool.getName()));
            sb.append(String.format("功能描述：%s\n", tool.getDescription()));
            sb.append("参数：\n");
            
            for (ToolParameter param : tool.getParameters()) {
                sb.append(String.format("  - %s (%s)%s: %s\n",
                    param.getName(),
                    param.getType(),
                    param.getRequired() ? " [必需]" : "",
                    param.getDescription()
                ));
                if (param.getExample() != null) {
                    sb.append(String.format("    示例: %s\n", param.getExample()));
                }
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 获取记忆摘要（最近N条消息）
     */
    protected String getMemorySummary(AgentContext context, int lastN) {
        if (context.getMemory() == null || context.getMemory().isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        int size = context.getMemory().size();
        int start = Math.max(0, size - lastN);
        
        for (int i = start; i < size; i++) {
            AgentMessage msg = context.getMemory().get(i);
            sb.append(String.format("%s: %s\n", msg.getRole(), msg.getContent()));
        }
        
        return sb.toString();
    }
}

