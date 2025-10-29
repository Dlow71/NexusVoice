package com.nexusvoice.domain.developer.constant;

/**
 * 开发者API密钥状态枚举
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
public enum ApiKeyStatus {
    
    /**
     * 正常使用中
     */
    ACTIVE("正常"),
    
    /**
     * 已禁用
     */
    DISABLED("已禁用"),
    
    /**
     * 已过期
     */
    EXPIRED("已过期");
    
    private final String description;
    
    ApiKeyStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
