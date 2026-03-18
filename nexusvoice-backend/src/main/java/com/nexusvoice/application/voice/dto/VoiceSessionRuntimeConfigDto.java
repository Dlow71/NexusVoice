package com.nexusvoice.application.voice.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.nexusvoice.application.conversation.dto.ConversationRuntimeConfigDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 语音会话运行配置响应。
 */
@Data
@Builder
@Schema(description = "语音会话运行配置")
public class VoiceSessionRuntimeConfigDto {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "语音会话主键ID")
    private Long id;

    @Schema(description = "语音会话ID")
    private String voiceSessionId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "对话ID")
    private Long conversationId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "会话状态")
    private String state;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "语音类型")
    private String voiceType;

    @Schema(description = "ASR模型键")
    private String asrModelKey;

    @Schema(description = "是否严格模式")
    private Boolean strictMode;

    @Schema(description = "是否启用RAG")
    private Boolean ragEnabled;

    @Schema(description = "知识库ID列表JSON")
    private String knowledgeBaseIds;

    @Schema(description = "会话运行配置")
    private ConversationRuntimeConfigDto runtimeConfig;
}
