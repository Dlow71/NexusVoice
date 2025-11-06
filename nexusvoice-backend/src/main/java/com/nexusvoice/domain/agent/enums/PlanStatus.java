package com.nexusvoice.domain.agent.enums;

/**
 * 计划状态枚举
 */
public enum PlanStatus {
    
    /**
     * 待执行
     */
    PENDING("pending", "待执行"),
    
    /**
     * 执行中
     */
    RUNNING("running", "执行中"),
    
    /**
     * 已完成
     */
    COMPLETED("completed", "已完成"),
    
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
    
    PlanStatus(String code, String description) {
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

