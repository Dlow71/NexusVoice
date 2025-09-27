package com.nexusvoice.application.conversation.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;

/**
 * 对话中的角色信息DTO
 * 用于在对话相关接口中返回角色的基本信息
 *
 * @author NexusVoice
 * @since 2025-09-27
 */
@Data
@Builder
@Schema(description = "对话角色信息")
public class RoleInfoDto {

    @Schema(description = "角色ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "TTS声音类型")
    private String voiceType;

    @Schema(description = "是否公共角色")
    private Boolean isPublic;

    @Schema(description = "开场白文本")
    private String greetingMessage;

    @Schema(description = "开场白音频URL")
    private String greetingAudioUrl;
}
