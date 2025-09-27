package com.nexusvoice.application.conversation.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建对话请求
 */
@Data
@Schema(description = "创建对话请求")
public class ConversationCreateRequest {

    @Schema(description = "对话标题（可选）", example = "关于AI的讨论")
    @Size(max = 255, message = "标题不能超过255字符")
    private String title;

    @Schema(description = "模型名称（可选）", example = "gpt-4o-mini")
    private String modelName;

    @Schema(description = "系统提示词（可选）", example = "你是一个有用的AI助手")
    private String systemPrompt;

    @Schema(description = "角色ID（可选，用于为会话绑定一个角色）", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roleId;
}
