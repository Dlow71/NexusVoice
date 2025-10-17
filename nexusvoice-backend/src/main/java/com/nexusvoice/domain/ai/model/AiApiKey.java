package com.nexusvoice.domain.ai.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexusvoice.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI API密钥池实体
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("ai_api_keys")
public class AiApiKey extends BaseEntity {
    
    /**
     * 厂商代码
     */
    @TableField("provider_code")
    private String providerCode;
    
    /**
     * 模型代码
     */
    @TableField("model_code")
    private String modelCode;
    
    /**
     * API密钥（加密存储）
     */
    @TableField("api_key")
    private String apiKey;
    
    /**
     * API Secret（某些厂商需要）
     */
    @TableField("api_secret")
    private String apiSecret;
    
    /**
     * 自定义端点URL
     */
    @TableField("base_url")
    private String baseUrl;
    
    /**
     * 代理URL
     */
    @TableField("proxy_url")
    private String proxyUrl;
    
    /**
     * 权重（用于加权轮询）
     */
    @TableField("weight")
    private Integer weight;
    
    /**
     * 每分钟请求数限制
     */
    @TableField("rate_limit")
    private Integer rateLimit;
    
    /**
     * 并发请求数限制
     */
    @TableField("concurrent_limit")
    private Integer concurrentLimit;
    
    /**
     * 状态：0-异常 1-正常 2-禁用
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 连续失败次数
     */
    @TableField("fail_count")
    private Integer failCount;
    
    /**
     * 最后失败时间
     */
    @TableField("last_fail_time")
    private LocalDateTime lastFailTime;
    
    /**
     * 最后成功时间
     */
    @TableField("last_success_time")
    private LocalDateTime lastSuccessTime;
    
    /**
     * 最后健康检查时间
     */
    @TableField("health_check_time")
    private LocalDateTime healthCheckTime;
    
    /**
     * 总请求数
     */
    @TableField("total_requests")
    private Long totalRequests;
    
    /**
     * 总token使用量
     */
    @TableField("total_tokens_used")
    private Long totalTokensUsed;
    
    /**
     * 总费用（元）
     */
    @TableField("total_cost")
    private BigDecimal totalCost;
    
    /**
     * 最后使用时间
     */
    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;
    
    /**
     * 当月请求数
     */
    @TableField("monthly_requests")
    private Integer monthlyRequests;
    
    /**
     * 当月token使用量
     */
    @TableField("monthly_tokens_used")
    private Long monthlyTokensUsed;
    
    /**
     * 当月费用（元）
     */
    @TableField("monthly_cost")
    private BigDecimal monthlyCost;
    
    /**
     * 月度重置日期
     */
    @TableField("monthly_reset_date")
    private LocalDate monthlyResetDate;
    
    /**
     * 每日配额限制（tokens）
     */
    @TableField("daily_quota_limit")
    private Long dailyQuotaLimit;
    
    /**
     * 月度配额限制（tokens）
     */
    @TableField("monthly_quota_limit")
    private Long monthlyQuotaLimit;
    
    /**
     * 今日已用tokens
     */
    @TableField("daily_tokens_used")
    private Long dailyTokensUsed;
    
    /**
     * 获取API Key标识
     */
    public String getKeyIdentifier() {
        return providerCode + ":" + modelCode + ":" + id;
    }
    
    /**
     * 是否可用
     */
    public boolean isAvailable() {
        // 状态正常且未超过失败阈值
        return status != null && status == 1 && (failCount == null || failCount < 5);
    }
    
    /**
     * 是否需要熔断
     */
    public boolean needsCircuitBreaker() {
        return failCount != null && failCount >= 5;
    }
    
    /**
     * 检查是否超过日限额
     */
    public boolean isOverDailyQuota() {
        if (dailyQuotaLimit == null || dailyQuotaLimit <= 0) {
            return false;
        }
        return dailyTokensUsed != null && dailyTokensUsed >= dailyQuotaLimit;
    }
    
    /**
     * 检查是否超过月限额
     */
    public boolean isOverMonthlyQuota() {
        if (monthlyQuotaLimit == null || monthlyQuotaLimit <= 0) {
            return false;
        }
        return monthlyTokensUsed != null && monthlyTokensUsed >= monthlyQuotaLimit;
    }
    
    /**
     * 更新使用统计
     */
    public void updateUsageStats(int tokens, BigDecimal cost) {
        // 更新总计
        this.totalRequests = (totalRequests == null ? 0 : totalRequests) + 1;
        this.totalTokensUsed = (totalTokensUsed == null ? 0 : totalTokensUsed) + tokens;
        this.totalCost = (totalCost == null ? BigDecimal.ZERO : totalCost).add(cost);
        
        // 更新月度
        this.monthlyRequests = (monthlyRequests == null ? 0 : monthlyRequests) + 1;
        this.monthlyTokensUsed = (monthlyTokensUsed == null ? 0 : monthlyTokensUsed) + tokens;
        this.monthlyCost = (monthlyCost == null ? BigDecimal.ZERO : monthlyCost).add(cost);
        
        // 更新日度
        this.dailyTokensUsed = (dailyTokensUsed == null ? 0 : dailyTokensUsed) + tokens;
        
        this.lastUsedAt = LocalDateTime.now();
        this.lastSuccessTime = LocalDateTime.now();
        this.failCount = 0; // 重置失败计数
    }
    
    /**
     * 标记失败
     */
    public void markFailed() {
        this.failCount = (failCount == null ? 0 : failCount) + 1;
        this.lastFailTime = LocalDateTime.now();
        
        // 连续失败5次则自动禁用
        if (this.failCount >= 5) {
            this.status = 0; // 异常状态
        }
    }
}
