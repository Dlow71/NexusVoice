package com.nexusvoice.infrastructure.conversation.service;

import com.nexusvoice.application.conversation.dto.ChatRequestDto;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.conversation.constant.MessageRole;
import com.nexusvoice.domain.conversation.model.Conversation;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.model.vo.ConversationCompactMemory;
import com.nexusvoice.domain.conversation.model.vo.ConversationContextSnapshot;
import com.nexusvoice.domain.conversation.model.vo.ConversationRuntimePolicy;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 会话上下文规划服务。
 * 负责窗口裁剪、compact摘要和上下文快照生成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationContextService {

    private static final int CONTEXT_SAFETY_BUFFER = 512;
    private static final int RAG_RESERVED_TOKENS = 2400;
    private static final int SEARCH_RESERVED_TOKENS = 1200;

    private final DynamicAiModelBeanManager modelBeanManager;
    private final ConversationRuntimeConfigService runtimeConfigService;

    public PreparedConversationContext prepareContext(Conversation conversation,
                                                      ChatRequestDto requestDto,
                                                      String systemPrompt,
                                                      List<ConversationMessage> history,
                                                      String modelKey) {
        return prepareContext(conversation, requestDto, systemPrompt, history, modelKey, true);
    }

    public PreparedConversationContext prepareContext(Conversation conversation,
                                                      ChatRequestDto requestDto,
                                                      String systemPrompt,
                                                      List<ConversationMessage> history,
                                                      String modelKey,
                                                      boolean allowCompactRefresh) {
        AiModel model = modelBeanManager.getModelByKey(modelKey);
        ConversationRuntimePolicy storedPolicy = runtimeConfigService.readPolicy(conversation, model);
        ConversationRuntimePolicy resolvedPolicy = runtimeConfigService.mergeRequestOverrides(storedPolicy, requestDto, model);

        List<ConversationMessage> usableHistory = (history == null ? List.<ConversationMessage>of() : history).stream()
                .filter(message -> message != null
                        && (message.getRole() == MessageRole.USER || message.getRole() == MessageRole.ASSISTANT)
                        && message.getContent() != null
                        && !message.getContent().isBlank())
                .sorted(Comparator.comparing(message -> message.getSequence() != null ? message.getSequence() : Integer.MAX_VALUE))
                .toList();

        BudgetPlan budgetPlan = buildBudgetPlan(model, resolvedPolicy, requestDto);
        int systemPromptTokens = estimateTokenCount(systemPrompt);
        int totalHistoryTokens = usableHistory.stream()
                .map(ConversationMessage::getContent)
                .mapToInt(this::estimateTokenCount)
                .sum();

        int recentMessageLimit = Math.max(2, resolvedPolicy.getRecentTurnsToKeep() * 2);
        int splitIndex = Math.max(usableHistory.size() - recentMessageLimit, 0);
        List<ConversationMessage> olderMessages = usableHistory.subList(0, splitIndex);
        List<ConversationMessage> recentMessages = usableHistory.subList(splitIndex, usableHistory.size());

        boolean autoCompactNeeded = shouldCompactAutomatically(
                resolvedPolicy,
                usableHistory.size(),
                totalHistoryTokens,
                budgetPlan.availableInputTokens()
        );
        boolean compactEnabled = ConversationRuntimePolicy.STRATEGY_COMPACT.equals(resolvedPolicy.getContextStrategy())
                || (ConversationRuntimePolicy.STRATEGY_AUTO.equals(resolvedPolicy.getContextStrategy()) && autoCompactNeeded);

        ConversationCompactMemory compactMemory = resolvedPolicy.getCompactMemory() != null
                ? resolvedPolicy.getCompactMemory()
                : ConversationCompactMemory.builder().build();
        boolean compactUpdated = false;

        if (compactEnabled && !olderMessages.isEmpty()) {
            int compactUntilSequence = lastSequence(olderMessages);
            int previousSummaryUntilSequence = compactMemory.getSummaryUntilSequence() != null
                    ? compactMemory.getSummaryUntilSequence()
                    : 0;
            if (allowCompactRefresh
                    && compactUntilSequence > previousSummaryUntilSequence) {
                List<ConversationMessage> deltaMessages = olderMessages.stream()
                        .filter(message -> (message.getSequence() != null ? message.getSequence() : 0)
                                > previousSummaryUntilSequence)
                        .toList();
                if (!deltaMessages.isEmpty()) {
                    compactMemory = refreshCompactMemory(compactMemory, deltaMessages, modelKey, conversation);
                    compactUpdated = true;
                }
            }
        }
        resolvedPolicy.setCompactMemory(compactMemory);

        boolean usingCompactSummary = compactEnabled && compactMemory != null && compactMemory.hasSummary();
        int compactSummaryTokens = usingCompactSummary ? estimateTokenCount(compactMemory.getSummary()) : 0;
        int availableHistoryTokens = Math.max(256, budgetPlan.availableInputTokens() - systemPromptTokens - compactSummaryTokens);

        List<ConversationMessage> sourceHistory = usingCompactSummary ? recentMessages : usableHistory;
        HistorySelection selection = selectMessagesWithinBudget(sourceHistory, availableHistoryTokens);

        List<ChatMessage> finalMessages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            finalMessages.add(ChatMessage.system(systemPrompt));
        }
        if (usingCompactSummary) {
            finalMessages.add(ChatMessage.system(buildCompactMemoryMessage(compactMemory)));
        }
        for (ConversationMessage message : selection.selectedMessages()) {
            if (message.getRole() == MessageRole.USER) {
                finalMessages.add(ChatMessage.user(message.getContent()));
            } else if (message.getRole() == MessageRole.ASSISTANT) {
                finalMessages.add(ChatMessage.assistant(message.getContent()));
            }
        }

        int estimatedInputTokens = systemPromptTokens + compactSummaryTokens + selection.estimatedTokens();
        int remainingTokens = Math.max(0, budgetPlan.modelContextWindow()
                - estimatedInputTokens
                - budgetPlan.reservedOutputTokens()
                - budgetPlan.reservedThinkingTokens()
                - budgetPlan.reservedRagTokens()
                - budgetPlan.reservedSearchTokens());

        ConversationContextSnapshot snapshot = ConversationContextSnapshot.builder()
                .modelKey(model.getModelKey())
                .modelContextWindow(budgetPlan.modelContextWindow())
                .estimatedInputTokens(estimatedInputTokens)
                .reservedOutputTokens(budgetPlan.reservedOutputTokens())
                .reservedThinkingTokens(budgetPlan.reservedThinkingTokens())
                .reservedRagTokens(budgetPlan.reservedRagTokens())
                .reservedSearchTokens(budgetPlan.reservedSearchTokens())
                .remainingTokens(remainingTokens)
                .systemPromptTokens(systemPromptTokens)
                .compactSummaryTokens(compactSummaryTokens)
                .historyTokens(selection.estimatedTokens())
                .totalHistoryMessages(usableHistory.size())
                .includedHistoryMessages(selection.selectedMessages().size())
                .compactedMessages(usingCompactSummary ? olderMessages.size() : Math.max(usableHistory.size() - selection.selectedMessages().size(), 0))
                .usedCompactSummary(usingCompactSummary)
                .compactSummaryUpdated(compactUpdated)
                .needsCompaction(autoCompactNeeded)
                .appliedContextStrategy(usingCompactSummary ? ConversationRuntimePolicy.STRATEGY_COMPACT : ConversationRuntimePolicy.STRATEGY_WINDOW_ONLY)
                .build();

        return new PreparedConversationContext(finalMessages, resolvedPolicy, snapshot);
    }

    private BudgetPlan buildBudgetPlan(AiModel model,
                                       ConversationRuntimePolicy policy,
                                       ChatRequestDto requestDto) {
        int contextWindow = model.getContextWindow() != null ? model.getContextWindow() : 8192;
        int reservedOutput = clampInt(
                policy.getReservedOutputTokens() != null ? policy.getReservedOutputTokens() : policy.getMaxTokens(),
                256,
                Math.max(1024, contextWindow - CONTEXT_SAFETY_BUFFER)
        );
        int reservedThinking = requestDto != null
                && requestDto.getThinkingMode() != null
                && !"disabled".equalsIgnoreCase(requestDto.getThinkingMode())
                && requestDto.getThinkingBudgetTokens() != null
                ? clampInt(requestDto.getThinkingBudgetTokens(), 0, contextWindow / 2)
                : 0;
        int reservedRag = requestDto != null && Boolean.TRUE.equals(requestDto.getEnableRag()) ? RAG_RESERVED_TOKENS : 0;
        int reservedSearch = requestDto != null && Boolean.TRUE.equals(requestDto.getEnableWebSearch()) ? SEARCH_RESERVED_TOKENS : 0;

        int availableInput = Math.max(
                1024,
                contextWindow - reservedOutput - reservedThinking - reservedRag - reservedSearch - CONTEXT_SAFETY_BUFFER
        );
        return new BudgetPlan(contextWindow, availableInput, reservedOutput, reservedThinking, reservedRag, reservedSearch);
    }

    private boolean shouldCompactAutomatically(ConversationRuntimePolicy policy,
                                               int historyMessageCount,
                                               int historyTokens,
                                               int availableInputTokens) {
        int recentLimit = Math.max(2, policy.getRecentTurnsToKeep() * 2);
        boolean exceedsMessageThreshold = historyMessageCount > recentLimit + 4;
        double triggerRatio = policy.getCompactTriggerRatio() != null ? policy.getCompactTriggerRatio() : 0.72D;
        boolean exceedsTokenThreshold = historyTokens > (int) Math.floor(availableInputTokens * triggerRatio);
        return exceedsMessageThreshold && exceedsTokenThreshold;
    }

    private HistorySelection selectMessagesWithinBudget(List<ConversationMessage> candidates, int budget) {
        if (candidates == null || candidates.isEmpty()) {
            return new HistorySelection(List.of(), 0);
        }
        List<ConversationMessage> buffer = new ArrayList<>();
        int usedTokens = 0;

        for (int index = candidates.size() - 1; index >= 0; index--) {
            ConversationMessage message = candidates.get(index);
            int messageTokens = estimateTokenCount(message.getContent());
            if (buffer.size() < 3 || usedTokens + messageTokens <= budget) {
                usedTokens += messageTokens;
                buffer.add(message);
            } else {
                break;
            }
        }

        buffer.sort(Comparator.comparing(message -> message.getSequence() != null ? message.getSequence() : Integer.MAX_VALUE));
        return new HistorySelection(buffer, usedTokens);
    }

    private ConversationCompactMemory refreshCompactMemory(ConversationCompactMemory existingMemory,
                                                           List<ConversationMessage> deltaMessages,
                                                           String modelKey,
                                                           Conversation conversation) {
        String summary = generateCompactSummary(existingMemory, deltaMessages, modelKey, conversation);
        if (summary == null || summary.isBlank()) {
            summary = buildFallbackSummary(existingMemory, deltaMessages);
        }
        if (summary == null || summary.isBlank()) {
            return existingMemory;
        }

        int compactUntilSequence = lastSequence(deltaMessages);
        return ConversationCompactMemory.builder()
                .summary(summary)
                .summaryUntilSequence(compactUntilSequence)
                .sourceMessageCount((existingMemory != null && existingMemory.getSourceMessageCount() != null
                        ? existingMemory.getSourceMessageCount()
                        : 0) + deltaMessages.size())
                .estimatedTokens(estimateTokenCount(summary))
                .modelKey(modelKey)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String generateCompactSummary(ConversationCompactMemory existingMemory,
                                          List<ConversationMessage> deltaMessages,
                                          String modelKey,
                                          Conversation conversation) {
        try {
            AiChatService aiChatService = modelBeanManager.getServiceByModelKey(modelKey);
            String existingSummary = existingMemory != null && existingMemory.hasSummary() ? existingMemory.getSummary() : "暂无历史摘要。";
            String messageTranscript = deltaMessages.stream()
                    .map(message -> (message.getRole() == MessageRole.USER ? "用户" : "助手") + "：" + message.getContent())
                    .collect(Collectors.joining("\n"));

            List<ChatMessage> summaryMessages = List.of(
                    ChatMessage.system("""
                            你是一个会话上下文压缩器。你的任务是把旧对话压缩成结构化摘要，供后续多轮会话继续使用。
                            输出要求：
                            1. 只保留后续回答真正需要的信息，不要写客套话。
                            2. 按以下结构输出：会话目标 / 已确认事实 / 用户偏好 / 未完成事项 / 风险与限制。
                            3. 每项最多3条，尽量短句。
                            4. 不要编造对话中没有出现的信息。
                            """),
                    ChatMessage.user("""
                            现有摘要：
                            %s

                            新增需要压缩的旧消息：
                            %s

                            请输出更新后的压缩摘要。
                            """.formatted(existingSummary, messageTranscript))
            );

            ChatRequest summaryRequest = ChatRequest.builder()
                    .messages(summaryMessages)
                    .model(modelKey)
                    .temperature(0.2)
                    .maxTokens(700)
                    .topP(1.0)
                    .frequencyPenalty(0.0)
                    .presencePenalty(0.0)
                    .stream(false)
                    .userId(conversation.getUserId())
                    .conversationId(conversation.getId())
                    .enableWebSearch(false)
                    .enableRag(false)
                    .thinkingMode("disabled")
                    .showThinking(false)
                    .build();

            ChatResponse response = aiChatService.chat(summaryRequest);
            if (response != null && Boolean.TRUE.equals(response.getSuccess()) && response.getContent() != null) {
                return response.getContent().trim();
            }
        } catch (Exception e) {
            log.warn("生成会话compact摘要失败，将回退到启发式摘要，conversationId={}, error={}",
                    conversation.getId(), e.getMessage());
        }
        return null;
    }

    private String buildFallbackSummary(ConversationCompactMemory existingMemory, List<ConversationMessage> deltaMessages) {
        StringBuilder builder = new StringBuilder();
        if (existingMemory != null && existingMemory.hasSummary()) {
            builder.append(existingMemory.getSummary().trim()).append("\n\n");
        }
        builder.append("会话目标：\n");
        deltaMessages.stream()
                .filter(message -> message.getRole() == MessageRole.USER)
                .map(ConversationMessage::getContent)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(content -> !content.isEmpty())
                .limit(3)
                .forEach(content -> builder.append("- ").append(truncate(content, 120)).append("\n"));

        builder.append("已讨论内容：\n");
        deltaMessages.stream()
                .map(message -> (message.getRole() == MessageRole.USER ? "用户" : "助手") + "：" + truncate(message.getContent(), 120))
                .limit(6)
                .forEach(line -> builder.append("- ").append(line).append("\n"));
        return builder.toString().trim();
    }

    private String buildCompactMemoryMessage(ConversationCompactMemory compactMemory) {
        return """
                【会话压缩摘要】
                以下摘要来自本会话较早轮次的压缩记忆，用于帮助你延续长期上下文。
                它不是新的用户提问，而是旧历史的浓缩，请在后续回答中结合最近消息一起使用。

                %s
                """.formatted(compactMemory.getSummary());
    }

    private int lastSequence(List<ConversationMessage> messages) {
        return messages.stream()
                .map(ConversationMessage::getSequence)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private int estimateTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 3.2D);
    }

    private int clampInt(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private String truncate(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    public record PreparedConversationContext(
            List<ChatMessage> messages,
            ConversationRuntimePolicy policy,
            ConversationContextSnapshot snapshot
    ) {
    }

    private record HistorySelection(
            List<ConversationMessage> selectedMessages,
            int estimatedTokens
    ) {
    }

    private record BudgetPlan(
            int modelContextWindow,
            int availableInputTokens,
            int reservedOutputTokens,
            int reservedThinkingTokens,
            int reservedRagTokens,
            int reservedSearchTokens
    ) {
    }
}
