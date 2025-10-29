package com.nexusvoice.domain.developer.constant;

/**
 * 开发者API密钥权限范围枚举
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
public enum ApiKeyScope {
    
    /**
     * 聊天对话权限
     */
    CHAT("chat", "聊天对话"),
    
    /**
     * 图像生成权限
     */
    IMAGE("image", "图像生成"),
    
    /**
     * TTS语音合成权限
     */
    TTS("tts", "语音合成"),
    
    /**
     * ASR语音识别权限
     */
    ASR("asr", "语音识别"),
    
    /**
     * 向量化权限
     */
    EMBEDDING("embedding", "向量化"),
    
    /**
     * 重排序权限
     */
    RERANK("rerank", "重排序"),
    
    /**
     * 视频生成权限
     */
    VIDEO("video", "视频生成"),
    
    /**
     * 文件管理权限
     */
    FILE("file", "文件管理"),
    
    /**
     * 所有权限（超级密钥）
     */
    ALL("all", "所有权限");
    
    private final String code;
    private final String description;
    
    ApiKeyScope(String code, String description) {
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
     * 根据code获取枚举
     */
    public static ApiKeyScope fromCode(String code) {
        for (ApiKeyScope scope : values()) {
            if (scope.code.equals(code)) {
                return scope;
            }
        }
        return null;
    }
}
