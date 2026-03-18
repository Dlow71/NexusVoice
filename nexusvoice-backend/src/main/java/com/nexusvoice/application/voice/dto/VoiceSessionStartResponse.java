package com.nexusvoice.application.voice.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 语音会话创建响应。
 */
@Data
@Builder
@Schema(description = "语音会话创建响应")
public class VoiceSessionStartResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "语音会话主键ID")
    private Long id;

    @Schema(description = "语音会话ID")
    private String voiceSessionId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "对话ID")
    private Long conversationId;

    @Schema(description = "实时事件WebSocket地址")
    private String realtimeUrl;

    @Schema(description = "初始状态")
    private String state;

    @Schema(description = "语音会话运行配置")
    private VoiceSessionRuntimeConfigDto runtimeConfig;
}
