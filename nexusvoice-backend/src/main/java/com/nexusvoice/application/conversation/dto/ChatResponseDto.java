package com.nexusvoice.application.conversation.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 聊天响应DTO
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Data
@Builder
@Schema(description = "聊天响应")
public class ChatResponseDto {

    @Schema(description = "对话ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long conversationId;

    @Schema(description = "消息ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long messageId;

    @Schema(description = "AI回复内容")
    private String content;

    @Schema(description = "响应时间（毫秒）")
    private Long responseTimeMs;

    @Schema(description = "使用的模型名称")
    private String model;

    @Schema(description = "令牌使用统计")
    private TokenUsageDto usage;

    @Schema(description = "完成原因")
    private String finishReason;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "消息创建时间")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createdAt;

    @Schema(description = "AI回复语音地址")
    private String audioUrl;

    @Data
    @Builder
    @Schema(description = "令牌使用统计")
    public static class TokenUsageDto {
        @Schema(description = "输入令牌数")
        private Integer promptTokens;

        @Schema(description = "输出令牌数") 
        private Integer completionTokens;

        @Schema(description = "总令牌数")
        private Integer totalTokens;
    }

    /**
     * 创建成功响应
     */
    public static ChatResponseDto success(Long conversationId, Long messageId, String content, 
                                        String model, TokenUsageDto usage, Long responseTime) {
        return ChatResponseDto.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .content(content)
                .model(model)
                .usage(usage)
                .responseTimeMs(responseTime)
                .success(true)
                .finishReason("stop")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建成功响应（包含音频URL）
     */
    public static ChatResponseDto success(Long conversationId, Long messageId, String content,
                                        String model, TokenUsageDto usage, Long responseTime, String audioUrl) {
        return ChatResponseDto.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .content(content)
                .model(model)
                .usage(usage)
                .responseTimeMs(responseTime)
                .audioUrl(audioUrl)
                .success(true)
                .finishReason("stop")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败响应
     */
    public static ChatResponseDto error(String errorMessage) {
        return ChatResponseDto.builder()
                .success(false)
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
