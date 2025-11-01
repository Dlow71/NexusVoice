package com.nexusvoice.domain.rtc.model;

import com.nexusvoice.domain.rtc.enums.RtcSessionState;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RTC会话领域实体（纯POJO）
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Data
public class RtcSession {
    
    /** 主键ID */
    private Long id;
    
    /** 会话ID（UUID） */
    private String sessionId;
    
    /** 用户ID */
    private Long userId;
    
    /** 角色ID */
    private Long roleId;
    
    /** 关联对话ID */
    private Long conversationId;
    
    /** 会话状态 */
    private RtcSessionState state;
    
    /** 本地SDP */
    private String localSdp;
    
    /** 远端SDP */
    private String remoteSdp;
    
    /** Kurento Pipeline ID */
    private String kmsPipelineId;
    
    /** Kurento WebRtcEndpoint ID */
    private String kmsWebrtcEndpointId;
    
    /** gRPC ASR会话ID */
    private String grpcAsrSessionId;
    
    /** gRPC TTS会话ID */
    private String grpcTtsSessionId;
    
    /** 当前ASR识别文本 */
    private String currentAsrText;
    
    /** 当前TTS片段ID */
    private Integer currentTtsSegmentId;
    
    /** 打断次数 */
    private Integer interruptCount;
    
    /** 最后错误码 */
    private String lastErrorCode;
    
    /** 最后错误消息 */
    private String lastErrorMessage;
    
    /** 创建时间 */
    private LocalDateTime createdAt;
    
    /** 更新时间 */
    private LocalDateTime updatedAt;
    
    /** 开始时间 */
    private LocalDateTime startedAt;
    
    /** 结束时间 */
    private LocalDateTime endedAt;
    
    /** 是否删除 */
    private Boolean deleted;
    
    /**
     * 转换状态
     */
    public void transitionTo(RtcSessionState newState) {
        this.state = newState;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 记录错误
     */
    public void recordError(String errorCode, String errorMessage) {
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
        this.state = RtcSessionState.ERROR;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 增加打断计数
     */
    public void incrementInterruptCount() {
        if (this.interruptCount == null) {
            this.interruptCount = 0;
        }
        this.interruptCount++;
        this.updatedAt = LocalDateTime.now();
    }
}

