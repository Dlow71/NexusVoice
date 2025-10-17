package com.nexusvoice.domain.ai.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexusvoice.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

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
@Accessors(chain = true)
@TableName("ai_api_call_logs")
public class AiApiCallLog extends BaseEntity {
    
    /**
     * API密钥ID
     */
    @TableField("api_key_id")
    private Long apiKeyId;
    
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
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 对话ID
     */
    @TableField("conversation_id")
    private Long conversationId;
    
    /**
     * 请求ID
     */
    @TableField("request_id")
    private String requestId;
    
    /**
     * 请求时间
     */
    @TableField("request_time")
    private LocalDateTime requestTime;
    
    /**
     * 请求参数JSON
     */
    @TableField("request_params")
    private String requestParams;
    
    /**
     * 响应时间
     */
    @TableField("response_time")
    private LocalDateTime responseTime;
    
    /**
     * 响应耗时（毫秒）
     */
    @TableField("response_time_ms")
    private Integer responseTimeMs;
    
    /**
     * 状态：0-失败 1-成功
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;
    
    /**
     * 输入tokens
     */
    @TableField("prompt_tokens")
    private Integer promptTokens;
    
    /**
     * 输出tokens
     */
    @TableField("completion_tokens")
    private Integer completionTokens;
    
    /**
     * 总tokens
     */
    @TableField("total_tokens")
    private Integer totalTokens;
    
    /**
     * 输入费用（元）
     */
    @TableField("input_cost")
    private BigDecimal inputCost;
    
    /**
     * 输出费用（元）
     */
    @TableField("output_cost")
    private BigDecimal outputCost;
    
    /**
     * 总费用（元）
     */
    @TableField("total_cost")
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
