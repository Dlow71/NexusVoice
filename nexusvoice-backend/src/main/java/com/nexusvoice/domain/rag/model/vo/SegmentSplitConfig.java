package com.nexusvoice.domain.rag.model.vo;

import lombok.Getter;

/**
 * 分段配置值对象
 * 
 * @author NexusVoice
 * @since 2025-11-11
 */
@Getter
public class SegmentSplitConfig {
    
    private final int maxLength;
    private final int minLength;
    private final int overlapSize;
    
    /**
     * 构造函数
     */
    public SegmentSplitConfig(int maxLength, int minLength, int overlapSize) {
        this.maxLength = maxLength;
        this.minLength = minLength;
        this.overlapSize = overlapSize;
    }
    
    /**
     * 默认配置
     */
    public static SegmentSplitConfig defaultConfig() {
        return new SegmentSplitConfig(1800, 200, 100);
    }
    
    /**
     * 验证配置合法性
     */
    public boolean isValid() {
        return maxLength > minLength && minLength > 0 && overlapSize >= 0;
    }
    
    /**
     * 验证配置，不合法则抛出异常
     */
    public void validate() {
        if (maxLength <= minLength) {
            throw new IllegalArgumentException("最大长度必须大于最小长度");
        }
        if (minLength <= 0) {
            throw new IllegalArgumentException("最小长度必须大于0");
        }
        if (overlapSize < 0) {
            throw new IllegalArgumentException("重叠大小不能为负数");
        }
    }
}
