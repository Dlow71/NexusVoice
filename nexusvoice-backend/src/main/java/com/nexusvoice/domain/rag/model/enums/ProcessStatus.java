package com.nexusvoice.domain.rag.model.enums;

/**
 * 处理状态枚举
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public enum ProcessStatus {
    PENDING(0, "待处理"),
    UPLOADING(1, "上传中"),
    PARSING(2, "解析中"),
    SPLITTING(3, "分割中"),
    VECTORIZING(4, "向量化中"),
    COMPLETED(5, "已完成"),
    FAILED(6, "失败");
    
    private final int value;
    private final String description;
    
    ProcessStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据value获取枚举
     */
    public static ProcessStatus fromValue(int value) {
        for (ProcessStatus status : ProcessStatus.values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的处理状态值: " + value);
    }
    
    /**
     * 是否为终态
     */
    public boolean isFinalStatus() {
        return this == COMPLETED || this == FAILED;
    }
    
    /**
     * 是否为处理中
     */
    public boolean isProcessing() {
        return this == UPLOADING || this == PARSING || this == SPLITTING || this == VECTORIZING;
    }
    
    /**
     * 能否转换到目标状态
     */
    public boolean canTransitionTo(ProcessStatus target) {
        if (this == target) {
            return false;
        }
        
        if (isFinalStatus()) {
            return false; // 终态不能转换
        }
        
        // 失败状态可以从任何非终态转换
        if (target == FAILED) {
            return true;
        }
        
        // 定义状态转换规则
        switch (this) {
            case PENDING:
                return target == UPLOADING;
            case UPLOADING:
                return target == PARSING;
            case PARSING:
                return target == SPLITTING;
            case SPLITTING:
                return target == VECTORIZING;
            case VECTORIZING:
                return target == COMPLETED;
            default:
                return false;
        }
    }
}
