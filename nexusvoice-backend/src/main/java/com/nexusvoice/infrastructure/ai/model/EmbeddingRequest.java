package com.nexusvoice.infrastructure.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 向量化请求
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {
    
    /**
     * 待向量化的文本（单个文本）
     */
    private String text;
    
    /**
     * 待向量化的文本列表（批量）
     */
    private List<String> texts;
    
    /**
     * 用户ID（用于日志记录）
     */
    private Long userId;
    
    /**
     * 业务标识（用于日志记录）
     */
    private String bizId;
    
    /**
     * 向量维度（可选，使用模型默认值）
     */
    private Integer dimensions;
    
    /**
     * 是否单个文本请求
     */
    public boolean isSingleText() {
        return text != null && (texts == null || texts.isEmpty());
    }
    
    /**
     * 获取实际的文本列表
     */
    public List<String> getActualTexts() {
        if (isSingleText()) {
            return List.of(text);
        }
        return texts;
    }
}
