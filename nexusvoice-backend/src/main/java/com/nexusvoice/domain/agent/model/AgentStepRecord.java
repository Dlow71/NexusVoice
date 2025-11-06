package com.nexusvoice.domain.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent执行步骤记录值对象（纯POJO）
 * 
 * 职责：
 * - 记录Agent单步执行的详细信息
 * - 用于调试和分析
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStepRecord {
    
    /**
     * 步骤序号
     */
    private Integer stepNumber;
    
    /**
     * 步骤类型：think/act/observe
     */
    private String stepType;
    
    /**
     * 步骤描述
     */
    private String description;
    
    /**
     * 输入内容
     */
    private String input;
    
    /**
     * 输出内容
     */
    private String output;
    
    /**
     * 调用的工具名称（如果有）
     */
    private String toolName;
    
    /**
     * 工具参数（如果有）
     */
    private String toolParams;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 创建Think步骤记录
     */
    public static AgentStepRecord think(Integer stepNumber, String input, String output) {
        return AgentStepRecord.builder()
            .stepNumber(stepNumber)
            .stepType("think")
            .description("LLM思考决策")
            .input(input)
            .output(output)
            .success(true)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 创建Act步骤记录
     */
    public static AgentStepRecord act(Integer stepNumber, String toolName, String toolParams, String output) {
        return AgentStepRecord.builder()
            .stepNumber(stepNumber)
            .stepType("act")
            .description("执行工具调用")
            .toolName(toolName)
            .toolParams(toolParams)
            .output(output)
            .success(true)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 创建Observe步骤记录
     */
    public static AgentStepRecord observe(Integer stepNumber, String input, String output) {
        return AgentStepRecord.builder()
            .stepNumber(stepNumber)
            .stepType("observe")
            .description("观察结果并判断")
            .input(input)
            .output(output)
            .success(true)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}

