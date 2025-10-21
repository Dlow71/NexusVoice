package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户持久化对象（PO）
 * 用于数据库映射，包含所有MyBatis-Plus注解
 * 仅在infrastructure层使用
 * 
 * @author NexusVoice
 * @since 2025-10-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class UserPO extends BasePO {

    private static final long serialVersionUID = 1L;

    /**
     * 用户登录邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 加密后的用户密码
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 用户昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 用户头像图片的URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 手机号码
     */
    @TableField("phone")
    private String phone;

    /**
     * 用户类型 (USER, ADMIN)
     */
    @TableField("user_type")
    private String userType;

    /**
     * 账户状态 (1-正常, 2-封禁, 3-待激活)
     */
    @TableField("status")
    private Integer status;

    /**
     * 邮箱是否已验证 (0-未验证, 1-已验证)
     */
    @TableField("email_verified")
    private Integer emailVerified;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 用户简介
     */
    @TableField("profile_bio")
    private String profileBio;

    /**
     * OAuth提供商
     */
    @TableField("oauth_provider")
    private String oauthProvider;

    /**
     * OAuth用户ID
     */
    @TableField("oauth_id")
    private String oauthId;

    /**
     * OAuth用户名
     */
    @TableField("oauth_username")
    private String oauthUsername;

    /**
     * OAuth头像URL
     */
    @TableField("oauth_avatar_url")
    private String oauthAvatarUrl;

    /**
     * OAuth访问令牌
     */
    @TableField("oauth_access_token")
    private String oauthAccessToken;

    /**
     * OAuth刷新令牌
     */
    @TableField("oauth_refresh_token")
    private String oauthRefreshToken;

    /**
     * OAuth令牌过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("oauth_token_expires_at")
    private LocalDateTime oauthTokenExpiresAt;

    /**
     * OAuth绑定时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("oauth_bind_time")
    private LocalDateTime oauthBindTime;

    /**
     * OAuth原始数据
     */
    @TableField("oauth_raw_data")
    private String oauthRawData;
}
