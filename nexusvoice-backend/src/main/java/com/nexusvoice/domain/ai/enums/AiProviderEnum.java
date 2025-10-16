package com.nexusvoice.domain.ai.enums;

/**
 * AI提供商枚举
 * 定义支持的AI服务提供商
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
public enum AiProviderEnum {
    
    OPENAI("openai", "OpenAI", "OpenAI GPT系列模型"),
    CLAUDE("claude", "Anthropic Claude", "Anthropic Claude系列模型"),
    GEMINI("gemini", "Google Gemini", "Google Gemini系列模型"),
    WENXIN("wenxin", "百度文心", "百度文心一言系列模型"),
    QWEN("qwen", "阿里通义千问", "阿里通义千问系列模型"),
    ZHIPU("zhipu", "智谱AI", "智谱GLM系列模型");
    
    private final String code;
    private final String name;
    private final String description;
    
    AiProviderEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static AiProviderEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AiProviderEnum provider : values()) {
            if (provider.getCode().equalsIgnoreCase(code)) {
                return provider;
            }
        }
        return null;
    }
}
