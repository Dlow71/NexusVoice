package com.nexusvoice.interfaces.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 管理端 AI API 密钥保存请求
 */
@Schema(description = "管理端 AI API密钥保存请求")
public class AdminAiApiKeySaveRequest {

    @Schema(description = "提供商ID", example = "9")
    private Long providerId;

    @Schema(description = "提供商代码", example = "qiniu")
    private String providerCode;

    @Schema(description = "模型代码", example = "qiniu-tts")
    private String modelCode;

    @Schema(description = "API Key")
    private String apiKey;

    @Schema(description = "API Secret")
    private String apiSecret;

    @Schema(description = "Base URL")
    private String baseUrl;

    @Schema(description = "代理 URL")
    private String proxyUrl;

    @Schema(description = "权重")
    private Integer weight;

    @Schema(description = "每分钟限流")
    private Integer rateLimit;

    @Schema(description = "并发限制")
    private Integer concurrentLimit;

    @Schema(description = "状态：0-异常 1-正常 2-禁用")
    private Integer status;

    @Schema(description = "日配额限制")
    private Long dailyQuotaLimit;

    @Schema(description = "月配额限制")
    private Long monthlyQuotaLimit;

    public String getProviderCode() {
        return providerCode;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(Integer rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Integer getConcurrentLimit() {
        return concurrentLimit;
    }

    public void setConcurrentLimit(Integer concurrentLimit) {
        this.concurrentLimit = concurrentLimit;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getDailyQuotaLimit() {
        return dailyQuotaLimit;
    }

    public void setDailyQuotaLimit(Long dailyQuotaLimit) {
        this.dailyQuotaLimit = dailyQuotaLimit;
    }

    public Long getMonthlyQuotaLimit() {
        return monthlyQuotaLimit;
    }

    public void setMonthlyQuotaLimit(Long monthlyQuotaLimit) {
        this.monthlyQuotaLimit = monthlyQuotaLimit;
    }
}
