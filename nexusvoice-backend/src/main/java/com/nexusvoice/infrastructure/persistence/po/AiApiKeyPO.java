package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI API密钥持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_api_keys")
public class AiApiKeyPO extends BasePO {

    @TableField("provider_id")
    private Long providerId;

    @TableField("provider_code")
    private String providerCode;

    @TableField("model_code")
    private String modelCode;

    @TableField("api_key")
    private String apiKey;

    @TableField("api_secret")
    private String apiSecret;

    @TableField("base_url")
    private String baseUrl;

    @TableField("proxy_url")
    private String proxyUrl;

    @TableField("weight")
    private Integer weight;

    @TableField("rate_limit")
    private Integer rateLimit;

    @TableField("concurrent_limit")
    private Integer concurrentLimit;

    @TableField("status")
    private Integer status;

    @TableField("fail_count")
    private Integer failCount;

    @TableField("last_fail_time")
    private LocalDateTime lastFailTime;

    @TableField("last_success_time")
    private LocalDateTime lastSuccessTime;

    @TableField("health_check_time")
    private LocalDateTime healthCheckTime;

    @TableField("total_requests")
    private Long totalRequests;

    @TableField("total_tokens_used")
    private Long totalTokensUsed;

    @TableField("total_cost")
    private BigDecimal totalCost;

    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;

    @TableField("monthly_requests")
    private Integer monthlyRequests;

    @TableField("monthly_tokens_used")
    private Long monthlyTokensUsed;

    @TableField("monthly_cost")
    private BigDecimal monthlyCost;

    @TableField("monthly_reset_date")
    private LocalDate monthlyResetDate;

    @TableField("daily_quota_limit")
    private Long dailyQuotaLimit;

    @TableField("monthly_quota_limit")
    private Long monthlyQuotaLimit;

    @TableField("daily_tokens_used")
    private Long dailyTokensUsed;
}
