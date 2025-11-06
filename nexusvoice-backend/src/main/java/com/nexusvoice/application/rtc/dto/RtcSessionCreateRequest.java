package com.nexusvoice.application.rtc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * RTC会话创建请求DTO
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Data
@Schema(description = "RTC会话创建请求")
public class RtcSessionCreateRequest {
    
    @Schema(description = "角色ID（可选）")
    private Long roleId;
    
    @Schema(description = "关联对话ID（可选）")
    private Long conversationId;
    
    @Schema(description = "模型名称（如：openai:gpt-4o）", example = "openai:gpt-4o")
    private String modelName;
}







