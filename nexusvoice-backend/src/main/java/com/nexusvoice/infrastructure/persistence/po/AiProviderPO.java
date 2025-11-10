package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * AI服务提供商持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
@TableName("ai_providers")
public class AiProviderPO extends BasePO {
    
    /**
     * 厂商代码：openai/grok/deepseek等
     */
    @TableField("provider_code")
    private String providerCode;
    
    /**
     * 厂商显示名称
     */
    @TableField("provider_name")
    private String providerName;
    
    /**
     * 协议类型：openai_compatible/anthropic/dashscope等
     */
    @TableField("protocol")
    private String protocol;
    
    /**
     * 厂商描述
     */
    @TableField("description")
    private String description;
    
    /**
     * 默认API端点URL
     */
    @TableField("default_base_url")
    private String defaultBaseUrl;
    
    /**
     * 厂商特定配置JSON
     */
    @TableField("config_json")
    private String configJson;
    
    /**
     * 是否官方内置服务商
     */
    @TableField("is_official")
    private Boolean isOfficial;
    
    /**
     * 用户ID（自定义服务商的创建者）
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 状态：0-禁用 1-启用
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 优先级（越小越优先）
     */
    @TableField("priority")
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
    
    public Boolean getIsOfficial() {
        return isOfficial;
    }
    
    public void setIsOfficial(Boolean isOfficial) {
        this.isOfficial = isOfficial;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
