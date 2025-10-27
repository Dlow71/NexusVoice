package com.nexusvoice.application.video.assembler;

import com.nexusvoice.application.video.dto.VideoGenerationRequestDTO;
import com.nexusvoice.application.video.dto.VideoGenerationResponseDTO;
import com.nexusvoice.domain.video.constant.VideoQualityEnum;
import com.nexusvoice.domain.video.constant.VideoSizeEnum;
import com.nexusvoice.domain.video.model.VideoGenerationRequest;
import com.nexusvoice.domain.video.model.VideoGenerationResult;
import org.springframework.stereotype.Component;

/**
 * 视频生成DTO与领域模型转换器
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Component
public class VideoGenerationAssembler {
    
    /**
     * DTO转领域请求模型
     * 
     * @param dto DTO对象
     * @param userId 用户ID
     * @return 领域请求模型
     */
    public VideoGenerationRequest toDomain(VideoGenerationRequestDTO dto, Long userId) {
        VideoGenerationRequest request = new VideoGenerationRequest();
        
        request.setModelKey(dto.getModelKey());
        request.setPrompt(dto.getPrompt());
        request.setUserId(userId);
        request.setConversationId(dto.getConversationId());
        
        // 质量模式
        if (dto.getQuality() != null) {
            VideoQualityEnum quality = VideoQualityEnum.fromCode(dto.getQuality());
            if (quality != null) {
                request.setQuality(quality);
            }
        }
        
        // 视频尺寸
        if (dto.getSize() != null) {
            VideoSizeEnum size = VideoSizeEnum.fromSize(dto.getSize());
            if (size != null) {
                request.setSize(size);
            }
        }
        
        // 其他参数
        request.setWithAudio(dto.getWithAudio());
        request.setWatermarkEnabled(dto.getWatermarkEnabled());
        request.setFps(dto.getFps());
        request.setDuration(dto.getDuration());
        
        // 图片输入
        request.setImageUrl(dto.getImageUrl());
        request.setImageUrls(dto.getImageUrls());
        
        return request;
    }
    
    /**
     * 领域结果模型转DTO
     * 
     * @param result 领域结果模型
     * @return DTO对象
     */
    public VideoGenerationResponseDTO toDTO(VideoGenerationResult result) {
        VideoGenerationResponseDTO.VideoGenerationResponseDTOBuilder builder = VideoGenerationResponseDTO.builder()
                .taskId(result.getTaskId())
                .taskStatus(result.getTaskStatus())
                .videoUrl(result.getVideoUrl())
                .coverImageUrl(result.getCoverImageUrl())
                .requestId(result.getRequestId())
                .modelName(result.getModelName())
                .errorMessage(result.getErrorMessage())
                .generationTime(result.getGenerationTime());
        
        // Token使用统计
        if (result.getTokenUsage() != null) {
            VideoGenerationResponseDTO.TokenUsageDTO tokenUsage = VideoGenerationResponseDTO.TokenUsageDTO.builder()
                    .promptTokens(result.getTokenUsage().getPromptTokens())
                    .completionTokens(result.getTokenUsage().getCompletionTokens())
                    .totalTokens(result.getTokenUsage().getTotalTokens())
                    .build();
            builder.tokenUsage(tokenUsage);
        }
        
        return builder.build();
    }
}
