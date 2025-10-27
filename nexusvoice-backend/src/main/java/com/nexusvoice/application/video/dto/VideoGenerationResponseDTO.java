package com.nexusvoice.application.video.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频生成响应DTO
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频生成响应结果")
public class VideoGenerationResponseDTO {
    
    @Schema(description = "任务ID（用于查询结果）", example = "202510271234567890")
    private String taskId;
    
    @Schema(description = "任务状态：PROCESSING（处理中）、SUCCESS（成功）、FAIL（失败）", 
            example = "PROCESSING")
    private String taskStatus;
    
    @Schema(description = "视频CDN URL（成功时返回）", 
            example = "https://cdn.example.com/video_xxx.mp4")
    private String videoUrl;
    
    @Schema(description = "封面图CDN URL（成功时返回）", 
            example = "https://cdn.example.com/cover_xxx.jpg")
    private String coverImageUrl;
    
    @Schema(description = "请求ID（追踪用）", 
            example = "uuid-xxx")
    private String requestId;
    
    @Schema(description = "模型名称", 
            example = "CogVideoX-Flash")
    private String modelName;
    
    @Schema(description = "错误信息（失败时返回）")
    private String errorMessage;
    
    @Schema(description = "生成耗时（毫秒）", 
            example = "45000")
    private Long generationTime;
    
    @Schema(description = "Token使用统计")
    private TokenUsageDTO tokenUsage;
    
    /**
     * Token使用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Token使用统计")
    public static class TokenUsageDTO {
        
        @Schema(description = "输入Token数", example = "100")
        private Integer promptTokens;
        
        @Schema(description = "输出Token数", example = "0")
        private Integer completionTokens;
        
        @Schema(description = "总Token数", example = "100")
        private Integer totalTokens;
    }
}
