package com.nexusvoice.domain.rtc.enums;

import lombok.Getter;

/**
 * 信令消息类型枚举
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Getter
public enum SignalMessageType {
    
    /** SDP Offer */
    OFFER("SDP Offer"),
    
    /** SDP Answer */
    ANSWER("SDP Answer"),
    
    /** ICE候选者 */
    ICE_CANDIDATE("ICE候选者"),
    
    /** 打断信号 */
    INTERRUPT("打断信号"),
    
    /** 状态更新 */
    STATE_UPDATE("状态更新"),
    
    /** 背压信号 */
    BACKPRESSURE("背压信号"),
    
    /** 心跳 */
    HEARTBEAT("心跳"),
    
    /** 错误信号 */
    ERROR("错误信号"),
    
    /** 会话创建 */
    SESSION_CREATED("会话创建"),
    
    /** 会话终止 */
    SESSION_TERMINATED("会话终止");

    private final String description;

    SignalMessageType(String description) {
        this.description = description;
    }

    public static SignalMessageType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SignalMessageType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        return null;
    }
}

