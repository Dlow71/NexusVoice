package com.nexusvoice.application.conversation.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.nexusvoice.domain.conversation.constant.MessageRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 包含角色信息的对话消息DTO
 * 用于对话历史接口返回消息及其关联的角色信息
 *
 * @author NexusVoice
 * @since 2025-09-27
 */
@Data
@Builder
@Schema(description = "对话消息（含角色信息）")
public class ConversationMessageWithRoleDto {

    @Schema(description = "消息ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "对话ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long conversationId;

    @Schema(description = "消息角色")
    private MessageRole role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "AI回复语音地址")
    private String audioUrl;

    @Schema(description = "消息序号")
    private Integer sequence;

    @Schema(description = "令牌数量")
    private Integer tokenCount;

    @Schema(description = "消息状态")
    private String status;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "消息元数据")
    private String metadata;

    @Schema(description = "消息发送时间")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime sentAt;

    @Schema(description = "创建时间")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updatedAt;

    @Schema(description = "对话绑定的角色ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long conversationRoleId;

    @Schema(description = "对话绑定的角色信息")
    private RoleInfoDto conversationRole;
}
