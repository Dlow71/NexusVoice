package com.nexusvoice.domain.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户会话领域模型
 * 用于管理用户的登录会话
 *
 * @author NexusVoice
 * @since 2025-01-27
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSession {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 设备标识
     */
    private String deviceId;
    
    /**
     * 设备类型（web、mobile、desktop等）
     */
    private String deviceType;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveAt;
    
    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 是否已过期
     * 使用@JsonIgnore避免被序列化为expired字段
     */
    @JsonIgnore
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 更新最后活跃时间
     */
    public void updateLastActive() {
        this.lastActiveAt = LocalDateTime.now();
    }
}
