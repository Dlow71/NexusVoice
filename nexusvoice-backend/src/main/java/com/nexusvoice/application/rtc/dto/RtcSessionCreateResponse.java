package com.nexusvoice.application.rtc.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RTC会话创建响应DTO
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RTC会话创建响应")
public class RtcSessionCreateResponse {
    
    @Schema(description = "会话ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    @Schema(description = "WebRTC会话ID（UUID）")
    private String sessionId;
    
    @Schema(description = "信令WebSocket地址", example = "ws://localhost:8081/ws/rtc/signal")
    private String signalingUrl;
    
    @Schema(description = "STUN服务器地址", example = "stun:stun.l.google.com:19302")
    private String stunServer;
    
    @Schema(description = "会话超时时间（分钟）", example = "30")
    private Integer timeoutMinutes;
}







