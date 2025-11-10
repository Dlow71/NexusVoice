package com.nexusvoice.application.ai.dto.provider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新AI服务提供商DTO
 * 用于接收更新服务商的请求参数
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
public class UpdateAiProviderDTO {
    
    /**
     * 厂商名称（可选）
     */
    @Size(min = 2, max = 100, message = "服务商名称长度必须在2-100个字符之间")
    private String providerName;
    
    /**
     * 厂商描述
     */
    @Size(max = 500, message = "厂商描述不能超过500个字符")
    private String description;
    
    /**
     * 默认API端点（可选）
     */
    @Size(max = 500, message = "API端点不能超过500个字符")
    @Pattern(regexp = "^https?://.*", message = "API端点必须是有效的HTTP或HTTPS地址")
    private String defaultBaseUrl;
    
    /**
     * 厂商配置JSON
     */
    private String configJson;
    
    /**
     * 优先级
     */
    private Integer priority;
    
    // ========== Getter and Setter ==========
    
    public String getProviderName() {
        return providerName;
    }
    
    public void setProviderName(String providerName) {
        this.providerName = providerName;
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
