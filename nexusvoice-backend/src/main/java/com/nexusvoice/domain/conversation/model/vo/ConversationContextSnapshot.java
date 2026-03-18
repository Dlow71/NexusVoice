package com.nexusvoice.domain.conversation.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单次上下文规划后的快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContextSnapshot {

    private String modelKey;

    private Integer modelContextWindow;

    private Integer estimatedInputTokens;

    private Integer reservedOutputTokens;

    private Integer reservedThinkingTokens;

    private Integer reservedRagTokens;

    private Integer reservedSearchTokens;

    private Integer remainingTokens;

    private Integer systemPromptTokens;

    private Integer compactSummaryTokens;

    private Integer historyTokens;

    private Integer totalHistoryMessages;

    private Integer includedHistoryMessages;

    private Integer compactedMessages;

    private Boolean usedCompactSummary;

    private Boolean compactSummaryUpdated;

    private Boolean needsCompaction;

    private String appliedContextStrategy;
}
