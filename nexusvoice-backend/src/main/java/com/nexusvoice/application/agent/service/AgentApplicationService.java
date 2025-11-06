package com.nexusvoice.application.agent.service;

import com.nexusvoice.application.agent.dto.AgentExecuteRequest;
import com.nexusvoice.application.agent.dto.AgentExecuteResponse;
import com.nexusvoice.domain.agent.enums.AgentType;
import com.nexusvoice.domain.agent.model.Agent;
import com.nexusvoice.domain.agent.model.AgentConfig;
import com.nexusvoice.domain.agent.model.AgentContext;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.agent.executor.BaseAgentExecutor;
import com.nexusvoice.infrastructure.agent.executor.PlanSolveAgentExecutor;
import com.nexusvoice.infrastructure.agent.executor.ReactAgentExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent应用服务
 * 
 * 职责：
 * - 编排Agent执行流程
 * - 选择合适的Agent和执行器
 * - 管理执行上下文
 * - 处理执行结果
 */
@Slf4j
@Service
public class AgentApplicationService {
    
    @Autowired
    private ReactAgentExecutor reactExecutor;
    
    @Autowired
    private PlanSolveAgentExecutor planSolveExecutor;
    
    /**
     * Agent默认模型（可通过配置修改）
     */
    private static final String DEFAULT_AGENT_MODEL = "deepseek:deepseek-v3.1";
    
    /**
     * 执行Agent任务（主入口）
     * 
     * @param request 执行请求
     * @return 执行响应
     */
    public AgentExecuteResponse executeTask(AgentExecuteRequest request) {
        log.info("开始执行Agent任务，查询：{}", request.getQuery());
        
        try {
            // 1. 创建或选择Agent
            Agent agent = selectAgent(request);
            
            // 2. 创建执行上下文
            AgentContext context = createContext(request);
            
            // 3. 选择执行器
            BaseAgentExecutor executor;
            if (request.getAgentType() == null) {
                // 自动选择
                executor = selectExecutorAuto(request.getQuery());
            } else {
                // 使用指定类型
                executor = selectExecutor(agent.getType());
            }
            
            // 4. 执行
            String result = executor.execute(agent, request.getQuery(), context);
            
            // 5. 构建响应
            return buildResponse(agent, context, result, null);
            
        } catch (BizException e) {
            log.error("Agent执行失败", e);
            return AgentExecuteResponse.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
                
        } catch (Exception e) {
            log.error("Agent执行异常", e);
            return AgentExecuteResponse.builder()
                .success(false)
                .errorMessage("执行异常：" + e.getMessage())
                .build();
        }
    }
    
    /**
     * 选择或创建Agent
     */
    private Agent selectAgent(AgentExecuteRequest request) {
        // TODO: 后续从数据库加载已保存的Agent
        // if (request.getAgentName() != null) {
        //     return agentRepository.findByName(request.getAgentName())
        //         .orElseThrow(() -> BizException.of(ErrorCodeEnum.DATA_NOT_FOUND));
        // }
        
        // 暂时创建默认Agent
        return createDefaultAgent(request);
    }
    
    /**
     * 创建默认Agent
     */
    private Agent createDefaultAgent(AgentExecuteRequest request) {
        // 确定Agent类型
        AgentType type = request.getAgentType() != null 
            ? request.getAgentType() 
            : AgentType.REACT;
        
        // 确定可用工具
        List<String> tools = request.getAvailableTools() != null 
            ? request.getAvailableTools()
            : getDefaultTools();
        
        // 创建配置
        AgentConfig config = AgentConfig.builder()
            .maxSteps(request.getMaxSteps() != null ? request.getMaxSteps() : 10)
            .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
            .maxTokens(2000)
            .timeoutMs(60000L)
            .enableConcurrentTools(false)
            .build();
        
        // 创建Agent
        Agent agent = Agent.builder()
            .name("GeneralAgent")
            .description("通用任务处理Agent")
            .type(type)
            .systemPrompt(getDefaultSystemPrompt())
            .nextStepPrompt(getDefaultNextStepPrompt())
            .availableTools(tools)
            .config(config)
            .enabled(true)
            .build();
        
        // 保存模型名称到上下文（用于后续LLM调用）
        agent.getConfig().setModelName(
            request.getModelName() != null ? request.getModelName() : DEFAULT_AGENT_MODEL
        );
        
        return agent;
    }
    
