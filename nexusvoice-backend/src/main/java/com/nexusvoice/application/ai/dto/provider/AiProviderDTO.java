package com.nexusvoice.application.ai.dto.provider;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * AI服务提供商DTO
 * 用于对外展示服务商信息
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
public class AiProviderDTO {
    
    /**
     * 服务商ID
     */
    private Long id;
    
    /**
     * 厂商代码
     */
    private String providerCode;
    
    /**
     * 厂商名称
     */
    private String providerName;
    
    /**
     * 协议类型
     */
    private String protocol;
    
    /**
     * 协议类型名称
     */
    private String protocolName;
    
    /**
     * 厂商描述
     */
    private String description;
    
    /**
     * 默认API端点
     */
    private String defaultBaseUrl;
    
    /**
     * 是否官方
     */
    private Boolean isOfficial;
    
    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;
    
    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 优先级
     */
    private Integer priority;
    
    /**
     * 用户ID（仅自定义服务商有）
     */
    private Long userId;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // ========== Getter and Setter ==========
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public String getProtocolName() {
        return protocolName;
    }
    
    public void setProtocolName(String protocolName) {
        this.protocolName = protocolName;
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
    
    public Boolean getIsOfficial() {
        return isOfficial;
    }
    
    public void setIsOfficial(Boolean isOfficial) {
        this.isOfficial = isOfficial;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getStatusName() {
        return statusName;
    }
    
    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
