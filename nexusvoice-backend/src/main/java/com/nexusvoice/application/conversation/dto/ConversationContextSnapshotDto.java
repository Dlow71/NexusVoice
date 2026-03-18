package com.nexusvoice.application.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上下文快照DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "上下文占用快照")
public class ConversationContextSnapshotDto {

    @Schema(description = "模型键", example = "qiniu:deepseek/deepseek-v3.2-251201")
    private String modelKey;

    @Schema(description = "模型上下文窗口", example = "64000")
    private Integer modelContextWindow;

    @Schema(description = "预计输入tokens", example = "4820")
    private Integer estimatedInputTokens;

    @Schema(description = "预留输出tokens", example = "2000")
    private Integer reservedOutputTokens;

    @Schema(description = "预留思考tokens", example = "1024")
    private Integer reservedThinkingTokens;

    @Schema(description = "预留RAG tokens", example = "2400")
    private Integer reservedRagTokens;

    @Schema(description = "预留搜索tokens", example = "1200")
    private Integer reservedSearchTokens;

    @Schema(description = "剩余tokens", example = "53556")
    private Integer remainingTokens;

    @Schema(description = "系统提示词tokens", example = "190")
    private Integer systemPromptTokens;

    @Schema(description = "compact摘要tokens", example = "320")
    private Integer compactSummaryTokens;

    @Schema(description = "历史消息tokens", example = "4310")
    private Integer historyTokens;

    @Schema(description = "历史消息总数", example = "28")
    private Integer totalHistoryMessages;

    @Schema(description = "实际纳入上下文的历史消息数", example = "10")
    private Integer includedHistoryMessages;

    @Schema(description = "被compact折叠的消息数", example = "18")
    private Integer compactedMessages;

    @Schema(description = "本次是否使用了compact摘要", example = "true")
    private Boolean usedCompactSummary;

    @Schema(description = "本次是否更新了compact摘要", example = "false")
    private Boolean compactSummaryUpdated;

    @Schema(description = "当前是否建议执行compact", example = "true")
    private Boolean needsCompaction;

    @Schema(description = "实际应用的上下文策略", example = "COMPACT")
    private String appliedContextStrategy;
}
