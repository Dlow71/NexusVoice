package com.nexusvoice.domain.agent.enums;

/**
 * Agent状态枚举
 */
public enum AgentState {
    
    /**
     * 空闲状态（未开始执行）
     */
    IDLE("idle", "空闲"),
    
    /**
     * 运行中
     */
    RUNNING("running", "运行中"),
    
    /**
     * 已完成
     */
    FINISHED("finished", "已完成"),
    
    /**
     * 失败
     */
    FAILED("failed", "失败"),
    
    /**
     * 已取消
     */
    CANCELLED("cancelled", "已取消");
    
    private final String code;
    private final String description;
    
    AgentState(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}

