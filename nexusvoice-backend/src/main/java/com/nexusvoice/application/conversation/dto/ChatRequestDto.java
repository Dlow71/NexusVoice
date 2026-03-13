package com.nexusvoice.application.conversation.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 聊天请求DTO
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Data
@Schema(description = "聊天请求")
public class ChatRequestDto {

    @Schema(description = "对话ID，如果为空则创建新对话", example = "1")
    private Long conversationId;

    @Schema(description = "用户消息内容", example = "你好，请介绍一下自己")
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 4000, message = "消息内容不能超过4000字符")
    private String message;

    @Schema(description = "模型名称", example = "gpt-4o-mini")
    private String modelName;

    @Schema(description = "温度参数", example = "0.7")
    private Double temperature;

    @Schema(description = "最大令牌数", example = "2000")
    private Integer maxTokens;

    @Schema(description = "系统提示词", example = "你是一个有用的AI助手")
    private String systemPrompt;

    @Schema(description = "对话标题", example = "关于AI的讨论")
    private String title;

    @Schema(description = "是否启用联网搜索", example = "false")
    private Boolean enableWebSearch = false;

    @Schema(description = "是否启用语音合成，生成audioUrl", example = "false")
    private Boolean enableAudio = false;

    @Schema(description = "角色ID，指定AI扮演的角色", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roleId;
    
    @Schema(description = "是否启用RAG（知识库检索增强）", example = "false")
    private Boolean enableRag = false;
    
    @Schema(description = "知识库ID列表，启用RAG时使用", example = "[1, 2, 3]")
    private java.util.List<Long> knowledgeBaseIds;

    @Schema(description = "RAG回答约束模式：STRICT=严格基于知识库资料，FLEXIBLE=允许在资料基础上适度发挥", example = "STRICT")
    private String ragGroundingMode = "STRICT";
    
    @Schema(description = "图像URL列表，支持多图输入（仅OCR/视觉模型支持）", example = "[\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"]")
    private java.util.List<String> imageUrls;
    
    @Schema(description = "Base64编码的图像数据，单图输入（优先级低于imageUrls）", example = "data:image/jpeg;base64,/9j/4AAQSkZJRg...")
    private String imageBase64;
    
    @Schema(description = "附件URL列表，JSON字符串数组，每个JSON包含{type,url,name,size,mimeType}等字段", 
            example = "[{\"type\":\"image\",\"url\":\"https://cdn.example.com/img.jpg\",\"name\":\"screenshot.jpg\",\"size\":102400}]")
    private java.util.List<String> attachmentUrls;
}
