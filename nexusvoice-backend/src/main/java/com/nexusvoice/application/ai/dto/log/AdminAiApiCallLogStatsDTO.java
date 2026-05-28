package com.nexusvoice.application.ai.dto.log;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端 AI API 调用日志统计 DTO
 *
 * @author NexusVoice
 * @since 2026-03-19
 */
public class AdminAiApiCallLogStatsDTO {

    /**
     * 调用总数
     */
    private Long totalCalls = 0L;

    /**
     * 成功调用数
     */
    private Long successCalls = 0L;

    /**
     * 失败调用数
     */
    private Long failedCalls = 0L;

    /**
     * 成功率（百分比）
     */
    private BigDecimal successRate = BigDecimal.ZERO;

    /**
     * 总 Token 数
     */
    private Long totalTokens = 0L;

    /**
     * 总费用
     */
    private BigDecimal totalCost = BigDecimal.ZERO;

    /**
     * 平均响应耗时（毫秒）
     */
    private BigDecimal avgResponseTimeMs = BigDecimal.ZERO;

    /**
     * 热门模型统计
     */
    private List<TopModelStat> topModels = new ArrayList<>();

    public Long getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(Long totalCalls) {
        this.totalCalls = totalCalls;
    }

    public Long getSuccessCalls() {
        return successCalls;
    }

    public void setSuccessCalls(Long successCalls) {
        this.successCalls = successCalls;
    }

    public Long getFailedCalls() {
        return failedCalls;
    }

    public void setFailedCalls(Long failedCalls) {
        this.failedCalls = failedCalls;
    }

    public BigDecimal getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(BigDecimal successRate) {
        this.successRate = successRate;
    }

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getAvgResponseTimeMs() {
        return avgResponseTimeMs;
    }

    public void setAvgResponseTimeMs(BigDecimal avgResponseTimeMs) {
        this.avgResponseTimeMs = avgResponseTimeMs;
    }

    public List<TopModelStat> getTopModels() {
        return topModels;
    }

    public void setTopModels(List<TopModelStat> topModels) {
        this.topModels = topModels;
    }

    /**
     * 热门模型统计项
     */
    public static class TopModelStat {
        private String providerCode;
        private String modelCode;
        private Long totalCalls = 0L;
        private Long successCalls = 0L;
        private Long failedCalls = 0L;
        private BigDecimal successRate = BigDecimal.ZERO;
        private Long totalTokens = 0L;
        private BigDecimal totalCost = BigDecimal.ZERO;
        private BigDecimal avgResponseTimeMs = BigDecimal.ZERO;

        public String getProviderCode() {
            return providerCode;
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

        public Long getTotalCalls() {
            return totalCalls;
        }

        public void setTotalCalls(Long totalCalls) {
            this.totalCalls = totalCalls;
        }

        public Long getSuccessCalls() {
            return successCalls;
        }

        public void setSuccessCalls(Long successCalls) {
            this.successCalls = successCalls;
        }

        public Long getFailedCalls() {
            return failedCalls;
        }

        public void setFailedCalls(Long failedCalls) {
            this.failedCalls = failedCalls;
        }

        public BigDecimal getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(BigDecimal successRate) {
            this.successRate = successRate;
        }

        public Long getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Long totalTokens) {
            this.totalTokens = totalTokens;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
        }

        public BigDecimal getAvgResponseTimeMs() {
            return avgResponseTimeMs;
        }

        public void setAvgResponseTimeMs(BigDecimal avgResponseTimeMs) {
            this.avgResponseTimeMs = avgResponseTimeMs;
        }
    }
}
