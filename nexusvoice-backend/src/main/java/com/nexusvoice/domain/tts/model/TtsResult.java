package com.nexusvoice.domain.tts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TTS合成结果
 * 纯粹的领域模型，零基础设施依赖
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsResult {
    
    /**
     * 主音频URL（单段时为唯一URL，多段时为第一段URL）
     */
    private String audioUrl;
    
    /**
     * 音频格式（mp3/wav/pcm）
     */
    private String audioFormat;
    
    /**
     * 总音频大小（字节）
     */
    private Integer audioSize;
    
    /**
     * 音频总时长（秒）
     */
    private Double audioDuration;
    
    /**
     * 原始文本
     */
    private String text;
    
    /**
     * 使用的语音类型
     */
    private String voiceType;
    
    /**
     * 语速比例
     */
    private Double speedRatio;
    
    /**
     * 字符数（用于计费）
     */
    private Integer charCount;
    
    /**
     * 是否为分段合成
     */
    private Boolean chunked;
    
    /**
     * 分段批次ID（多段合成时有值）
     */
    private String groupId;
    
    /**
     * 分段列表（按index顺序播放）
     */
    private List<TtsSegment> segments;
    
    /**
     * 获取分段数量
     */
    public int getSegmentCount() {
        return segments != null ? segments.size() : 0;
    }
    
    /**
     * 是否为单段合成
     */
    public boolean isSingleSegment() {
        return !Boolean.TRUE.equals(chunked) || getSegmentCount() <= 1;
    }
    
    /**
     * 构建单段结果
     */
    public static TtsResult singleSegment(String audioUrl, String audioFormat, int audioSize, 
                                          String text, String voiceType, double speedRatio) {
        TtsSegment segment = TtsSegment.builder()
                .index(0)
                .text(text)
                .url(audioUrl)
                .size(audioSize)
                .charCount(text.length())
                .build();
        
        return TtsResult.builder()
                .audioUrl(audioUrl)
                .audioFormat(audioFormat)
                .audioSize(audioSize)
                .text(text)
                .voiceType(voiceType)
                .speedRatio(speedRatio)
                .charCount(text.length())
                .chunked(false)
                .segments(List.of(segment))
                .build();
    }
    
    /**
     * 构建多段结果
     */
    public static TtsResult multiSegments(List<TtsSegment> segments, String audioFormat,
                                          String text, String voiceType, double speedRatio, String groupId) {
        int totalSize = segments.stream().mapToInt(TtsSegment::getSize).sum();
        Double totalDuration = segments.stream()
                .map(TtsSegment::getDuration)
                .filter(d -> d != null)
                .reduce(0.0, Double::sum);
        
        return TtsResult.builder()
                .audioUrl(segments.isEmpty() ? null : segments.get(0).getUrl())
                .audioFormat(audioFormat)
                .audioSize(totalSize)
                .audioDuration(totalDuration > 0 ? totalDuration : null)
                .text(text)
                .voiceType(voiceType)
                .speedRatio(speedRatio)
                .charCount(text.length())
                .chunked(true)
                .groupId(groupId)
                .segments(segments)
                .build();
    }
}
