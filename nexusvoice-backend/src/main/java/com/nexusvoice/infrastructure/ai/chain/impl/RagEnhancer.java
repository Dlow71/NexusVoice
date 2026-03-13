package com.nexusvoice.infrastructure.ai.chain.impl;

import com.nexusvoice.domain.ai.model.EnhancementContext;
import com.nexusvoice.domain.rag.model.entity.KnowledgeBase;
import com.nexusvoice.domain.rag.model.enums.KnowledgeBaseStatus;
import com.nexusvoice.domain.rag.repository.KnowledgeBaseRepository;
import com.nexusvoice.infrastructure.ai.chain.AbstractChatEnhancer;
import com.nexusvoice.infrastructure.ai.converter.AiModelConverter;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.rag.service.DocumentRetrievalService;
import com.nexusvoice.infrastructure.rag.service.RagQueryPlanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class RagEnhancer extends AbstractChatEnhancer {

    private static final int KB_TOP_K = 3;

    private final DocumentRetrievalService documentRetrievalService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RagQueryPlanner ragQueryPlanner;

    public RagEnhancer(@Lazy DocumentRetrievalService documentRetrievalService,
                       KnowledgeBaseRepository knowledgeBaseRepository,
                       RagQueryPlanner ragQueryPlanner) {
        this.documentRetrievalService = documentRetrievalService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.ragQueryPlanner = ragQueryPlanner;
    }

    @Value("${nexusvoice.ai.enhancement.rag.enabled:true}")
    private boolean ragEnabled;

    @Override
    protected EnhancementContext doEnhance(EnhancementContext context) {
        ChatRequest request = AiModelConverter.getInfrastructureRequest(context);
        if (request == null) {
            return context;
        }

        String userQuery = extractLastUserMessage(request.getMessages());
        if (userQuery == null || userQuery.isBlank()) {
            return context;
        }
        RagQueryPlanner.RagQueryPlan queryPlan = ragQueryPlanner.plan(userQuery);
        RagGroundingMode groundingMode = resolveGroundingMode(request.getRagGroundingMode());

        List<Long> knowledgeBaseIds = sanitizeKnowledgeBaseIds(request.getKnowledgeBaseIds(), request.getUserId());
        if (knowledgeBaseIds.isEmpty()) {
            log.debug("RAG增强跳过，没有可用知识库");
            return context;
        }

        List<RetrievedSnippet> snippets = retrieveSnippets(queryPlan, knowledgeBaseIds);
        if (snippets.isEmpty()) {
            List<Long> fallbackIds = findFallbackKnowledgeBaseIds(request.getUserId(), knowledgeBaseIds);
            if (!fallbackIds.isEmpty()) {
                log.info("RAG主知识库未命中，回退检索用户其他知识库，userId={}, fallbackCount={}",
                        request.getUserId(), fallbackIds.size());
                snippets = retrieveSnippets(queryPlan, fallbackIds);
                if (!snippets.isEmpty()) {
                    knowledgeBaseIds = fallbackIds;
                }
            }
        }
        String ragContext = snippets.isEmpty()
                ? buildNoHitRagContext(knowledgeBaseIds, queryPlan, groundingMode)
                : buildRagContext(snippets, queryPlan, groundingMode);
        context.setRagResults(ragContext);

        List<ChatMessage> enhancedMessages = new ArrayList<>(request.getMessages());
        rewriteLastUserMessage(enhancedMessages, userQuery, queryPlan, groundingMode);
        enhancedMessages.add(0, ChatMessage.system(ragContext));
        request.setMessages(enhancedMessages);
        AiModelConverter.updateContextRequest(context, request);

        log.info("RAG增强完成，原始query='{}'，主检索query='{}'，查询意图={}，groundingMode={}，检索变体={}，知识库数量：{}，命中片段：{}",
                userQuery, queryPlan.normalizedQuery(), queryPlan.intent(), groundingMode,
                queryPlan.retrievalQueries(), knowledgeBaseIds.size(), snippets.size());
        return context;
    }

    @Override
    public boolean shouldProcess(EnhancementContext context) {
        return ragEnabled && Boolean.TRUE.equals(context.getEnableRag());
    }

    @Override
    public String getName() {
        return "RAG增强器";
    }

    private List<Long> sanitizeKnowledgeBaseIds(List<Long> rawIds, Long userId) {
        if (userId == null) {
            return List.of();
        }

        if (rawIds == null || rawIds.isEmpty()) {
            return knowledgeBaseRepository.findByUserIdAndStatus(userId, KnowledgeBaseStatus.ACTIVE).stream()
                    .filter(kb -> !kb.isDeleted() && kb.isAvailable())
                    .map(KnowledgeBase::getId)
                    .toList();
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(rawIds);
        List<Long> validIds = new ArrayList<>();
        for (Long knowledgeBaseId : uniqueIds) {
            if (knowledgeBaseId == null) {
                continue;
            }
            knowledgeBaseRepository.findById(knowledgeBaseId)
                    .filter(kb -> userId.equals(kb.getUserId()) && kb.isAvailable() && !kb.isDeleted())
                    .map(KnowledgeBase::getId)
                    .ifPresent(validIds::add);
        }
        return validIds;
    }

    private List<Long> findFallbackKnowledgeBaseIds(Long userId, List<Long> usedIds) {
        if (userId == null) {
            return List.of();
        }
        Set<Long> used = new LinkedHashSet<>(usedIds);
        return knowledgeBaseRepository.findByUserIdAndStatus(userId, KnowledgeBaseStatus.ACTIVE).stream()
                .filter(kb -> !kb.isDeleted() && kb.isAvailable() && !used.contains(kb.getId()))
                .map(KnowledgeBase::getId)
                .toList();
    }

    private List<RetrievedSnippet> retrieveSnippets(RagQueryPlanner.RagQueryPlan queryPlan, List<Long> knowledgeBaseIds) {
        List<RetrievedSnippet> snippets = new ArrayList<>();
        for (Long knowledgeBaseId : knowledgeBaseIds) {
            try {
                List<DocumentRetrievalService.RetrievalResult> results =
                        documentRetrievalService.hybridSearch(
                                queryPlan.normalizedQuery(),
                                queryPlan.retrievalQueries(),
                                knowledgeBaseId,
                                KB_TOP_K
                        );
                for (DocumentRetrievalService.RetrievalResult result : results) {
                    snippets.add(new RetrievedSnippet(knowledgeBaseId, result));
                }
            } catch (Exception ex) {
                log.warn("RAG检索失败，knowledgeBaseId={}，error={}", knowledgeBaseId, ex.getMessage());
            }
        }

        return snippets.stream()
                .sorted((left, right) -> Double.compare(
                        right.result().getScore() != null ? right.result().getScore() : 0D,
                        left.result().getScore() != null ? left.result().getScore() : 0D))
                .limit(6)
                .toList();
    }

    private String buildRagContext(List<RetrievedSnippet> snippets,
                                   RagQueryPlanner.RagQueryPlan queryPlan,
                                   RagGroundingMode groundingMode) {
        StringBuilder builder = new StringBuilder();
        builder.append("【知识库能力说明】你当前会话已经接入了用户的知识库，本轮你能看到下面这些由系统检索出来的知识库片段。")
                .append("你不能声称自己完全看不到知识库；但也不要声称自己能浏览整个知识库，只能基于本轮检索到的片段回答。\n\n");
        if (groundingMode == RagGroundingMode.STRICT) {
            builder.append("【强约束】以下规则优先级极高，必须严格遵守：\n")
                    .append("1. 只能依据下面给出的资料片段回答，禁止补充片段外的事实、常识、背景资料或你的主观推断。\n")
                    .append("2. 如果某个结论无法从资料中直接或稳定推出，就必须明确写“资料未覆盖”或“无法从资料确认”。\n")
                    .append("3. 如果用户要求对比、总结、归因，只能总结资料里明确出现的维度；不要自行发明比较维度。\n")
                    .append("4. 禁止新增资料中未出现的专有名词、产品名、人物标签、方法论名词或案例。\n")
                    .append("5. 尽量在句内标注资料编号，如[资料1]、[资料2]。\n\n");
        } else {
            builder.append("【回答模式】当前是扩展回答模式：\n")
                    .append("1. 仍然要优先依据下面给出的资料片段回答。\n")
                    .append("2. 在资料明确覆盖的前提下，可以做少量补充解释或更自然的表达，但不要把补充内容伪装成知识库原文。\n")
                    .append("3. 如果使用了资料外的延伸理解，请明确写成“基于资料的补充理解”或“延伸解释”。\n\n");
        }
        builder.append("【检索策略】用户原问题：").append(queryPlan.originalQuery()).append("\n");
        builder.append("检索意图：").append(queryPlan.intent()).append("\n");
        builder.append("回答模式：").append(groundingMode).append("\n");
        builder.append("检索变体：").append(String.join(" | ", queryPlan.retrievalQueries())).append("\n\n");
        builder.append("【知识库检索结果】请优先依据以下资料回答；如果资料不足，再明确说明不确定，不要编造。\n\n");
        int index = 1;
        for (RetrievedSnippet snippet : snippets) {
            String location = formatLocation(snippet.result());
            String knowledgeBaseName = resolveKnowledgeBaseName(snippet.knowledgeBaseId());
            builder.append(index++)
                    .append(". [资料").append(index - 1).append("] 知识库=").append(knowledgeBaseName)
                    .append("，知识库ID=").append(snippet.knowledgeBaseId())
                    .append("，文件ID=").append(snippet.result().getFileId())
                    .append("，文件=").append(snippet.result().getTitle() != null ? snippet.result().getTitle() : "未命名文件")
                    .append(location)
                    .append("，分数=").append(String.format("%.4f", snippet.result().getScore() != null ? snippet.result().getScore() : 0D))
                    .append("，命中查询=").append(snippet.result().getMatchedQueries() == null || snippet.result().getMatchedQueries().isEmpty()
                            ? queryPlan.normalizedQuery()
                            : String.join(" / ", snippet.result().getMatchedQueries()))
                    .append("\n")
                    .append(snippet.result().getContent())
                    .append("\n\n");
        }
        builder.append("【回答要求】1. 优先引用上面的资料内容组织答案，并尽量在句内标注资料编号，如[资料1]、[资料2]。")
                .append("2. 如果需要综合多条资料，显式说明是综合哪些资料得出的。");
        if (groundingMode == RagGroundingMode.STRICT) {
            builder.append("3. 对比题、总结题、归因题都要先说“资料里明确写到了什么”，再给结论。")
                    .append("4. 如果用户问题超出资料覆盖范围，要明确说明资料未覆盖，不要补写为确定事实。")
                    .append("5. 如果必须归纳，请只做贴近原文的归纳，不要新增资料里没出现的术语或案例。");
        } else {
            builder.append("3. 如果做了超出资料原句的概括，请明确标注那是基于资料的延伸理解。")
                    .append("4. 如果用户问题超出资料覆盖范围，可以结合常识补充，但必须先说明资料本身覆盖到哪里。");
        }
        if (groundingMode == RagGroundingMode.STRICT && queryPlan.intent() == RagQueryPlanner.QueryIntent.COMPARISON) {
            builder.append("5. 当前是对比题时，严禁套用通用人物比较模板；不要自行扩展到公司规模、公众形象、国际化、管理风格等资料未出现的维度。");
        }
        return builder.toString();
    }

    private String buildNoHitRagContext(List<Long> knowledgeBaseIds,
                                        RagQueryPlanner.RagQueryPlan queryPlan,
                                        RagGroundingMode groundingMode) {
        return "【知识库能力说明】你当前会话已经接入了用户的知识库，知识库ID为 " + knowledgeBaseIds
                + "。本轮针对“" + queryPlan.normalizedQuery() + "”检索，没有命中到与当前问题直接相关的知识库片段。"
                + "系统已经尝试过这些检索变体：" + queryPlan.retrievalQueries() + "。"
                + (groundingMode == RagGroundingMode.STRICT
                    ? "你仍然不能假装自己看到了相关资料，更不能用外部知识替代知识库内容。"
                    : "你可以给出一般性解释，但必须先声明知识库本轮没有命中相关资料。")
                + "如果用户询问你能否看到知识库，请回答：你只能基于本轮检索命中的片段来回答，当前这次没有检索到相关内容；"
                + "不要回答成“完全看不到知识库”或“没有知识库能力”。";
    }

    private String formatLocation(DocumentRetrievalService.RetrievalResult result) {
        if (result.getStartPage() == null && result.getEndPage() == null) {
            return "";
        }
        if (result.getStartPage() != null && result.getStartPage().equals(result.getEndPage())) {
            return "，片段位置=" + result.getStartPage();
        }
        return "，片段范围=" + result.getStartPage() + "-" + result.getEndPage();
    }

    private String resolveKnowledgeBaseName(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return "未命名知识库";
        }
        return knowledgeBaseRepository.findById(knowledgeBaseId)
                .map(KnowledgeBase::getName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("未命名知识库");
    }

    private String extractLastUserMessage(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.getRole() == com.nexusvoice.domain.conversation.constant.MessageRole.USER) {
                return message.getContent();
            }
        }
        return null;
    }

    private void rewriteLastUserMessage(List<ChatMessage> messages,
                                        String originalUserQuery,
                                        RagQueryPlanner.RagQueryPlan queryPlan,
                                        RagGroundingMode groundingMode) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.getRole() == com.nexusvoice.domain.conversation.constant.MessageRole.USER) {
                StringBuilder rewritten = new StringBuilder();
                if (groundingMode == RagGroundingMode.STRICT) {
                    rewritten.append("请严格只根据上方【知识库检索结果】回答，不要补充任何片段外的常识或背景信息。\n")
                            .append("如果资料不足，请直接说明“资料未覆盖”。\n")
                            .append("回答时尽量引用资料编号。\n")
                            .append("不要新增资料中未出现的专有名词、产品名、人物标签或案例。\n");
                    if (queryPlan.intent() == RagQueryPlanner.QueryIntent.COMPARISON) {
                        rewritten.append("这是对比题：只能比较资料里明确出现的维度，不要扩展到通用人物比较模板。\n");
                    } else if (queryPlan.intent() == RagQueryPlanner.QueryIntent.CAUSAL) {
                        rewritten.append("这是归因题：只能写资料明确给出的原因链，不能补写额外背景。\n");
                    }
                } else {
                    rewritten.append("请优先根据上方【知识库检索结果】回答。\n")
                            .append("如果做了超出资料原句的延伸解释，请明确说明那是基于资料的补充理解。\n");
                }
                rewritten.append("用户问题：").append(originalUserQuery);
                message.setContent(rewritten.toString());
                return;
            }
        }
    }

    private RagGroundingMode resolveGroundingMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return RagGroundingMode.STRICT;
        }
        try {
            return RagGroundingMode.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return RagGroundingMode.STRICT;
        }
    }

    private enum RagGroundingMode {
        STRICT,
        FLEXIBLE
    }

    private record RetrievedSnippet(Long knowledgeBaseId, DocumentRetrievalService.RetrievalResult result) {
    }
}