    /**
     * 创建执行上下文
     */
    private AgentContext createContext(AgentExecuteRequest request) {
        String sessionId = request.getSessionId() != null 
            ? request.getSessionId() 
            : UUID.randomUUID().toString();
        
        AgentContext context = AgentContext.builder()
            .sessionId(sessionId)
            .userId(request.getUserId())
            .conversationId(request.getConversationId())
            .currentStep(0)
            .maxSteps(10)
            .variables(new HashMap<>())
            .memory(new ArrayList<>())
            .executionHistory(new ArrayList<>())
            .build();
        
        // 添加上下文变量
        if (request.getContextVariables() != null) {
            context.getVariables().putAll(request.getContextVariables());
        }
        
        return context;
    }
    
    /**
     * 选择执行器
     */
    private BaseAgentExecutor selectExecutor(AgentType type) {
        if (type == AgentType.PLAN_SOLVE) {
            return planSolveExecutor;
        }
        return reactExecutor;  // 默认使用ReAct
    }
    
    /**
     * 智能选择执行器（根据任务复杂度）
     */
    private BaseAgentExecutor selectExecutorAuto(String query) {
        // 启发式判断任务复杂度
        if (isComplexTask(query)) {
            log.info("检测到复杂任务，使用Plan+Solve模式");
            return planSolveExecutor;
        }
        log.info("简单任务，使用ReAct模式");
        return reactExecutor;
    }
    
    /**
     * 判断是否是复杂任务
     */
    private boolean isComplexTask(String query) {
        // 启发式规则：
        // 1. 包含多个步骤关键词
        String[] multiStepKeywords = {"并且", "然后", "接着", "最后", "分析", "比较", "统计", "汇总"};
        int keywordCount = 0;
        for (String keyword : multiStepKeywords) {
            if (query.contains(keyword)) {
                keywordCount++;
            }
        }
        if (keywordCount >= 2) {
            return true;
        }
        
        // 2. 查询长度较长（超过50个字符）
        if (query.length() > 50) {
            return true;
        }
        
        // 3. 包含复杂分析关键词
        String[] complexKeywords = {"报告", "方案", "计划", "对比分析", "深度分析", "多方面"};
        for (String keyword : complexKeywords) {
            if (query.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 构建响应
     */
    private AgentExecuteResponse buildResponse(Agent agent, AgentContext context, 
                                                String result, String errorMessage) {
        // 提取使用的工具列表
        List<String> usedTools = context.getExecutionHistory().stream()
            .filter(record -> "act".equals(record.getStepType()))
            .map(record -> record.getToolName())
            .distinct()
            .collect(Collectors.toList());
        
        return AgentExecuteResponse.builder()
            .success(errorMessage == null)
            .result(result)
            .agentName(agent.getName())
            .steps(context.getCurrentStep())
            .totalTimeMs(context.getTotalTimeMs())
            .usedTools(usedTools)
            .executionHistory(context.getExecutionHistory())
            .errorMessage(errorMessage)
            .build();
    }
    
    /**
     * 默认系统提示词（针对DeepSeek V3.1优化）
     */
    private String getDefaultSystemPrompt() {
        return """
            你是一个智能助手，具备使用工具完成任务的能力。
            
            【核心能力】
            - 深度推理：可以进行多步骤逻辑推理
            - 工具调用：可以调用外部工具获取信息
            - 结果综合：基于工具结果给出准确答案
            
            【工作流程】
            1. 理解用户需求，分析任务目标
            2. 判断是否需要工具支持（如搜索、数据处理等）
            3. 如需工具：调用合适的工具并获取结果
            4. 基于工具结果或已有知识，给出准确回答
            5. 确保答案完整、清晰、有价值
            
            【回答要求】
            - 准确性：基于事实，不编造信息
            - 完整性：回答要全面，涵盖用户关注点
            - 清晰性：结构化表达，条理分明
            - 中文：使用简体中文回复
            - 主动性：信息不足时主动使用工具
            """;
    }
    
    /**
     * 默认下一步提示词（优化版，引导DeepSeek V3.1推理）
     */
    private String getDefaultNextStepPrompt() {
        return """
            
            【下一步决策】
            请分析当前状态，决定下一步行动：
            
            选项A：使用工具获取信息
            - 如果需要外部信息、实时数据、或需要执行操作
            - 格式：TOOL_CALL: {"name": "工具名", "params": {"参数名": "参数值"}}
            - 示例：TOOL_CALL: {"name": "web_search", "params": {"query": "人工智能发展趋势", "limit": 10}}
            
            选项B：直接回答用户
            - 如果已有足够信息可以准确回答
            - 格式：FINAL_ANSWER: [你的完整答案]
            - 要求：答案要完整、准确、结构清晰
            
            【请给出决定】
            （先简要分析，再给出具体动作）
            """;
    }
    
    /**
     * 默认工具列表
     */
    private List<String> getDefaultTools() {
        return List.of("web_search");  // 默认只使用web_search，role_draft_generator按需使用
    }
}

