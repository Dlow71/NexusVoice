package com.nexusvoice.domain.conversation.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话运行策略。
 * 表达单个会话在生成参数、上下文管理和思考模式上的长期设置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRuntimePolicy {

    public static final String STRATEGY_AUTO = "AUTO";
    public static final String STRATEGY_WINDOW_ONLY = "WINDOW_ONLY";
    public static final String STRATEGY_COMPACT = "COMPACT";

    private Double temperature;

    private Integer maxTokens;

    private Double topP;

    private Double frequencyPenalty;

    private Double presencePenalty;

    private String thinkingMode;

    private Boolean showThinking;

    private Integer thinkingBudgetTokens;

    private String reasoningEffort;

    /**
     * 上下文管理策略：AUTO / WINDOW_ONLY / COMPACT
     */
    private String contextStrategy;

    /**
     * 保留的最近对话轮数。
     */
    private Integer recentTurnsToKeep;

    /**
     * 为回答输出保留的token预算。
     */
    private Integer reservedOutputTokens;

    /**
     * 自动触发compact的阈值比例，取值区间 0.35 ~ 0.95。
     */
    private Double compactTriggerRatio;

    /**
     * 压缩记忆。
     */
    private ConversationCompactMemory compactMemory;

    public static ConversationRuntimePolicy defaults() {
        return ConversationRuntimePolicy.builder()
                .temperature(0.7)
                .maxTokens(2000)
                .topP(1.0)
                .frequencyPenalty(0.0)
                .presencePenalty(0.0)
                .thinkingMode("disabled")
                .showThinking(false)
                .reasoningEffort("medium")
                .contextStrategy(STRATEGY_AUTO)
                .recentTurnsToKeep(8)
                .reservedOutputTokens(2000)
                .compactTriggerRatio(0.72)
                .compactMemory(ConversationCompactMemory.builder().build())
                .build();
    }
}
