package com.nexusvoice.infrastructure.conversation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.application.conversation.dto.ChatRequestDto;
import com.nexusvoice.application.conversation.dto.ConversationContextSnapshotDto;
import com.nexusvoice.application.conversation.dto.ConversationRuntimeConfigDto;
import com.nexusvoice.application.conversation.dto.ConversationRuntimePolicyDto;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.conversation.model.Conversation;
import com.nexusvoice.domain.conversation.model.vo.ConversationCompactMemory;
import com.nexusvoice.domain.conversation.model.vo.ConversationContextSnapshot;
import com.nexusvoice.domain.conversation.model.vo.ConversationRuntimePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 会话运行配置读写服务。
 * 负责将会话级高级设置持久化到 conversations.config_params 中。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationRuntimeConfigService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final Set<String> THINKING_MODES = Set.of("disabled", "auto", "enabled");
    private static final Set<String> REASONING_EFFORTS = Set.of("minimal", "low", "medium", "high", "none");

    private final ObjectMapper objectMapper;

    public ConversationRuntimePolicy readPolicy(Conversation conversation, AiModel model) {
        return sanitizePolicy(readPolicy(conversation != null ? conversation.getConfigParams() : null), model);
    }

    public ConversationRuntimePolicy readPolicy(String configParams) {
        ConversationRuntimePolicy defaults = ConversationRuntimePolicy.defaults();
        Map<String, Object> root = parseRoot(configParams);
        Map<String, Object> runtimePolicyMap = extractNestedMap(root, "runtimePolicy");
        if (runtimePolicyMap.isEmpty()) {
            runtimePolicyMap = root;
        }

        ConversationRuntimePolicy parsed = convert(runtimePolicyMap, ConversationRuntimePolicy.class);
        ConversationCompactMemory compactMemory = convert(extractNestedMap(root, "compactMemory"), ConversationCompactMemory.class);
        if (compactMemory == null) {
            compactMemory = convert(extractNestedMap(runtimePolicyMap, "compactMemory"), ConversationCompactMemory.class);
        }

        ConversationRuntimePolicy merged = defaults();
        if (parsed != null) {
            mergePolicyInto(merged, parsed);
        }
        merged.setCompactMemory(compactMemory != null ? compactMemory : ConversationCompactMemory.builder().build());
        return merged;
    }

    public ConversationRuntimePolicy mergeRequestOverrides(ConversationRuntimePolicy basePolicy,
                                                           ChatRequestDto requestDto,
                                                           AiModel model) {
        ConversationRuntimePolicy merged = copyOf(basePolicy);
        if (requestDto == null) {
            return sanitizePolicy(merged, model);
        }

        if (requestDto.getTemperature() != null) {
            merged.setTemperature(requestDto.getTemperature());
        }
        if (requestDto.getMaxTokens() != null) {
            merged.setMaxTokens(requestDto.getMaxTokens());
        }
        if (requestDto.getTopP() != null) {
            merged.setTopP(requestDto.getTopP());
        }
        if (requestDto.getFrequencyPenalty() != null) {
            merged.setFrequencyPenalty(requestDto.getFrequencyPenalty());
        }
        if (requestDto.getPresencePenalty() != null) {
            merged.setPresencePenalty(requestDto.getPresencePenalty());
        }
        if (requestDto.getThinkingMode() != null && !requestDto.getThinkingMode().isBlank()) {
            merged.setThinkingMode(requestDto.getThinkingMode());
        }
        if (requestDto.getShowThinking() != null) {
            merged.setShowThinking(requestDto.getShowThinking());
        }
        if (requestDto.getThinkingBudgetTokens() != null) {
            merged.setThinkingBudgetTokens(requestDto.getThinkingBudgetTokens());
        }
        if (requestDto.getReasoningEffort() != null && !requestDto.getReasoningEffort().isBlank()) {
            merged.setReasoningEffort(requestDto.getReasoningEffort());
        }
        if (requestDto.getContextStrategy() != null && !requestDto.getContextStrategy().isBlank()) {
            merged.setContextStrategy(requestDto.getContextStrategy());
        }
        if (requestDto.getRecentTurnsToKeep() != null) {
            merged.setRecentTurnsToKeep(requestDto.getRecentTurnsToKeep());
        }
        if (requestDto.getReservedOutputTokens() != null) {
            merged.setReservedOutputTokens(requestDto.getReservedOutputTokens());
        }
        if (requestDto.getCompactTriggerRatio() != null) {
            merged.setCompactTriggerRatio(requestDto.getCompactTriggerRatio());
        }
        return sanitizePolicy(merged, model);
    }

    public ConversationRuntimePolicy mergePolicyUpdate(ConversationRuntimePolicy basePolicy,
                                                       ConversationRuntimePolicyDto requestDto,
                                                       AiModel model) {
        ConversationRuntimePolicy merged = copyOf(basePolicy);
        if (requestDto == null) {
            return sanitizePolicy(merged, model);
        }

        if (requestDto.getTemperature() != null) {
            merged.setTemperature(requestDto.getTemperature());
        }
        if (requestDto.getMaxTokens() != null) {
            merged.setMaxTokens(requestDto.getMaxTokens());
        }
        if (requestDto.getTopP() != null) {
            merged.setTopP(requestDto.getTopP());
        }
        if (requestDto.getFrequencyPenalty() != null) {
            merged.setFrequencyPenalty(requestDto.getFrequencyPenalty());
        }
        if (requestDto.getPresencePenalty() != null) {
            merged.setPresencePenalty(requestDto.getPresencePenalty());
        }
        if (requestDto.getThinkingMode() != null) {
            merged.setThinkingMode(requestDto.getThinkingMode());
        }
        if (requestDto.getShowThinking() != null) {
            merged.setShowThinking(requestDto.getShowThinking());
        }
        if (requestDto.getThinkingBudgetTokens() != null) {
            merged.setThinkingBudgetTokens(requestDto.getThinkingBudgetTokens());
        }
        if (requestDto.getReasoningEffort() != null) {
            merged.setReasoningEffort(requestDto.getReasoningEffort());
        }
        if (requestDto.getContextStrategy() != null) {
            merged.setContextStrategy(requestDto.getContextStrategy());
        }
        if (requestDto.getRecentTurnsToKeep() != null) {
            merged.setRecentTurnsToKeep(requestDto.getRecentTurnsToKeep());
        }
        if (requestDto.getReservedOutputTokens() != null) {
            merged.setReservedOutputTokens(requestDto.getReservedOutputTokens());
        }
        if (requestDto.getCompactTriggerRatio() != null) {
            merged.setCompactTriggerRatio(requestDto.getCompactTriggerRatio());
        }
        return sanitizePolicy(merged, model);
    }

    public boolean applyPolicy(Conversation conversation, ConversationRuntimePolicy policy) {
        if (conversation == null || policy == null) {
            return false;
        }
        String oldConfig = conversation.getConfigParams();
        String newConfig = writeConfig(oldConfig, policy);
        if (newConfig.equals(oldConfig)) {
            return false;
        }
        conversation.setConfigParams(newConfig);
        return true;
    }

    public ConversationRuntimeConfigDto toDto(ConversationRuntimePolicy policy, ConversationContextSnapshot snapshot) {
        return ConversationRuntimeConfigDto.builder()
                .policy(toDto(policy))
                .contextSnapshot(toDto(snapshot))
                .build();
    }

    public ConversationRuntimePolicyDto toDto(ConversationRuntimePolicy policy) {
        ConversationRuntimePolicy source = policy != null ? policy : ConversationRuntimePolicy.defaults();
        return ConversationRuntimePolicyDto.builder()
                .temperature(source.getTemperature())
                .maxTokens(source.getMaxTokens())
                .topP(source.getTopP())
                .frequencyPenalty(source.getFrequencyPenalty())
                .presencePenalty(source.getPresencePenalty())
                .thinkingMode(source.getThinkingMode())
                .showThinking(source.getShowThinking())
                .thinkingBudgetTokens(source.getThinkingBudgetTokens())
                .reasoningEffort(source.getReasoningEffort())
                .contextStrategy(source.getContextStrategy())
                .recentTurnsToKeep(source.getRecentTurnsToKeep())
                .reservedOutputTokens(source.getReservedOutputTokens())
                .compactTriggerRatio(source.getCompactTriggerRatio())
                .build();
    }

    public ConversationContextSnapshotDto toDto(ConversationContextSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return ConversationContextSnapshotDto.builder()
                .modelKey(snapshot.getModelKey())
                .modelContextWindow(snapshot.getModelContextWindow())
                .estimatedInputTokens(snapshot.getEstimatedInputTokens())
                .reservedOutputTokens(snapshot.getReservedOutputTokens())
                .reservedThinkingTokens(snapshot.getReservedThinkingTokens())
                .reservedRagTokens(snapshot.getReservedRagTokens())
                .reservedSearchTokens(snapshot.getReservedSearchTokens())
                .remainingTokens(snapshot.getRemainingTokens())
                .systemPromptTokens(snapshot.getSystemPromptTokens())
                .compactSummaryTokens(snapshot.getCompactSummaryTokens())
                .historyTokens(snapshot.getHistoryTokens())
                .totalHistoryMessages(snapshot.getTotalHistoryMessages())
                .includedHistoryMessages(snapshot.getIncludedHistoryMessages())
                .compactedMessages(snapshot.getCompactedMessages())
                .usedCompactSummary(snapshot.getUsedCompactSummary())
                .compactSummaryUpdated(snapshot.getCompactSummaryUpdated())
                .needsCompaction(snapshot.getNeedsCompaction())
                .appliedContextStrategy(snapshot.getAppliedContextStrategy())
                .build();
    }

    public ConversationRuntimePolicy copyOf(ConversationRuntimePolicy policy) {
        ConversationRuntimePolicy source = policy != null ? policy : ConversationRuntimePolicy.defaults();
        return ConversationRuntimePolicy.builder()
                .temperature(source.getTemperature())
                .maxTokens(source.getMaxTokens())
                .topP(source.getTopP())
                .frequencyPenalty(source.getFrequencyPenalty())
                .presencePenalty(source.getPresencePenalty())
                .thinkingMode(source.getThinkingMode())
                .showThinking(source.getShowThinking())
                .thinkingBudgetTokens(source.getThinkingBudgetTokens())
                .reasoningEffort(source.getReasoningEffort())
                .contextStrategy(source.getContextStrategy())
                .recentTurnsToKeep(source.getRecentTurnsToKeep())
                .reservedOutputTokens(source.getReservedOutputTokens())
                .compactTriggerRatio(source.getCompactTriggerRatio())
                .compactMemory(source.getCompactMemory() != null
                        ? ConversationCompactMemory.builder()
                                .summary(source.getCompactMemory().getSummary())
                                .summaryUntilSequence(source.getCompactMemory().getSummaryUntilSequence())
                                .sourceMessageCount(source.getCompactMemory().getSourceMessageCount())
                                .estimatedTokens(source.getCompactMemory().getEstimatedTokens())
                                .modelKey(source.getCompactMemory().getModelKey())
                                .updatedAt(source.getCompactMemory().getUpdatedAt())
                                .build()
                        : ConversationCompactMemory.builder().build())
                .build();
    }

    public ConversationRuntimePolicy sanitizePolicy(ConversationRuntimePolicy policy, AiModel model) {
        ConversationRuntimePolicy sanitized = copyOf(policy != null ? policy : ConversationRuntimePolicy.defaults());

        sanitized.setTemperature(clampDouble(sanitized.getTemperature(), 0D, 2D, 0.7));
        sanitized.setMaxTokens(clampInt(sanitized.getMaxTokens(), 128, resolveMaxTokensUpperBound(model), 2000));
        sanitized.setTopP(clampDouble(sanitized.getTopP(), 0D, 1D, 1.0));
        sanitized.setFrequencyPenalty(clampDouble(sanitized.getFrequencyPenalty(), -2D, 2D, 0.0));
        sanitized.setPresencePenalty(clampDouble(sanitized.getPresencePenalty(), -2D, 2D, 0.0));
        sanitized.setThinkingMode(normalizeThinkingMode(sanitized.getThinkingMode()));
        sanitized.setShowThinking(Boolean.TRUE.equals(sanitized.getShowThinking()));
        sanitized.setThinkingBudgetTokens(sanitized.getThinkingBudgetTokens() == null
                ? null
                : clampInt(sanitized.getThinkingBudgetTokens(), 256, resolveThinkingBudgetUpperBound(model), 1024));
        sanitized.setReasoningEffort(normalizeReasoningEffort(sanitized.getReasoningEffort()));
        sanitized.setContextStrategy(normalizeContextStrategy(sanitized.getContextStrategy()));
        sanitized.setRecentTurnsToKeep(clampInt(sanitized.getRecentTurnsToKeep(), 2, 20, 8));
        sanitized.setReservedOutputTokens(clampInt(
                sanitized.getReservedOutputTokens(),
                256,
                resolveMaxTokensUpperBound(model),
                sanitized.getMaxTokens()
        ));
        sanitized.setCompactTriggerRatio(clampDouble(sanitized.getCompactTriggerRatio(), 0.35D, 0.95D, 0.72D));

        if (sanitized.getCompactMemory() == null) {
            sanitized.setCompactMemory(ConversationCompactMemory.builder().build());
        } else if (sanitized.getCompactMemory().getEstimatedTokens() != null) {
            sanitized.getCompactMemory().setEstimatedTokens(
                    Math.max(0, sanitized.getCompactMemory().getEstimatedTokens())
            );
        }
        return sanitized;
    }

    private ConversationRuntimePolicy defaults() {
        return ConversationRuntimePolicy.defaults();
    }

    private String writeConfig(String oldConfigParams, ConversationRuntimePolicy policy) {
        Map<String, Object> root = parseRoot(oldConfigParams);
        Map<String, Object> runtimePolicyMap = new LinkedHashMap<>(objectToMap(policy));
        runtimePolicyMap.remove("compactMemory");

        root.put("runtimePolicy", runtimePolicyMap);
        root.put("compactMemory", policy.getCompactMemory() != null ? policy.getCompactMemory() : new ConversationCompactMemory());

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("写入会话运行配置失败: {}", e.getMessage());
            return oldConfigParams;
        }
    }

    private void mergePolicyInto(ConversationRuntimePolicy target, ConversationRuntimePolicy source) {
        if (source.getTemperature() != null) {
            target.setTemperature(source.getTemperature());
        }
        if (source.getMaxTokens() != null) {
            target.setMaxTokens(source.getMaxTokens());
        }
        if (source.getTopP() != null) {
            target.setTopP(source.getTopP());
        }
        if (source.getFrequencyPenalty() != null) {
            target.setFrequencyPenalty(source.getFrequencyPenalty());
        }
        if (source.getPresencePenalty() != null) {
            target.setPresencePenalty(source.getPresencePenalty());
        }
        if (source.getThinkingMode() != null) {
            target.setThinkingMode(source.getThinkingMode());
        }
        if (source.getShowThinking() != null) {
            target.setShowThinking(source.getShowThinking());
        }
        if (source.getThinkingBudgetTokens() != null) {
            target.setThinkingBudgetTokens(source.getThinkingBudgetTokens());
        }
        if (source.getReasoningEffort() != null) {
            target.setReasoningEffort(source.getReasoningEffort());
        }
        if (source.getContextStrategy() != null) {
            target.setContextStrategy(source.getContextStrategy());
        }
        if (source.getRecentTurnsToKeep() != null) {
            target.setRecentTurnsToKeep(source.getRecentTurnsToKeep());
        }
        if (source.getReservedOutputTokens() != null) {
            target.setReservedOutputTokens(source.getReservedOutputTokens());
        }
        if (source.getCompactTriggerRatio() != null) {
            target.setCompactTriggerRatio(source.getCompactTriggerRatio());
        }
        if (source.getCompactMemory() != null) {
            target.setCompactMemory(source.getCompactMemory());
        }
    }

    private String normalizeContextStrategy(String value) {
        if (value == null || value.isBlank()) {
            return ConversationRuntimePolicy.STRATEGY_AUTO;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case ConversationRuntimePolicy.STRATEGY_WINDOW_ONLY, ConversationRuntimePolicy.STRATEGY_COMPACT -> normalized;
            default -> ConversationRuntimePolicy.STRATEGY_AUTO;
        };
    }

    private String normalizeThinkingMode(String value) {
        if (value == null || value.isBlank()) {
            return "disabled";
        }
        String normalized = value.trim().toLowerCase();
        return THINKING_MODES.contains(normalized) ? normalized : "disabled";
    }

    private String normalizeReasoningEffort(String value) {
        if (value == null || value.isBlank()) {
            return "medium";
        }
        String normalized = value.trim().toLowerCase();
        return REASONING_EFFORTS.contains(normalized) ? normalized : "medium";
    }

    private int resolveMaxTokensUpperBound(AiModel model) {
        int contextWindow = model != null && model.getContextWindow() != null ? model.getContextWindow() : 8192;
        return Math.max(1024, contextWindow - 512);
    }

    private int resolveThinkingBudgetUpperBound(AiModel model) {
        int contextWindow = model != null && model.getContextWindow() != null ? model.getContextWindow() : 8192;
        return Math.max(1024, contextWindow / 2);
    }

    private int clampInt(Integer value, int min, int max, int defaultValue) {
        int actual = value != null ? value : defaultValue;
        return Math.min(Math.max(actual, min), Math.max(min, max));
    }

    private double clampDouble(Double value, double min, double max, double defaultValue) {
        double actual = value != null ? value : defaultValue;
        return Math.min(Math.max(actual, min), max);
    }

    private Map<String, Object> parseRoot(String configParams) {
        if (configParams == null || configParams.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(configParams, MAP_TYPE);
        } catch (Exception e) {
            log.debug("解析会话config_params失败，忽略原配置: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractNestedMap(Map<String, Object> root, String key) {
        if (root == null || root.isEmpty()) {
            return Map.of();
        }
        Object nested = root.get(key);
        if (nested instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private Map<String, Object> objectToMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        try {
            return objectMapper.convertValue(value, MAP_TYPE);
        } catch (IllegalArgumentException e) {
            return Map.of();
        }
    }

    private <T> T convert(Object source, Class<T> type) {
        if (source == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(source, type);
        } catch (IllegalArgumentException e) {
            log.debug("转换会话运行配置失败: {}", e.getMessage());
            return null;
        }
    }
}
