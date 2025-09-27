package com.nexusvoice.application.conversation.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 对话列表DTO
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Data
@Builder
@Schema(description = "对话列表项")
public class ConversationListDto {

    @Schema(description = "对话ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "对话标题")
    private String title;

    @Schema(description = "使用的模型名称")
    private String modelName;

    @Schema(description = "对话状态")
    private String status;

    @Schema(description = "最后一条消息预览")
    private String lastMessage;

    @Schema(description = "消息数量")
    private Integer messageCount;

    @Schema(description = "最后活跃时间")
    private LocalDateTime lastActiveAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
