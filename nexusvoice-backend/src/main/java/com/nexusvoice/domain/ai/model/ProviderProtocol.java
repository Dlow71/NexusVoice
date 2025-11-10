package com.nexusvoice.domain.ai.model;

/**
 * AI服务商协议类型枚举
 * 定义不同服务商支持的API协议
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
public enum ProviderProtocol {
    
    /**
     * OpenAI兼容协议
     * 适用于：OpenAI、DeepSeek、Grok、豆包、硅基流动等
     */
    OPENAI_COMPATIBLE("openai_compatible", "OpenAI兼容协议"),
    
    /**
     * Anthropic Claude协议
     * 适用于：Claude系列模型
     */
    ANTHROPIC("anthropic", "Anthropic协议"),
    
    /**
     * 阿里云通义千问DashScope协议
     * 适用于：通义千问系列模型
     */
    DASHSCOPE("dashscope", "DashScope协议"),
    
    /**
     * 百度千帆协议
     * 适用于：文心一言系列模型
     */
    QIANFAN("qianfan", "千帆协议"),
    
    /**
     * 自定义协议
     * 适用于：用户自定义的非标准协议
     */
    CUSTOM("custom", "自定义协议");
    
    private final String code;
    private final String name;
    
    ProviderProtocol(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * 根据code获取枚举
     * 
     * @param code 协议代码
     * @return 协议枚举，未找到则返回CUSTOM
     */
    public static ProviderProtocol fromCode(String code) {
        if (code == null) {
            return CUSTOM;
        }
        
        for (ProviderProtocol protocol : values()) {
            if (protocol.code.equalsIgnoreCase(code)) {
                return protocol;
            }
        }
        return CUSTOM;
    }
    
    /**
     * 是否为OpenAI兼容协议
     */
    public boolean isOpenAiCompatible() {
        return this == OPENAI_COMPATIBLE;
    }
    
    /**
     * 是否需要自定义适配器
     */
    public boolean needsCustomAdapter() {
        return this == CUSTOM;
    }
}
