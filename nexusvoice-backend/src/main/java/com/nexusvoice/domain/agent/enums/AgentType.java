package com.nexusvoice.domain.agent.enums;

/**
 * Agent类型枚举
 */
public enum AgentType {
    
    /**
     * ReAct模式：Think-Act-Observe循环
     * 适用于简单、直接的任务
     */
    REACT("react", "ReAct模式"),
    
    /**
     * Plan+Solve模式：先规划后执行
     * 适用于复杂、多步骤的任务
     */
    PLAN_SOLVE("plan_solve", "Plan+Solve模式"),
    
    /**
     * 自定义Agent
     */
    CUSTOM("custom", "自定义Agent");
    
    private final String code;
    private final String description;
    
    AgentType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static AgentType fromCode(String code) {
        for (AgentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return REACT;  // 默认使用ReAct
    }
}

