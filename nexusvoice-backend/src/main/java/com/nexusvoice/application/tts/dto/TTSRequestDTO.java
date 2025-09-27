package com.nexusvoice.application.tts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * TTS请求DTO
 * 
 * @author NexusVoice Team
 * @since 2025-09-24
 */
@Data
@Schema(description = "TTS文本转语音请求")
public class TTSRequestDTO {

    @Schema(description = "要转换的文本内容", example = "你好，这是一段测试文本", required = true)
    @NotBlank(message = "文本内容不能为空")
    @Size(max = 10000, message = "文本长度不能超过10000字符")
    private String text;

    @Schema(description = "语音类型", example = "qiniu_zh_female_wwxkjx")
    private String voiceType;

    @Schema(description = "音频编码格式", example = "mp3", allowableValues = {"mp3", "wav", "pcm"})
    private String encoding;

    @Schema(description = "语速比例", example = "1.0", minimum = "0.5", maximum = "2.0")
    @DecimalMin(value = "0.5", message = "语速比例不能小于0.5")
    @DecimalMax(value = "2.0", message = "语速比例不能大于2.0")
    private Double speedRatio;
}
