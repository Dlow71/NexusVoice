package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 开发者API密钥持久化对象（PO）
 * 用于数据库映射，包含所有MyBatis-Plus注解
 * 仅在infrastructure层使用
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("developer_api_keys")
public class DeveloperApiKeyPO extends BasePO {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 密钥名称（便于用户识别）
     */
    @TableField("key_name")
    private String keyName;
    
    /**
     * API Key的SHA256哈希值（存储用）
     */
    @TableField("key_value_hash")
    private String keyValueHash;
    
    /**
     * API Key前缀（显示用，如sk-nv-abc123...）
     */
    @TableField("key_prefix")
    private String keyPrefix;
    
    /**
     * 绑定的用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 应用名称（可选）
     */
    @TableField("app_name")
    private String appName;
    
    /**
     * 权限范围（JSONB存储，如["chat","image","tts"]）
     */
    @TableField("scopes")
    private String scopes;
    
    /**
     * 允许的模型列表（JSONB存储，如["openai:gpt-4","doubao:seed-1.6"]）
     */
    @TableField("allowed_models")
    private String allowedModels;
    
    /**
     * IP白名单（JSONB存储，如["192.168.1.1","10.0.0.0/24"]）
     */
    @TableField("ip_whitelist")
    private String ipWhitelist;
    
    /**
     * 每分钟请求次数限制
     */
    @TableField("rate_limit_per_minute")
    private Integer rateLimitPerMinute;
    
    /**
     * 每日请求次数限制
     */
    @TableField("rate_limit_per_day")
    private Integer rateLimitPerDay;
    
    /**
     * 总请求次数
     */
    @TableField("total_request_count")
    private Long totalRequestCount;
    
    /**
     * 今日请求次数
     */
    @TableField("today_request_count")
    private Integer todayRequestCount;
    
    /**
     * 最后请求时间
     */
    @TableField("last_request_at")
    private LocalDateTime lastRequestAt;
    
    /**
     * 总输入Token数
     */
    @TableField("total_input_tokens")
    private Long totalInputTokens;
    
    /**
     * 总输出Token数
     */
    @TableField("total_output_tokens")
    private Long totalOutputTokens;
    
    /**
     * 总Token数（输入+输出）
     */
    @TableField("total_tokens")
    private Long totalTokens;
    
    /**
     * 今日Token数
     */
    @TableField("today_tokens")
    private Long todayTokens;
    
    /**
     * 每日费用限额（元）
     */
    @TableField("daily_cost_limit")
    private BigDecimal dailyCostLimit;
    
    /**
     * 每月费用限额（元）
     */
    @TableField("monthly_cost_limit")
    private BigDecimal monthlyCostLimit;
    
    /**
     * 总费用（元）
     */
    @TableField("total_cost")
    private BigDecimal totalCost;
    
    /**
     * 每日Token限额
     */
    @TableField("daily_token_limit")
    private Long dailyTokenLimit;
    
    /**
     * 每月Token限额
     */
    @TableField("monthly_token_limit")
    private Long monthlyTokenLimit;
    
    /**
     * 状态（ACTIVE-正常，DISABLED-禁用，EXPIRED-过期）
     */
    @TableField("status")
    private String status;
    
    /**
     * 过期时间（为空表示永不过期）
     */
    @TableField("expire_at")
    private LocalDateTime expireAt;
    
    /**
     * 最后使用的IP地址
     */
    @TableField("last_used_ip")
    private String lastUsedIp;
}
