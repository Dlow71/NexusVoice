package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI API调用日志持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_api_call_logs")
public class AiApiCallLogPO extends BasePO {

    /**
     * 日志表不需要deleted字段，排除BasePO的deleted
     */
    @TableField(exist = false)
    private Integer deleted;

    @TableField("api_key_id")
    private Long apiKeyId;

    /**
     * 开发者API Key ID（用户侧密钥）
     */
    @TableField("developer_api_key_id")
    private Long developerApiKeyId;

    /**
     * 认证类型：JWT/API_KEY
     */
    @TableField("auth_type")
    private String authType;

    @TableField("provider_code")
    private String providerCode;

    @TableField("model_code")
    private String modelCode;

    @TableField("user_id")
    private Long userId;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("request_id")
    private String requestId;

    @TableField("request_time")
    private LocalDateTime requestTime;

    @TableField("request_params")
    private String requestParams;

    @TableField("response_time")
    private LocalDateTime responseTime;

    @TableField("response_time_ms")
    private Integer responseTimeMs;

    @TableField("status")
    private Integer status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("prompt_tokens")
    private Integer promptTokens;

    @TableField("completion_tokens")
    private Integer completionTokens;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("input_cost")
    private BigDecimal inputCost;

    @TableField("output_cost")
    private BigDecimal outputCost;

    @TableField("total_cost")
    private BigDecimal totalCost;
}
