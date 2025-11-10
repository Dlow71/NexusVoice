package com.nexusvoice.application.ai.dto.provider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建AI服务提供商DTO
 * 用于接收创建服务商的请求参数
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
public class CreateAiProviderDTO {
    
    /**
     * 厂商代码：只允许小写字母、数字、下划线
     */
    @NotBlank(message = "服务商代码不能为空")
    @Size(min = 2, max = 50, message = "服务商代码长度必须在2-50个字符之间")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "服务商代码只能包含小写字母、数字和下划线")
    private String providerCode;
    
    /**
     * 厂商名称
     */
    @NotBlank(message = "服务商名称不能为空")
    @Size(min = 2, max = 100, message = "服务商名称长度必须在2-100个字符之间")
    private String providerName;
    
    /**
     * 协议类型
     */
    @NotBlank(message = "协议类型不能为空")
    @Pattern(regexp = "^(openai_compatible|anthropic|dashscope|qianfan|custom)$", 
             message = "协议类型必须是：openai_compatible、anthropic、dashscope、qianfan或custom")
    private String protocol;
    
    /**
     * 厂商描述
     */
    @Size(max = 500, message = "厂商描述不能超过500个字符")
    private String description;
    
    /**
     * 默认API端点
     */
    @NotBlank(message = "API端点不能为空")
    @Size(max = 500, message = "API端点不能超过500个字符")
    @Pattern(regexp = "^https?://.*", message = "API端点必须是有效的HTTP或HTTPS地址")
    private String defaultBaseUrl;
    
    /**
     * 厂商配置JSON
     */
    private String configJson;
    
    /**
     * 优先级（可选，默认100）
     */
    private Integer priority;
    
    // ========== Getter and Setter ==========
    
    public String getProviderCode() {
        return providerCode;
    }
    
    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }
    
    public String getProviderName() {
        return providerName;
    }
    
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }
    
    public void setDefaultBaseUrl(String defaultBaseUrl) {
        this.defaultBaseUrl = defaultBaseUrl;
    }
    
    public String getConfigJson() {
        return configJson;
    }
    
    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
