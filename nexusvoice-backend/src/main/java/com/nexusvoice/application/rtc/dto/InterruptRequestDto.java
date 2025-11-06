package com.nexusvoice.application.rtc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 打断请求DTO
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Data
@Schema(description = "打断请求")
public class InterruptRequestDto {
    
    @NotNull(message = "打断模式不能为空")
    @Schema(description = "打断模式（SOFT/HARD）", example = "SOFT")
    private String mode;
    
    @Schema(description = "打断原因", example = "user_barge_in")
    private String reason;
}







