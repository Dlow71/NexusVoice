package com.nexusvoice.domain.video.constant;

import lombok.Getter;

/**
 * 视频生成质量枚举
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Getter
public enum VideoQualityEnum {
    
    /**
     * 质量优先：生成质量高
     */
    QUALITY("quality", "质量优先"),
    
    /**
     * 速度优先：生成时间更快，质量相对稍低
     */
    SPEED("speed", "速度优先");
    
    private final String code;
    private final String name;
    
    VideoQualityEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static VideoQualityEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (VideoQualityEnum quality : values()) {
            if (quality.code.equalsIgnoreCase(code)) {
                return quality;
            }
        }
        
        return null;
    }
}
