package com.nexusvoice.domain.rtc.enums;

import lombok.Getter;

/**
 * RTC会话状态枚举
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Getter
public enum RtcSessionState {
    
    /** 空闲 - 会话已创建但未连接 */
    IDLE("空闲"),
    
    /** 连接中 - WebRTC连接建立中 */
    CONNECTING("连接中"),
    
    /** 聆听中 - 等待用户说话 */
    LISTENING("聆听中"),
    
    /** 识别中 - ASR识别用户语音 */
    RECOGNIZING("识别中"),
    
    /** 思考中 - LLM生成响应 */
    THINKING("思考中"),
    
    /** 播放中 - TTS播放AI响应 */
    SPEAKING("播放中"),
    
    /** 已打断 - 用户打断AI播放 */
    INTERRUPTED("已打断"),
    
    /** 错误 - 会话出错 */
    ERROR("错误"),
    
    /** 已终止 - 会话正常结束 */
    TERMINATED("已终止");

    private final String description;

    RtcSessionState(String description) {
        this.description = description;
    }

    /**
     * 从字符串代码获取枚举
     */
    public static RtcSessionState fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (RtcSessionState state : values()) {
            if (state.name().equals(code)) {
                return state;
            }
        }
        return null;
    }
}

