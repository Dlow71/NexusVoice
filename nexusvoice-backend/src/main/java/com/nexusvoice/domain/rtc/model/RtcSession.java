package com.nexusvoice.domain.rtc.model;

import com.nexusvoice.domain.rtc.enums.RtcSessionState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RTC会话领域实体（纯POJO）
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Slf4j
@Data
public class RtcSession {
    
    // 状态转换白名单：当前状态 -> 允许转换到的状态列表
    private static final Map<RtcSessionState, List<RtcSessionState>> STATE_TRANSITIONS = new HashMap<>();
    
    static {
        // IDLE 只能转到 CONNECTING
        STATE_TRANSITIONS.put(RtcSessionState.IDLE, Arrays.asList(
            RtcSessionState.CONNECTING, RtcSessionState.ERROR, RtcSessionState.TERMINATED
        ));
        
        // CONNECTING 只能转到 LISTENING
        STATE_TRANSITIONS.put(RtcSessionState.CONNECTING, Arrays.asList(
            RtcSessionState.LISTENING, RtcSessionState.ERROR, RtcSessionState.TERMINATED
        ));
        
        // LISTENING 可以转到 RECOGNIZING
        STATE_TRANSITIONS.put(RtcSessionState.LISTENING, Arrays.asList(
            RtcSessionState.RECOGNIZING, RtcSessionState.ERROR, RtcSessionState.TERMINATED
        ));
        
        // RECOGNIZING 可以转到 THINKING 或回到 LISTENING
        STATE_TRANSITIONS.put(RtcSessionState.RECOGNIZING, Arrays.asList(
            RtcSessionState.THINKING, RtcSessionState.LISTENING, RtcSessionState.ERROR, RtcSessionState.TERMINATED
        ));
        
        // THINKING 可以转到 SPEAKING 或回到 LISTENING（LLM失败）
        STATE_TRANSITIONS.put(RtcSessionState.THINKING, Arrays.asList(
            RtcSessionState.SPEAKING, RtcSessionState.LISTENING, RtcSessionState.ERROR, RtcSessionState.TERMINATED
        ));
        
        // SPEAKING 可以转到 LISTENING（播放完成或打断）
        STATE_TRANSITIONS.put(RtcSessionState.SPEAKING, Arrays.asList(
            RtcSessionState.LISTENING, RtcSessionState.ERROR, RtcSessionState.TERMINATED
        ));
        
        // INTERRUPTED 已删除，改为直接回到 LISTENING
        
        // ERROR 只能转到 TERMINATED
        STATE_TRANSITIONS.put(RtcSessionState.ERROR, Arrays.asList(
            RtcSessionState.TERMINATED
        ));
        
        // TERMINATED 是终态，不能转移
        STATE_TRANSITIONS.put(RtcSessionState.TERMINATED, Arrays.asList());
    }
    
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
     * 转换状态（带状态机验证）
     */
    public void transitionTo(RtcSessionState newState) {
        // 状态机验证
        if (this.state != null) {
            List<RtcSessionState> allowedStates = STATE_TRANSITIONS.get(this.state);
            if (allowedStates == null || !allowedStates.contains(newState)) {
                log.warn("不允许的状态转换: {} -> {}, sessionId={}", 
                        this.state, newState, this.sessionId);
                // 记录错误但不阻止转换（MVP阶段容错）
            }
        }
        
        log.debug("状态转换: sessionId={}, {} -> {}", this.sessionId, this.state, newState);
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

