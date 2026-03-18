package com.nexusvoice.application.voice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语音实时事件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "语音实时事件")
public class VoiceRealtimeEventDto {

    @Schema(description = "事件类型")
    private String type;

    @Schema(description = "语音会话ID")
    private String voiceSessionId;

    @Schema(description = "轮次")
    private Integer turnNo;

    @Schema(description = "序列号")
    private Long seq;

    @Schema(description = "毫秒时间戳")
    private Long ts;

    @Schema(description = "事件负载")
    private Object payload;
}
