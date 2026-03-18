package com.nexusvoice.application.voice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建语音会话请求。
 */
@Data
@Schema(description = "创建语音会话请求")
public class VoiceSessionStartRequest {

    @Schema(description = "对话ID，可选，为空时创建新对话")
    private Long conversationId;

    @Schema(description = "角色ID，可选")
    private Long roleId;

    @Schema(description = "模型名称", example = "qiniu:deepseek-v3.2-thinking")
    private String modelName;

    @Schema(description = "语音类型，可选", example = "qiniu_zh_female_wwxkjx")
    private String voiceType;

    @Schema(description = "ASR模型键", example = "siliconflow:telespeech-asr")
    private String asrModelKey;

    @Schema(description = "知识库ID列表")
    private List<Long> knowledgeBaseIds;

    @Schema(description = "对话标题，可选")
    @Size(max = 255)
    private String title;

    @Schema(description = "严格模式", example = "true")
    private Boolean strictMode = true;

    @Schema(description = "是否启用RAG", example = "true")
    private Boolean ragEnabled = false;

    @Schema(description = "思考模式", example = "disabled")
    private String thinkingMode;

    @Schema(description = "是否展示思考过程")
    private Boolean showThinking;

    @Schema(description = "上下文策略", example = "COMPACT")
    private String contextStrategy;

    @Schema(description = "温度参数", example = "0.7")
    private Double temperature;
}
