package com.nexusvoice.domain.rtc.enums;

import lombok.Getter;

/**
 * 打断模式枚举
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Getter
public enum InterruptMode {
    
    /** 软中断 - 停止TTS源+前端静音 */
    SOFT("软中断"),
    
    /** 硬中断 - 取消LLM+清空队列 */
    HARD("硬中断");

    private final String description;

    InterruptMode(String description) {
        this.description = description;
    }

    public static InterruptMode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (InterruptMode mode : values()) {
            if (mode.name().equals(code)) {
                return mode;
            }
        }
        return null;
    }
}

