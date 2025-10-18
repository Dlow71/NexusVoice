package com.nexusvoice.domain.mq.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * RocketMQ延迟级别枚举
 * 预设18个延迟级别
 * 
 * @author Dlow
 * @date 2025/10/18
 */
@Getter
@AllArgsConstructor
public enum DelayLevelEnum {
    
    /**
     * 1秒
     */
    DELAY_1S(1, "1s", "1秒"),
    
    /**
     * 5秒
     */
    DELAY_5S(2, "5s", "5秒"),
    
    /**
     * 10秒
     */
    DELAY_10S(3, "10s", "10秒"),
    
    /**
     * 30秒
     */
    DELAY_30S(4, "30s", "30秒"),
    
    /**
     * 1分钟
     */
    DELAY_1M(5, "1m", "1分钟"),
    
    /**
     * 2分钟
     */
    DELAY_2M(6, "2m", "2分钟"),
    
    /**
     * 3分钟
     */
    DELAY_3M(7, "3m", "3分钟"),
    
    /**
     * 4分钟
     */
    DELAY_4M(8, "4m", "4分钟"),
    
    /**
     * 5分钟
     */
    DELAY_5M(9, "5m", "5分钟"),
    
    /**
     * 6分钟
     */
    DELAY_6M(10, "6m", "6分钟"),
    
    /**
     * 7分钟
     */
    DELAY_7M(11, "7m", "7分钟"),
    
    /**
     * 8分钟
     */
    DELAY_8M(12, "8m", "8分钟"),
    
    /**
     * 9分钟
     */
    DELAY_9M(13, "9m", "9分钟"),
    
    /**
     * 10分钟
     */
    DELAY_10M(14, "10m", "10分钟"),
    
    /**
     * 20分钟
     */
    DELAY_20M(15, "20m", "20分钟"),
    
    /**
     * 30分钟
     */
    DELAY_30M(16, "30m", "30分钟"),
    
    /**
     * 1小时
     */
    DELAY_1H(17, "1h", "1小时"),
    
    /**
     * 2小时
     */
    DELAY_2H(18, "2h", "2小时");
    
    /**
     * 延迟级别
     */
    private final Integer level;
    
    /**
     * 延迟时间
     */
    private final String time;
    
    /**
     * 描述
     */
    private final String desc;
    
    /**
     * 根据秒数获取最接近的延迟级别
     */
    public static DelayLevelEnum getBySeconds(int seconds) {
        if (seconds <= 1) return DELAY_1S;
        if (seconds <= 5) return DELAY_5S;
        if (seconds <= 10) return DELAY_10S;
        if (seconds <= 30) return DELAY_30S;
        if (seconds <= 60) return DELAY_1M;
        if (seconds <= 120) return DELAY_2M;
        if (seconds <= 180) return DELAY_3M;
        if (seconds <= 240) return DELAY_4M;
        if (seconds <= 300) return DELAY_5M;
        if (seconds <= 360) return DELAY_6M;
        if (seconds <= 420) return DELAY_7M;
        if (seconds <= 480) return DELAY_8M;
        if (seconds <= 540) return DELAY_9M;
        if (seconds <= 600) return DELAY_10M;
        if (seconds <= 1200) return DELAY_20M;
        if (seconds <= 1800) return DELAY_30M;
        if (seconds <= 3600) return DELAY_1H;
        return DELAY_2H;
    }
    
    /**
     * 根据分钟数获取最接近的延迟级别
     */
    public static DelayLevelEnum getByMinutes(int minutes) {
        return getBySeconds(minutes * 60);
    }
}
