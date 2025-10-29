package com.nexusvoice.domain.ai.model;

import com.nexusvoice.domain.common.BaseDomainEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI API调用日志实体
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiApiCallLog extends BaseDomainEntity {
    
    /**
     * API密钥ID
     */
    private Long apiKeyId;

    /**
     * 开发者API Key ID（用户侧密钥）
     */
    private Long developerApiKeyId;

    /**
     * 认证类型：JWT/API_KEY
     */
    private String authType;
    
    /**
     * 厂商代码
     */
    private String providerCode;
    
    /**
     * 模型代码
     */
    private String modelCode;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 对话ID
     */
    private Long conversationId;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 请求时间
     */
    private LocalDateTime requestTime;
    
    /**
     * 请求参数JSON
     */
    private String requestParams;
    
    /**
     * 响应时间
     */
    private LocalDateTime responseTime;
    
    /**
     * 响应耗时（毫秒）
     */
    private Integer responseTimeMs;
    
    /**
     * 状态：0-失败 1-成功
     */
    private Integer status;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 输入tokens
     */
    private Integer promptTokens;
    
    /**
     * 输出tokens
     */
    private Integer completionTokens;
    
    /**
     * 总tokens
     */
    private Integer totalTokens;
    
    /**
     * 输入费用（元）
     */
    private BigDecimal inputCost;
    
    /**
     * 输出费用（元）
     */
    private BigDecimal outputCost;
    
    /**
     * 总费用（元）
     */
    private BigDecimal totalCost;
    
    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return status != null && status == 1;
    }
    
    /**
     * 创建成功日志
     */
    public static AiApiCallLog success(Long apiKeyId, String providerCode, String modelCode,
                                       Long userId, Long conversationId,
                                       String requestId, LocalDateTime requestTime,
                                       Integer responseTimeMs, 
                                       Integer promptTokens, Integer completionTokens,
                                       BigDecimal totalCost) {
        AiApiCallLog log = new AiApiCallLog();
        log.setApiKeyId(apiKeyId);
        log.setProviderCode(providerCode);
        log.setModelCode(modelCode);
        log.setUserId(userId);
        log.setConversationId(conversationId);
        log.setRequestId(requestId);
        log.setRequestTime(requestTime);
        log.setResponseTime(LocalDateTime.now());
        log.setResponseTimeMs(responseTimeMs);
        log.setStatus(1);
        log.setPromptTokens(promptTokens);
        log.setCompletionTokens(completionTokens);
        log.setTotalTokens(promptTokens + completionTokens);
        log.setTotalCost(totalCost);
        return log;
    }
    
    /**
     * 创建失败日志
     */
    public static AiApiCallLog failure(Long apiKeyId, String providerCode, String modelCode,
                                       Long userId, Long conversationId,
                                       String requestId, LocalDateTime requestTime,
                                       String errorMessage) {
        AiApiCallLog log = new AiApiCallLog();
        log.setApiKeyId(apiKeyId);
        log.setProviderCode(providerCode);
        log.setModelCode(modelCode);
        log.setUserId(userId);
        log.setConversationId(conversationId);
        log.setRequestId(requestId);
        log.setRequestTime(requestTime);
        log.setResponseTime(LocalDateTime.now());
        log.setStatus(0);
        log.setErrorMessage(errorMessage);
        return log;
    }
}
