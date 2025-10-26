package com.nexusvoice.domain.tts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS分段信息
 * 纯粹的领域模型，用于表示TTS合成的单个音频片段
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsSegment {
    
    /**
     * 分段序号（从0开始）
     */
    private Integer index;
    
    /**
     * 该段文本内容
     */
    private String text;
    
    /**
     * 该段音频URL（CDN地址）
     */
    private String url;
    
    /**
     * 该段音频大小（字节）
     */
    private Integer size;
    
    /**
     * 该段音频时长（秒）
     */
    private Double duration;
    
    /**
     * 字符数（用于计费）
     */
    private Integer charCount;
}
