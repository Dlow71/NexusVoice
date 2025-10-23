package com.nexusvoice.domain.rag.model.enums;

/**
 * 任务状态枚举
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public enum TaskStatus {
    PENDING("待处理"),
    RUNNING("执行中"),
    SUCCESS("成功"),
    FAILED("失败"),
    CANCELLED("已取消");
    
    private final String description;
    
    TaskStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 是否为终态
     */
    public boolean isFinalStatus() {
        return this == SUCCESS || this == FAILED || this == CANCELLED;
    }
    
    /**
     * 是否可重试
     */
    public boolean isRetryable() {
        return this == FAILED;
    }
}
