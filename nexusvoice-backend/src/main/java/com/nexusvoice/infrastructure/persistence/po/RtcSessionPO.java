package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * RTC会话持久化对象
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rtc_sessions")
public class RtcSessionPO extends BasePO {
    
    @TableField("session_id")
    private String sessionId;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("role_id")
    private Long roleId;
    
    @TableField("conversation_id")
    private Long conversationId;
    
    @TableField("state")
    private String state;
    
    @TableField("local_sdp")
    private String localSdp;
    
    @TableField("remote_sdp")
    private String remoteSdp;
    
    @TableField("kms_pipeline_id")
    private String kmsPipelineId;
    
    @TableField("kms_webrtc_endpoint_id")
    private String kmsWebrtcEndpointId;
    
    @TableField("grpc_asr_session_id")
    private String grpcAsrSessionId;
    
    @TableField("grpc_tts_session_id")
    private String grpcTtsSessionId;
    
    @TableField("current_asr_text")
    private String currentAsrText;
    
    @TableField("current_tts_segment_id")
    private Integer currentTtsSegmentId;
    
    @TableField("interrupt_count")
    private Integer interruptCount;
    
    @TableField("last_error_code")
    private String lastErrorCode;
    
    @TableField("last_error_message")
    private String lastErrorMessage;
    
    @TableField("started_at")
    private LocalDateTime startedAt;
    
    @TableField("ended_at")
    private LocalDateTime endedAt;
}

