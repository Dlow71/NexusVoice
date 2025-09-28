package com.nexusvoice.application.tts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * TTS响应DTO
 * 
 * @author NexusVoice Team
 * @since 2025-09-24
 */
@Data
@Schema(description = "TTS文本转语音响应")
public class TTSResponseDTO {

    @Schema(description = "音频文件URL", example = "https://cdn.example.com/audio/tts_audio_1695552000000.mp3")
    private String audioData;

    @Schema(description = "音频格式", example = "mp3")
    private String audioFormat;

    @Schema(description = "音频大小(字节)", example = "12345")
    private Integer audioSize;

    @Schema(description = "原始文本", example = "你好，这是一段测试文本")
    private String text;

    @Schema(description = "使用的语音类型", example = "qiniu_zh_female_wwxkjx")
    private String voiceType;

    @Schema(description = "语速比例", example = "1.0")
    private Double speedRatio;

    @Schema(description = "音频时长(秒)", example = "5.2")
    private Double duration;

    @Schema(description = "是否为分段合成")
    private Boolean chunked;

    @Schema(description = "分段批次ID")
    private String groupId;

    @Schema(description = "分段列表，顺序播放")
    private java.util.List<Segment> segments;

    @Data
    @Schema(description = "TTS分段信息")
    public static class Segment {
        @Schema(description = "分段序号，从0开始", example = "0")
        private Integer index;

        @Schema(description = "该段文本")
        private String text;

        @Schema(description = "该段音频URL")
        private String url;

        @Schema(description = "该段音频大小(字节)")
        private Integer size;

        @Schema(description = "该段时长(秒)")
        private Double duration;
    }
}
