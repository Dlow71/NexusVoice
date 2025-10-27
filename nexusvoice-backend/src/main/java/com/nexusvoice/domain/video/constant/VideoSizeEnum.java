package com.nexusvoice.domain.video.constant;

import lombok.Getter;

/**
 * 视频尺寸枚举
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Getter
public enum VideoSizeEnum {
    
    /**
     * 1280x720（横屏16:9）
     */
    SIZE_1280x720("1280x720", 1280, 720, "横屏HD"),
    
    /**
     * 720x1280（竖屏9:16）
     */
    SIZE_720x1280("720x1280", 720, 1280, "竖屏HD"),
    
    /**
     * 1024x1024（方形）
     */
    SIZE_1024x1024("1024x1024", 1024, 1024, "方形"),
    
    /**
     * 1920x1080（横屏全高清）
     */
    SIZE_1920x1080("1920x1080", 1920, 1080, "横屏全高清"),
    
    /**
     * 1080x1920（竖屏全高清）
     */
    SIZE_1080x1920("1080x1920", 1080, 1920, "竖屏全高清"),
    
    /**
     * 2048x1080（超宽屏）
     */
    SIZE_2048x1080("2048x1080", 2048, 1080, "超宽屏"),
    
    /**
     * 3840x2160（4K）
     */
    SIZE_3840x2160("3840x2160", 3840, 2160, "4K超高清");
    
    private final String size;
    private final int width;
    private final int height;
    private final String description;
    
    VideoSizeEnum(String size, int width, int height, String description) {
        this.size = size;
        this.width = width;
        this.height = height;
        this.description = description;
    }
    
    /**
     * 根据尺寸字符串获取枚举
     */
    public static VideoSizeEnum fromSize(String size) {
        if (size == null) {
            return null;
        }
        
        for (VideoSizeEnum videoSize : values()) {
            if (videoSize.size.equalsIgnoreCase(size)) {
                return videoSize;
            }
        }
        
        return null;
    }
    
    /**
     * 是否为横屏
     */
    public boolean isLandscape() {
        return width > height;
    }
    
    /**
     * 是否为竖屏
     */
    public boolean isPortrait() {
        return height > width;
    }
    
    /**
     * 是否为方形
     */
    public boolean isSquare() {
        return width == height;
    }
}
