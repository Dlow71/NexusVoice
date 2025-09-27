package com.nexusvoice.application.conversation.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建对话响应
 */
@Data
@Builder
@Schema(description = "创建对话响应")
public class ConversationCreateResponse {

    @Schema(description = "对话ID")
    private Long conversationId;

    @Schema(description = "对话标题")
    private String title;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "系统提示词")
    private String systemPrompt;

    @Schema(description = "角色ID（如果绑定了角色则返回）")
    private Long roleId;

    @Schema(description = "创建时间")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createdAt;
}
