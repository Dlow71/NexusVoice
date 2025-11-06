package com.nexusvoice.domain.agent.enums;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    
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
     * 已跳过
     */
    SKIPPED("skipped", "已跳过");
    
    private final String code;
    private final String description;
    
    TaskStatus(String code, String description) {
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

