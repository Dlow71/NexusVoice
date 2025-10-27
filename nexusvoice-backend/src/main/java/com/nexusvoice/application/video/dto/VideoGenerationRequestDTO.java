package com.nexusvoice.application.video.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 视频生成请求DTO
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Data
@Schema(description = "视频生成请求参数")
public class VideoGenerationRequestDTO {
    
    @Schema(description = "模型标识，格式：provider:model，如 zhipu:cogvideox-flash", 
            example = "zhipu:cogvideox-flash", 
            required = true)
    @NotBlank(message = "模型标识不能为空")
    private String modelKey;
    
    @Schema(description = "视频生成提示词", 
            example = "一只可爱的橘猫在阳光下打哈欠", 
            required = true)
    @NotBlank(message = "提示词不能为空")
    @Size(max = 1000, message = "提示词长度不能超过1000个字符")
    private String prompt;
    
    @Schema(description = "视频质量模式：quality（质量优先）、speed（速度优先）", 
            example = "speed", 
            allowableValues = {"quality", "speed"})
    private String quality = "speed";
    
    @Schema(description = "是否包含音频", 
            example = "false")
    private Boolean withAudio = false;
    
    @Schema(description = "是否添加水印", 
            example = "true")
    private Boolean watermarkEnabled = true;
    
    @Schema(description = "视频尺寸，如：1280x720、1920x1080、1024x1024等", 
            example = "1280x720")
    private String size = "1280x720";
    
    @Schema(description = "帧率（fps），支持30或60", 
            example = "30",
            allowableValues = {"30", "60"})
    @Min(value = 30, message = "帧率最小为30fps")
    @Max(value = 60, message = "帧率最大为60fps")
    private Integer fps = 30;
    
    @Schema(description = "视频时长（秒），支持5或10", 
            example = "5",
            allowableValues = {"5", "10"})
    @Min(value = 5, message = "视频时长最小为5秒")
    @Max(value = 10, message = "视频时长最大为10秒")
    private Integer duration = 5;
    
    @Schema(description = "图生视频：图片URL（单图模式）", 
            example = "https://cdn.example.com/image.jpg")
    private String imageUrl;
    
    @Schema(description = "首尾帧生成：图片URL列表（双图模式）", 
            example = "[\"https://cdn.example.com/frame1.jpg\", \"https://cdn.example.com/frame2.jpg\"]")
    private List<String> imageUrls;
    
    @Schema(description = "对话ID（用于统计）")
    private Long conversationId;
}
