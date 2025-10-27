package com.nexusvoice.domain.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Token黑名单领域模型
 * 用于存储已失效的Token
 *
 * @author NexusVoice
 * @since 2025-01-27
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenBlacklist {
    
    /**
     * Token（通常是JTI或完整token）
     */
    private String token;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * Token类型（access、refresh）
     */
    private String tokenType;
    
    /**
     * 加入黑名单的原因
     */
    private String reason;
    
    /**
     * 加入时间
     */
    private LocalDateTime addedAt;
    
    /**
     * 过期时间（黑名单记录在token过期后可以删除）
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
}
