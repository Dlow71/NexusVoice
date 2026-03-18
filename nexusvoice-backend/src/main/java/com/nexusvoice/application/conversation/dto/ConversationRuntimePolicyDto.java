package com.nexusvoice.application.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话运行策略DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话运行策略")
public class ConversationRuntimePolicyDto {

    @Schema(description = "温度参数", example = "0.7")
    private Double temperature;

    @Schema(description = "最大输出tokens", example = "2000")
    private Integer maxTokens;

    @Schema(description = "Top P", example = "1.0")
    private Double topP;

    @Schema(description = "频率惩罚", example = "0.0")
    private Double frequencyPenalty;

    @Schema(description = "存在惩罚", example = "0.0")
    private Double presencePenalty;

    @Schema(description = "思考模式", example = "disabled")
    private String thinkingMode;

    @Schema(description = "是否展示思考过程", example = "false")
    private Boolean showThinking;

    @Schema(description = "思考预算tokens", example = "1024")
    private Integer thinkingBudgetTokens;

    @Schema(description = "推理强度", example = "medium")
    private String reasoningEffort;

    @Schema(description = "上下文策略：AUTO / WINDOW_ONLY / COMPACT", example = "AUTO")
    private String contextStrategy;

    @Schema(description = "保留最近对话轮数", example = "8")
    private Integer recentTurnsToKeep;

    @Schema(description = "预留输出tokens", example = "2000")
    private Integer reservedOutputTokens;

    @Schema(description = "compact触发阈值比例", example = "0.72")
    private Double compactTriggerRatio;
}
