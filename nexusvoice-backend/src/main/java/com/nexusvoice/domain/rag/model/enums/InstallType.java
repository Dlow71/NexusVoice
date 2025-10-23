package com.nexusvoice.domain.rag.model.enums;

/**
 * 安装类型枚举
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public enum InstallType {
    
    /**
     * 引用类型
     * 动态引用原始数据集，支持实时更新
     */
    REFERENCE("REFERENCE", "引用类型"),
    
    /**
     * 快照类型
     * 使用版本快照数据，内容固定不变
     */
    SNAPSHOT("SNAPSHOT", "快照类型");
    
    private final String code;
    private final String description;
    
    InstallType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static InstallType fromCode(String code) {
        for (InstallType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的安装类型：" + code);
    }
}
