package com.nexusvoice.domain.developer.model;

import com.nexusvoice.domain.common.BaseDomainEntity;
import com.nexusvoice.domain.developer.constant.ApiKeyStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 开发者API密钥领域实体
 * 纯净的领域模型，不包含任何技术框架注解
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
public class DeveloperApiKey extends BaseDomainEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 密钥名称（便于用户识别）
     */
    private String keyName;
    
    /**
     * API Key的SHA256哈希值（存储用）
     */
    private String keyValueHash;
    
    /**
     * API Key前缀（显示用，如sk-nv-abc123...）
     */
    private String keyPrefix;
    
    /**
     * 绑定的用户ID
     */
    private Long userId;
    
    /**
     * 应用名称（可选）
     */
    private String appName;
    
    /**
     * 权限范围（如["chat","image","tts"]）
     */
    private List<String> scopes;
    
    /**
     * 允许的模型列表（如["openai:gpt-4","doubao:seed-1.6"]）
     */
    private List<String> allowedModels;
    
    /**
     * IP白名单（如["192.168.1.1","10.0.0.0/24"]）
     */
    private List<String> ipWhitelist;
    
    /**
     * 每分钟请求次数限制
     */
    private Integer rateLimitPerMinute;
    
    /**
     * 每日请求次数限制
     */
    private Integer rateLimitPerDay;
    
    /**
     * 总请求次数
     */
    private Long totalRequestCount;
    
    /**
     * 今日请求次数
     */
    private Integer todayRequestCount;
    
    /**
     * 最后请求时间
     */
    private LocalDateTime lastRequestAt;
    
    /**
     * 总输入Token数
     */
    private Long totalInputTokens;
    
    /**
     * 总输出Token数
     */
    private Long totalOutputTokens;
    
    /**
     * 总Token数（输入+输出）
     */
    private Long totalTokens;
    
    /**
     * 今日Token数
     */
    private Long todayTokens;
    
    /**
     * 每日费用限额（元）
     */
    private BigDecimal dailyCostLimit;
    
    /**
     * 每月费用限额（元）
     */
    private BigDecimal monthlyCostLimit;
    
    /**
     * 总费用（元）
     */
    private BigDecimal totalCost;
    
    /**
     * 每日Token限额
     */
    private Long dailyTokenLimit;
    
    /**
     * 每月Token限额
     */
    private Long monthlyTokenLimit;
    
    /**
     * 状态
     */
    private ApiKeyStatus status;
    
    /**
     * 过期时间（为空表示永不过期）
     */
    private LocalDateTime expireAt;
    
    /**
     * 最后使用的IP地址
     */
    private String lastUsedIp;
    
    // ========== 业务方法 ==========
    
    /**
     * 检查API Key是否有效
     */
    public boolean isValid() {
        return status == ApiKeyStatus.ACTIVE && !isExpired();
    }
    
    /**
     * 检查API Key是否已过期
     */
    public boolean isExpired() {
        return expireAt != null && LocalDateTime.now().isAfter(expireAt);
    }
    
    /**
     * 检查IP是否在白名单中
     */
    public boolean isIpAllowed(String ip) {
        if (ipWhitelist == null || ipWhitelist.isEmpty()) {
            return true; // 未设置白名单，允许所有IP
        }
        // 支持精确匹配与IPv4 CIDR（a.b.c.d/n）
        for (String rule : ipWhitelist) {
            if (rule == null || rule.isEmpty()) {
                continue;
            }
            String trimmed = rule.trim();
            if (trimmed.equals(ip)) {
                return true;
            }
            // CIDR 支持（仅IPv4）
            int slash = trimmed.indexOf('/');
            if (slash > 0) {
                String network = trimmed.substring(0, slash);
                String prefixStr = trimmed.substring(slash + 1);
                try {
                    int prefix = Integer.parseInt(prefixStr);
                    if (prefix < 0 || prefix > 32) {
                        continue;
                    }
                    long net = ipv4ToLong(network);
                    long addr = ipv4ToLong(ip);
                    long mask = prefix == 0 ? 0L : (~0L << (32 - prefix)) & 0xFFFFFFFFL;
                    if ((net & mask) == (addr & mask)) {
                        return true;
                    }
                } catch (Exception ignored) {
                    // 非法CIDR，忽略该规则
                }
            }
        }
        return false;
    }
    
    /**
     * 检查是否有指定权限
     */
    public boolean hasScope(String scope) {
        if (scopes == null || scopes.isEmpty()) {
            return false;
        }
        // 如果有"all"权限，允许所有操作
        if (scopes.contains("all")) {
            return true;
        }
        return scopes.contains(scope);
    }
    
    /**
     * 检查是否允许访问指定模型
     */
    public boolean isModelAllowed(String modelKey) {
        if (allowedModels == null || allowedModels.isEmpty()) {
            return true; // 未设置模型限制，允许所有模型
        }
        return allowedModels.contains(modelKey);
    }
    
    /**
     * 记录使用情况
     */
    public void recordUsage(String clientIp) {
        if (this.totalRequestCount == null) {
            this.totalRequestCount = 0L;
        }
        if (this.todayRequestCount == null) {
            this.todayRequestCount = 0;
        }
        
        this.totalRequestCount++;
        this.todayRequestCount++;
        this.lastRequestAt = LocalDateTime.now();
        this.lastUsedIp = clientIp;
        this.onUpdate();
    }
    
    /**
     * 增加Token使用量
     */
    public void addTokenUsage(long inputTokens, long outputTokens) {
        if (this.totalInputTokens == null) {
            this.totalInputTokens = 0L;
        }
        if (this.totalOutputTokens == null) {
            this.totalOutputTokens = 0L;
        }
        if (this.totalTokens == null) {
            this.totalTokens = 0L;
        }
        if (this.todayTokens == null) {
            this.todayTokens = 0L;
        }
        
        this.totalInputTokens += inputTokens;
        this.totalOutputTokens += outputTokens;
        long tokens = inputTokens + outputTokens;
        this.totalTokens += tokens;
        this.todayTokens += tokens;
        this.onUpdate();
    }
    
    /**
     * 增加费用
     */
    public void addCost(BigDecimal cost) {
        if (this.totalCost == null) {
            this.totalCost = BigDecimal.ZERO;
        }
        this.totalCost = this.totalCost.add(cost);
        this.onUpdate();
    }
    
    /**
     * 检查是否超出每日Token限额
     */
    public boolean isWithinDailyTokenLimit() {
        if (dailyTokenLimit == null) {
            return true; // 未设置限额，不限制
        }
        return todayTokens == null || todayTokens < dailyTokenLimit;
    }
    
    /**
     * 检查是否超出每日费用限额
     */
    public boolean isWithinDailyCostLimit(BigDecimal currentDailyCost) {
        if (dailyCostLimit == null) {
            return true; // 未设置限额，不限制
        }
        return currentDailyCost.compareTo(dailyCostLimit) <= 0;
    }
    
    /**
     * 重置今日统计（定时任务调用）
     */
    public void resetDailyStats() {
        this.todayRequestCount = 0;
        this.todayTokens = 0L;
        this.onUpdate();
    }
    
    /**
     * 禁用密钥
     */
    public void disable() {
        this.status = ApiKeyStatus.DISABLED;
        this.onUpdate();
    }
    
    /**
     * 启用密钥
     */
    public void enable() {
        if (!isExpired()) {
            this.status = ApiKeyStatus.ACTIVE;
            this.onUpdate();
        }
    }

    private long ipv4ToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address: " + ip);
        }
        long res = 0;
        for (int i = 0; i < 4; i++) {
            int octet = Integer.parseInt(parts[i]);
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("Invalid IPv4 octet: " + parts[i]);
            }
            res = (res << 8) | octet;
        }
        return res & 0xFFFFFFFFL;
    }
    
    // ========== Getter/Setter ==========
    
    public String getKeyName() {
        return keyName;
    }
    
    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }
    
    public String getKeyValueHash() {
        return keyValueHash;
    }
    
    public void setKeyValueHash(String keyValueHash) {
        this.keyValueHash = keyValueHash;
    }
    
    public String getKeyPrefix() {
        return keyPrefix;
    }
    
    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getAppName() {
        return appName;
    }
    
    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    public List<String> getScopes() {
        return scopes;
    }
    
    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
    
    public List<String> getAllowedModels() {
        return allowedModels;
    }
    
    public void setAllowedModels(List<String> allowedModels) {
        this.allowedModels = allowedModels;
    }
    
    public List<String> getIpWhitelist() {
        return ipWhitelist;
    }
    
    public void setIpWhitelist(List<String> ipWhitelist) {
        this.ipWhitelist = ipWhitelist;
    }
    
    public Integer getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }
    
    public void setRateLimitPerMinute(Integer rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }
    
    public Integer getRateLimitPerDay() {
        return rateLimitPerDay;
    }
    
    public void setRateLimitPerDay(Integer rateLimitPerDay) {
        this.rateLimitPerDay = rateLimitPerDay;
    }
    
    public Long getTotalRequestCount() {
        return totalRequestCount;
    }
    
    public void setTotalRequestCount(Long totalRequestCount) {
        this.totalRequestCount = totalRequestCount;
    }
    
    public Integer getTodayRequestCount() {
        return todayRequestCount;
    }
    
    public void setTodayRequestCount(Integer todayRequestCount) {
        this.todayRequestCount = todayRequestCount;
    }
    
    public LocalDateTime getLastRequestAt() {
        return lastRequestAt;
    }
    
    public void setLastRequestAt(LocalDateTime lastRequestAt) {
        this.lastRequestAt = lastRequestAt;
    }
    
    public Long getTotalInputTokens() {
        return totalInputTokens;
    }
    
    public void setTotalInputTokens(Long totalInputTokens) {
        this.totalInputTokens = totalInputTokens;
    }
    
    public Long getTotalOutputTokens() {
        return totalOutputTokens;
    }
    
    public void setTotalOutputTokens(Long totalOutputTokens) {
        this.totalOutputTokens = totalOutputTokens;
    }
    
    public Long getTotalTokens() {
        return totalTokens;
    }
    
    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }
    
    public Long getTodayTokens() {
        return todayTokens;
    }
    
    public void setTodayTokens(Long todayTokens) {
        this.todayTokens = todayTokens;
    }
    
    public BigDecimal getDailyCostLimit() {
        return dailyCostLimit;
    }
    
    public void setDailyCostLimit(BigDecimal dailyCostLimit) {
        this.dailyCostLimit = dailyCostLimit;
    }
    
    public BigDecimal getMonthlyCostLimit() {
        return monthlyCostLimit;
    }
    
    public void setMonthlyCostLimit(BigDecimal monthlyCostLimit) {
        this.monthlyCostLimit = monthlyCostLimit;
    }
    
    public BigDecimal getTotalCost() {
        return totalCost;
    }
    
    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }
    
    public Long getDailyTokenLimit() {
        return dailyTokenLimit;
    }
    
    public void setDailyTokenLimit(Long dailyTokenLimit) {
        this.dailyTokenLimit = dailyTokenLimit;
    }
    
    public Long getMonthlyTokenLimit() {
        return monthlyTokenLimit;
    }
    
    public void setMonthlyTokenLimit(Long monthlyTokenLimit) {
        this.monthlyTokenLimit = monthlyTokenLimit;
    }
    
    public ApiKeyStatus getStatus() {
        return status;
    }
    
    public void setStatus(ApiKeyStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getExpireAt() {
        return expireAt;
    }
    
    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }
    
    public String getLastUsedIp() {
        return lastUsedIp;
    }
    
    public void setLastUsedIp(String lastUsedIp) {
        this.lastUsedIp = lastUsedIp;
    }
}
