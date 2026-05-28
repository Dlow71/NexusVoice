package com.nexusvoice.application.role.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.application.agent.dto.AgentExecuteRequest;
import com.nexusvoice.application.agent.dto.AgentExecuteResponse;
import com.nexusvoice.application.agent.service.AgentApplicationService;
import com.nexusvoice.application.role.assembler.RoleAssembler;
import com.nexusvoice.application.role.dto.RoleBriefDto;
import com.nexusvoice.application.role.dto.RoleCreateRequest;
import com.nexusvoice.application.role.dto.RoleDTO;
import com.nexusvoice.application.role.dto.RoleAssistantConfirmRequest;
import com.nexusvoice.application.role.dto.RoleResearchApplyRequest;
import com.nexusvoice.domain.config.service.SystemConfigService;
import com.nexusvoice.domain.agent.enums.AgentType;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.repository.ConversationMessageRepository;
import com.nexusvoice.domain.conversation.repository.ConversationRepository;
import com.nexusvoice.domain.conversation.service.ConversationDomainService;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.domain.tool.model.SearchResult;
import com.nexusvoice.domain.tool.repository.SearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 角色助手应用服务
 * - 从对话摘要生成角色草稿（快速模式）
 * - 可选深研模式：检索外部来源后增强草稿
 * - 最终创建私人角色
 */
@Slf4j
@Service
public class RoleAssistantService {

    private static final int BRIEF_GENERATION_MAX_ATTEMPTS = 3;
    private static final long BRIEF_GENERATION_RETRY_DELAY_MS = 1200L;
    private static final String DEFAULT_BRIEF_MODEL_KEY = "openai:gpt-oss-20b";
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ConversationDomainService conversationDomainService;
    private final DynamicAiModelBeanManager modelBeanManager;
    private final SystemConfigService systemConfigService;
    private final RoleApplicationService roleApplicationService;
    private final com.nexusvoice.application.tts.service.TTSService ttsService;
    private final SearchRepository searchRepository;
    private final ObjectMapper objectMapper;
    private final AgentApplicationService agentApplicationService;  // 新增：Agent服务

    private record ResolvedBriefChat(AiChatService chatService, String modelKey) {}

    public RoleAssistantService(ConversationRepository conversationRepository,
                                ConversationMessageRepository messageRepository,
                                ConversationDomainService conversationDomainService,
                                DynamicAiModelBeanManager modelBeanManager,
                                SystemConfigService systemConfigService,
                                RoleApplicationService roleApplicationService,
                                com.nexusvoice.application.tts.service.TTSService ttsService,
                                SearchRepository searchRepository,
                                ObjectMapper objectMapper,
                                AgentApplicationService agentApplicationService) {  // 新增
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationDomainService = conversationDomainService;
        this.modelBeanManager = modelBeanManager;
        this.systemConfigService = systemConfigService;
        this.roleApplicationService = roleApplicationService;
        this.ttsService = ttsService;
        this.searchRepository = searchRepository;
        this.objectMapper = objectMapper;
        this.agentApplicationService = agentApplicationService;  // 新增
    }

    /**
     * 生成角色草稿（快速模式，支持按需联网）
     */
    @Transactional(readOnly = false)
    public RoleBriefDto generateBriefFromConversation(Long conversationId, Long userId, boolean enableWebSearch) {
        // 权限校验
        conversationDomainService.validateConversationAccess(conversationId, userId);

        // 获取最近的对话内容，限制消息数量避免超长
        List<ConversationMessage> history = messageRepository.findByConversationIdOrderBySequence(conversationId);
        List<String> transcript = toTranscript(history, 20, 500);

        String system = "你是资深AI角色设定助手。基于用户与AI的对话内容，总结出一个可用的’角色草稿’。" +
                "务必原创，避免复刻具体IP设定、名称、台词或标识。" +
                "输出严格为一个JSON对象，不要包含多余文字。字段：" +
                "name(<=20汉字)、description、personaPrompt、greetingMessage、avatarUrl(可空)、voiceType(如未给出，后端将使用默认音色)、" +
                "sources(数组，元素含title/url/snippet，可为空)、disclaimers(数组)。" +
                "整体语气与要求：中文，信息完整、具体、可直接用于人设。";

        String user = "请基于以下对话生成角色草稿JSON：\n\n" + String.join("\n", transcript);

        List<ChatMessage> messages = List.of(ChatMessage.system(system), ChatMessage.user(user));

        ResolvedBriefChat resolvedBriefChat = resolveRoleBriefChat();

        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .model(resolvedBriefChat.modelKey())
                .temperature(0.5)
                .maxTokens(1200)
                .userId(userId)
                .conversationId(conversationId)
                .enableWebSearch(enableWebSearch)
                .build();

        ChatResponse response = executeBriefChatWithRetry(resolvedBriefChat.chatService(), request, conversationId, userId);
        if (!response.getSuccess()) {
            throw BizException.of(ErrorCodeEnum.AI_REQUEST_FAILED, "生成角色草稿失败：" + response.getErrorMessage());
        }

        RoleBriefDto brief = parseBriefJson(response.getContent());
        applyBriefDefaults(brief);

        // 写入系统消息记录草稿（不存思维链，仅存结论与类型标记）
        saveSystemNote(conversationId, "已生成角色草稿", makeMetadata("ROLE_BRIEF", toJson(brief)));

        return brief;
    }

    /**
     * 深研任务清单预览（不执行搜索，只生成建议的查询任务）
     */
    @Transactional(readOnly = true)
    public com.nexusvoice.application.role.dto.RoleResearchTaskPreviewDto previewResearchTasks(Long conversationId, Long userId) {
        // 权限校验
        conversationDomainService.validateConversationAccess(conversationId, userId);

        RoleBriefDto draft = loadLatestBrief(conversationId)
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.DATA_NOT_FOUND, "未找到角色草稿，请先生成草稿"));

        List<String> queries = buildResearchQueries(draft);
        List<com.nexusvoice.application.role.dto.RoleResearchTaskDto> tasks = new ArrayList<>();
        int i = 1;
        for (String q : queries) {
            String rationale = i == 1 ? "补充风格与口吻示例" : (i == 2 ? "补充领域知识点" : "细化对话风格指南");
            tasks.add(com.nexusvoice.application.role.dto.RoleResearchTaskDto.builder()
                    .id("task-" + i)
                    .query(q)
                    .rationale(rationale)
                    .enabled(true)
                    .build());
            i++;
        }

        return com.nexusvoice.application.role.dto.RoleResearchTaskPreviewDto.builder()
                .tasks(tasks)
                .defaultLimit(12)
                .maxLimit(20)
                .build();
    }

    /**
     * 确认创建私人角色（可选深研增强）
     */
    @Transactional
    public RoleDTO confirmCreateRole(RoleAssistantConfirmRequest request, Long userId) {
        Long conversationId = request.getConversationId();
        conversationDomainService.validateConversationAccess(conversationId, userId);

        // 找到最近的ROLE_BRIEF元数据
        RoleBriefDto draft = loadLatestBrief(conversationId)
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.DATA_NOT_FOUND, "未找到角色草稿，请先生成草稿"));

        // 覆盖字段（如用户最后调整）
        if (request.getOverrideName() != null && !request.getOverrideName().isEmpty()) {
            draft.setName(request.getOverrideName());
        }
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            draft.setDescription(request.getDescription());
        }
        if (request.getPersonaPrompt() != null && !request.getPersonaPrompt().isEmpty()) {
            draft.setPersonaPrompt(request.getPersonaPrompt());
        }
        if (request.getGreetingMessage() != null && !request.getGreetingMessage().isEmpty()) {
            draft.setGreetingMessage(request.getGreetingMessage());
        }
        // 新参数：voiceType（前端直传，优先级最高）
        if (request.getVoiceType() != null && !request.getVoiceType().isEmpty()) {
            draft.setVoiceType(request.getVoiceType());
        } else if (request.getOverrideVoiceType() != null && !request.getOverrideVoiceType().isEmpty()) {
            draft.setVoiceType(request.getOverrideVoiceType());
        }
        // 新参数：avatarUrl（前端直传）
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
            draft.setAvatarUrl(request.getAvatarUrl());
        }

        RoleBriefDto finalBrief = draft;

        // 深研模式（可选，默认false；给出保守上限）
        if (Boolean.TRUE.equals(request.getDeepResearch())) {
            int limit = (request.getResearchLimit() != null && request.getResearchLimit() > 0)
                    ? Math.min(request.getResearchLimit(), 20) : 12; // 默认12，上限20
            List<String> overrideQueries = (request.getResearchQueries() != null && !request.getResearchQueries().isEmpty())
                    ? request.getResearchQueries() : null;
            
            // 使用Agent驱动的深研增强（新方式）
            try {
                finalBrief = deepResearchEnhanceWithAgent(draft, limit, userId, conversationId, overrideQueries);
                log.info("使用Agent成功完成深研增强");
            } catch (Exception e) {
                log.warn("Agent深研失败，降级使用传统方式：{}", e.getMessage());
                finalBrief = deepResearchEnhance(draft, limit, userId, conversationId, overrideQueries);
            }
        }

        // 转角色创建请求（私人）
        RoleCreateRequest createReq = new RoleCreateRequest();
        createReq.setName(safeStr(finalBrief.getName(), 50));
        createReq.setDescription(safeStr(finalBrief.getDescription(), 255));
        createReq.setPersonaPrompt(safeStr(finalBrief.getPersonaPrompt(), 2000));
        createReq.setGreetingMessage(safeStr(finalBrief.getGreetingMessage(), 255));
        createReq.setGreetingAudioUrl(null);
        createReq.setAvatarUrl(safeStr(finalBrief.getAvatarUrl(), 255));
        String resolvedVoiceType = (finalBrief.getVoiceType() != null && !finalBrief.getVoiceType().isEmpty())
                ? finalBrief.getVoiceType()
                : "qiniu_zh_female_dmytwz"; // 默认音色
        createReq.setVoiceType(safeStr(resolvedVoiceType, 50));

        RoleDTO created = roleApplicationService.createPrivateRole(userId, createReq);

        // 如果有开场白，则生成TTS音频并上传CDN，更新角色greeting_audio_url
        try {
            String greeting = finalBrief.getGreetingMessage();
            if (greeting != null && !greeting.trim().isEmpty()) {
                String cleaned = com.nexusvoice.utils.MarkdownTextUtils.cleanForTTS(greeting);
                com.nexusvoice.application.tts.dto.TTSRequestDTO ttsReq = new com.nexusvoice.application.tts.dto.TTSRequestDTO();
                ttsReq.setText(cleaned);
                ttsReq.setVoiceType(resolvedVoiceType);
                ttsReq.setEncoding("mp3");
                ttsReq.setSpeedRatio(1.0);
                com.nexusvoice.application.tts.dto.TTSResponseDTO ttsResp = ttsService.textToSpeech(ttsReq);
                String audioUrl = ttsResp != null ? ttsResp.getAudioData() : null;
                if (audioUrl != null && !audioUrl.isEmpty()) {
                    com.nexusvoice.application.role.dto.RoleUpdateRequest upd = new com.nexusvoice.application.role.dto.RoleUpdateRequest();
                    upd.setGreetingAudioUrl(audioUrl);
                    // 若最终voiceType与创建时不同（理论上不会），也一并更新
                    upd.setVoiceType(resolvedVoiceType);
                    roleApplicationService.updatePrivateRole(userId, created.getId(), upd);
                    created.setGreetingAudioUrl(audioUrl);
                    created.setVoiceType(resolvedVoiceType);
                }
            }
        } catch (Exception e) {
            log.warn("创建角色后生成开场白音频失败，将仅返回文本开场白。roleId={}, err={}", created.getId(), e.getMessage());
        }

        // 写入系统消息记录创建结果
        saveSystemNote(conversationId, "角色已创建：" + created.getName(), makeMetadata("ROLE_CREATED", toJson(finalBrief)));

        return created;
    }

    // ========================= 内部方法 =========================

    private List<String> toTranscript(List<ConversationMessage> history, int maxMessages, int maxPerMessage) {
        if (history == null || history.isEmpty()) return List.of();
        int start = Math.max(0, history.size() - maxMessages);
        List<String> lines = new ArrayList<>();
        for (int i = start; i < history.size(); i++) {
            ConversationMessage m = history.get(i);
            String role = m.getRole() != null ? m.getRole().name() : "UNKNOWN";
            String content = m.getContent() != null ? m.getContent() : "";
            if (content.length() > maxPerMessage) {
                content = content.substring(0, maxPerMessage) + "...";
            }
            lines.add(role + "：" + content);
        }
        return lines;
    }

    private ChatResponse executeBriefChatWithRetry(AiChatService chatService,
                                                   ChatRequest request,
                                                   Long conversationId,
                                                   Long userId) {
        ChatResponse lastResponse = null;

        for (int attempt = 1; attempt <= BRIEF_GENERATION_MAX_ATTEMPTS; attempt++) {
            lastResponse = chatService.chat(request);

            if (lastResponse.getSuccess()) {
                if (attempt > 1) {
                    log.info("角色草稿生成重试成功，conversationId={}，userId={}，attempt={}",
                            conversationId, userId, attempt);
                }
                return lastResponse;
            }

            String errorMessage = lastResponse.getErrorMessage();
            if (!isRetryableBriefGenerationError(errorMessage) || attempt >= BRIEF_GENERATION_MAX_ATTEMPTS) {
                return lastResponse;
            }

            log.warn("角色草稿生成遇到可重试错误，准备重试。conversationId={}，userId={}，attempt={}，error={}",
                    conversationId, userId, attempt, errorMessage);
            sleepBeforeBriefRetry(attempt);
        }

        return lastResponse != null ? lastResponse : ChatResponse.error("角色草稿生成失败");
    }

    private String resolveRoleBriefModelKey() {
        String modelKey = systemConfigService.getDefaultAiModel();
        if (modelKey == null || modelKey.isBlank()) {
            return DEFAULT_BRIEF_MODEL_KEY;
        }
        if (!modelKey.contains(":")) {
            String provider = systemConfigService.getDefaultAiModelProvider();
            if (provider == null || provider.isBlank()) {
                provider = "openai";
            }
            modelKey = provider + ":" + modelKey;
        }
        return modelKey;
    }

    private ResolvedBriefChat resolveRoleBriefChat() {
        String modelKey = resolveRoleBriefModelKey();
        try {
            return new ResolvedBriefChat(modelBeanManager.getServiceByModelKey(modelKey), modelKey);
        } catch (Exception e) {
            if (!DEFAULT_BRIEF_MODEL_KEY.equals(modelKey)) {
                log.warn("角色草稿模型不可用，回退到默认模型：{} -> {}", modelKey, DEFAULT_BRIEF_MODEL_KEY, e);
                return new ResolvedBriefChat(modelBeanManager.getServiceByModelKey(DEFAULT_BRIEF_MODEL_KEY), DEFAULT_BRIEF_MODEL_KEY);
            }
            throw e;
        }
    }

    private boolean isRetryableBriefGenerationError(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return false;
        }

        String normalized = errorMessage.toLowerCase();
        return normalized.contains("service temporarily unavailable")
                || normalized.contains("upstream_error")
                || normalized.contains("rate limit")
                || normalized.contains("rate_limit")
                || normalized.contains("timeout")
                || normalized.contains("temporarily unavailable");
    }

    private void sleepBeforeBriefRetry(int attempt) {
        try {
            Thread.sleep(BRIEF_GENERATION_RETRY_DELAY_MS * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw BizException.of(ErrorCodeEnum.SYSTEM_ERROR, "生成角色草稿时重试被中断");
        }
    }

    private RoleBriefDto parseBriefJson(String text) {
        if (text == null || text.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.AI_RESPONSE_INVALID, "AI响应为空");
        }
        String json = extractFirstJson(text);
        try {
            return objectMapper.readValue(json, RoleBriefDto.class);
        } catch (Exception e) {
            log.warn("解析角色草稿JSON失败，尝试容错解析。原文: {}", text);
            try {
                JsonNode node = objectMapper.readTree(json);
                RoleBriefDto.RoleBriefDtoBuilder b = RoleBriefDto.builder();
                b.name(getText(node, "name"));
                b.description(getText(node, "description"));
                b.personaPrompt(getText(node, "personaPrompt"));
                b.greetingMessage(getText(node, "greetingMessage"));
                b.avatarUrl(getText(node, "avatarUrl"));
                b.voiceType(getText(node, "voiceType"));
                // sources
                List<RoleBriefDto.SourceItem> sources = new ArrayList<>();
                if (node.has("sources") && node.get("sources").isArray()) {
                    for (JsonNode s : node.get("sources")) {
                        sources.add(RoleBriefDto.SourceItem.builder()
                                .title(getText(s, "title"))
                                .url(getText(s, "url"))
                                .snippet(getText(s, "snippet"))
                                .build());
                    }
                }
                b.sources(sources);
                // disclaimers
                List<String> disclaimers = new ArrayList<>();
                if (node.has("disclaimers") && node.get("disclaimers").isArray()) {
                    for (JsonNode d : node.get("disclaimers")) {
                        disclaimers.add(d.asText(""));
                    }
                }
                b.disclaimers(disclaimers);
                return b.build();
            } catch (Exception ex) {
                throw BizException.of(ErrorCodeEnum.AI_RESPONSE_INVALID, "角色草稿解析失败");
            }
        }
    }

    private String extractFirstJson(String text) {
        int i = text.indexOf('{');
        int j = text.lastIndexOf('}');
        if (i >= 0 && j >= i) {
            return text.substring(i, j + 1);
        }
        return text.trim();
    }

    private String getText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText("") : "";
    }

    private void applyBriefDefaults(RoleBriefDto brief) {
        if (brief.getVoiceType() == null || brief.getVoiceType().isEmpty()) {
            brief.setVoiceType("default");
        }
        if (brief.getDisclaimers() == null || brief.getDisclaimers().isEmpty()) {
            brief.setDisclaimers(List.of("本角色仅为原创风格设定，不复刻具体IP"));
        }
        if (brief.getSources() == null) {
            brief.setSources(Collections.emptyList());
        }
    }

    private Optional<RoleBriefDto> loadLatestBrief(Long conversationId) {
        List<ConversationMessage> history = messageRepository.findByConversationIdOrderBySequence(conversationId);
        List<ConversationMessage> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        for (ConversationMessage m : reversed) {
            String metadata = m.getMetadata();
            if (metadata != null && metadata.contains("ROLE_BRIEF")) {
                try {
                    JsonNode node = objectMapper.readTree(metadata);
                    if (node.has("payload")) {
                        return Optional.of(objectMapper.readValue(node.get("payload").toString(), RoleBriefDto.class));
                    }
                } catch (Exception e) {
                    log.warn("解析ROLE_BRIEF元数据失败，messageId={}", m.getId());
                }
            }
        }
        return Optional.empty();
    }

    private RoleBriefDto deepResearchEnhance(RoleBriefDto draft, int limit, Long userId, Long conversationId) {
        return deepResearchEnhance(draft, limit, userId, conversationId, null);
    }

    /**
     * 使用Agent驱动的深研增强（新方式）
     */
    private RoleBriefDto deepResearchEnhanceWithAgent(RoleBriefDto draft, int limit, Long userId, Long conversationId, List<String> overrideQueries) {
        log.info("开始使用Agent进行深研增强，角色：{}", draft.getName());
        
        // 构造Agent任务描述
        String query = buildAgentResearchQuery(draft, limit, overrideQueries);
        
        // 构造Agent请求
        AgentExecuteRequest agentRequest = AgentExecuteRequest.builder()
            .query(query)
            .agentType(AgentType.PLAN_SOLVE)  // 使用planExecute模式
            .userId(userId)
            .conversationId(conversationId)
            .availableTools(List.of("web_search"))  // ⚠️ 关键：深研只用搜索，不用role_draft_generator
            .maxSteps(5)  // 深研最多5步
            .temperature(0.4)  // 较低温度，保持稳定
            .contextVariables(new java.util.HashMap<String, Object>() {{
                put("originalDraft", toJson(draft));
                put("researchLimit", limit);
                put("purpose", "role_research");
            }})
            .build();
        
        // 执行Agent
        AgentExecuteResponse response = agentApplicationService.executeTask(agentRequest);
        
        if (!response.getSuccess() || response.getResult() == null) {
            throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                "Agent深研失败：" + response.getErrorMessage());
        }
        
        // 解析Agent返回的增强草稿
        RoleBriefDto enhancedBrief = parseEnhancedBriefFromAgent(response.getResult(), draft);
        
        // 记录使用的工具
        if (response.getUsedTools() != null && !response.getUsedTools().isEmpty()) {
            log.info("Agent使用了工具：{}", String.join(", ", response.getUsedTools()));
        }
        
        // 记录深研结果
        saveSystemNote(conversationId, 
            String.format("Agent深研完成（步数：%d，耗时：%dms）", response.getSteps(), response.getTotalTimeMs()),
            makeMetadata("AGENT_RESEARCH", toJson(new java.util.HashMap<String, Object>() {{
                put("steps", response.getSteps());
                put("usedTools", response.getUsedTools());
                put("executionHistory", response.getExecutionHistory());
            }}))
        );
        
        return enhancedBrief;
    }
    
    /**
     * 构造Agent研究任务描述（针对DeepSeek V3.1优化）
     */
    private String buildAgentResearchQuery(RoleBriefDto draft, int limit, List<String> overrideQueries) {
        StringBuilder query = new StringBuilder();
        
        query.append("【角色深度研究任务】\n\n");
        
        query.append("📋 当前草稿：\n");
        query.append("━━━━━━━━━━━━━━━━━━━━━━━━\n");
        query.append("角色名称：").append(draft.getName()).append("\n");
        query.append("角色描述：").append(draft.getDescription()).append("\n\n");
        query.append("现有人设：\n").append(draft.getPersonaPrompt()).append("\n");
        query.append("━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        query.append("🎯 研究目标：\n");
        query.append("通过联网搜索，获取相关信息，增强角色的人设提示词和开场白，使其更生动、更专业、更有趣。\n\n");
        
        query.append("🔍 搜索策略：\n");
        query.append("1. 使用web_search工具搜索").append(limit).append("条相关信息\n");
        
        if (overrideQueries != null && !overrideQueries.isEmpty()) {
            query.append("2. 建议搜索关键词（可调整）：\n");
            for (int i = 0; i < overrideQueries.size(); i++) {
                query.append("   ").append(i + 1).append(") ").append(overrideQueries.get(i)).append("\n");
            }
        } else {
            query.append("2. 自动生成搜索关键词，建议包括：\n");
            query.append("   • 角色风格、说话特点、经典用语\n");
            query.append("   • 相关领域的知识点、术语\n");
            query.append("   • 类似角色的对话技巧、互动方式\n");
        }
        query.append("3. 从搜索结果中提取有价值的内容\n");
        query.append("4. 记录参考来源（sources字段）\n\n");
        
        query.append("✨ 增强要求：\n");
        query.append("• personaPrompt增强：\n");
        query.append("  - 补充更具体的说话风格示例\n");
        query.append("  - 丰富知识背景和专业度\n");
        query.append("  - 增加行为准则和边界说明\n");
        query.append("  - 保持原有角色定位，不偏离\n\n");
        
        query.append("• greetingMessage增强：\n");
        query.append("  - 更生动、更有角色特色\n");
        query.append("  - 友好且引导性强\n");
        query.append("  - 体现角色个性\n\n");
        
        query.append("• disclaimers完善：\n");
        query.append("  - 版权声明（避免侵权）\n");
        query.append("  - 内容边界（禁止话题）\n");
        query.append("  - 免责说明（娱乐用途）\n\n");
        
        query.append("⚠️ 重要约束：\n");
        query.append("- 避免使用具体IP、影视、小说的专有名词\n");
        query.append("- 保持抽象的风格和特征描述\n");
        query.append("- 合法合规，不涉及敏感内容\n\n");
        
        query.append("📤 输出格式：\n");
        query.append("```json\n");
        query.append("{\n");
        query.append("  \"name\": \"角色名称\",\n");
        query.append("  \"description\": \"角色描述\",\n");
        query.append("  \"personaPrompt\": \"增强后的详细人设\",\n");
        query.append("  \"greetingMessage\": \"增强后的开场白\",\n");
        query.append("  \"voiceType\": \"qiniu_zh_female_dmytwz\",\n");
        query.append("  \"sources\": [{\"title\": \"参考来源标题\", \"url\": \"链接\", \"snippet\": \"摘要\"}],\n");
        query.append("  \"disclaimers\": [\"版权声明\", \"内容边界说明\", \"免责声明\"]\n");
        query.append("}\n");
        query.append("```\n\n");
        
        query.append("请开始执行深度研究任务！\n");
        
        return query.toString();
    }
    
    /**
     * 从Agent结果中解析增强后的草稿
     */
    private RoleBriefDto parseEnhancedBriefFromAgent(String agentResult, RoleBriefDto originalDraft) {
        try {
            // 提取JSON（Agent的回答中可能包含其他文字）
            String json = extractJsonFromText(agentResult);
            
            RoleBriefDto enhanced = objectMapper.readValue(json, RoleBriefDto.class);
            
            // 应用默认值
            applyBriefDefaults(enhanced);
            
            // 合并来源（保留原始+新增）
            if (originalDraft.getSources() != null) {
                List<RoleBriefDto.SourceItem> merged = new ArrayList<>(originalDraft.getSources());
                if (enhanced.getSources() != null) {
                    merged.addAll(enhanced.getSources());
                }
                enhanced.setSources(merged);
            }
            
            return enhanced;
            
        } catch (Exception e) {
            log.error("解析Agent返回的增强草稿失败：{}", agentResult, e);
            throw BizException.of(ErrorCodeEnum.AI_SERVICE_ERROR, "解析增强结果失败");
        }
    }
    
    /**
     * 从文本中提取JSON
     */
    private String extractJsonFromText(String text) {
        // 尝试提取 ```json ... ``` 或 ``` ... ``` 代码块
        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        } else if (text.contains("```")) {
            int start = text.indexOf("```") + 3;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }
        
        // 尝试提取 { ... } JSON对象
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end > start) {
            return text.substring(start, end + 1).trim();
        }
        
        // 直接返回原文本
        return text.trim();
    }
    
    /**
     * 传统深研增强方法（保留作为降级方案）
     */
    private RoleBriefDto deepResearchEnhance(RoleBriefDto draft, int limit, Long userId, Long conversationId, List<String> overrideQueries) {
        // 生成若干查询词（保守、泛化，避免指向具体版权内容）
        List<String> queries = (overrideQueries != null && !overrideQueries.isEmpty()) ? overrideQueries : buildResearchQueries(draft);

        List<SearchResult.SearchItem> items = new ArrayList<>();
        for (String q : queries) {
            SearchResult r = searchRepository.searchWeb(q, Math.min(4, limit), "zh-CN");
            if (r.getItems() != null) items.addAll(r.getItems());
            if (items.size() >= limit) break;
        }

        // 组装来源摘要
        StringBuilder src = new StringBuilder();
        int count = 0;
        for (SearchResult.SearchItem it : items) {
            src.append(++count).append(". ")
               .append(nonNull(it.getTitle())).append("\n")
               .append(nonNull(it.getSnippet())).append("\n")
               .append("来源：").append(nonNull(it.getLink())).append("\n\n");
            if (src.length() > 3000) break;
        }

        // 调用AI对草稿进行增强（限制温度与长度，保持稳健）
        String system = "你是角色研究助手。在不抄袭、仅保留抽象风格的前提下，根据参考资料优化以下角色草稿。" +
                "保持名称、语气与边界的合理性，不使用具体IP专有名词。输出一个JSON对象，字段同前：" +
                "name, description, personaPrompt, greetingMessage, avatarUrl, voiceType, sources, disclaimers。";
        String user = "原始草稿：\n" + toJson(draft) + "\n\n参考资料：\n" + src;

        ResolvedBriefChat resolvedBriefChat = resolveRoleBriefChat();

        ChatRequest enhanceReq = ChatRequest.builder()
                .messages(List.of(ChatMessage.system(system), ChatMessage.user(user)))
                .model(resolvedBriefChat.modelKey())
                .temperature(0.4)
                .maxTokens(1400)
                .userId(userId)
                .conversationId(conversationId)
                .enableWebSearch(false) // 已提供来源摘要，无需再联网
                .build();

        ChatResponse enhanced = resolvedBriefChat.chatService().chat(enhanceReq);
        if (!enhanced.getSuccess()) {
            log.warn("深研增强失败，降级使用原草稿：{}", enhanced.getErrorMessage());
            return draft;
        }
        RoleBriefDto brief = parseBriefJson(enhanced.getContent());
        applyBriefDefaults(brief);

        // 合并来源（保留可追溯性）
        List<RoleBriefDto.SourceItem> merged = new ArrayList<>();
        if (draft.getSources() != null) merged.addAll(draft.getSources());
        if (brief.getSources() != null) merged.addAll(brief.getSources());
        // 去重（按url）
        merged = merged.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(s -> nonNull(s.getUrl()), s -> s, (a, b) -> a),
                        m -> m.values().stream().collect(Collectors.toList())
                ));
        brief.setSources(merged);

        // 写入系统消息记录深研小结
        saveSystemNote(conversationId, "已完成深研增强（条目" + Math.min(items.size(), limit) + ")", makeMetadata("ROLE_RESEARCH", src.toString()));
        return brief;
    }

    /**
     * 应用深研任务并仅更新草稿（不创建角色）
     */
    @Transactional
    public RoleBriefDto applyResearchAndUpdateBrief(RoleResearchApplyRequest request, Long userId) {
        Long conversationId = request.getConversationId();
        conversationDomainService.validateConversationAccess(conversationId, userId);

        RoleBriefDto draft = loadLatestBrief(conversationId)
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.DATA_NOT_FOUND, "未找到角色草稿，请先生成草稿"));

        int limit = (request.getResearchLimit() != null && request.getResearchLimit() > 0)
                ? Math.min(request.getResearchLimit(), 20) : 12;
        List<String> queries = (request.getResearchQueries() != null && !request.getResearchQueries().isEmpty())
                ? request.getResearchQueries() : buildResearchQueries(draft);

        RoleBriefDto newBrief = deepResearchEnhance(draft, limit, userId, conversationId, queries);

        // 将新的Brief写入系统消息，作为最新草稿
        saveSystemNote(conversationId, "角色草稿已更新（深研结果已合并）", makeMetadata("ROLE_BRIEF", toJson(newBrief)));

        return newBrief;
    }

    private List<String> buildResearchQueries(RoleBriefDto draft) {
        String base = draft.getName() != null && !draft.getName().isEmpty() ? draft.getName() : "AI 人设风格";
        List<String> qs = new ArrayList<>();
        qs.add(base + " 风格 特点 写作 口吻 示例");
        qs.add(base + " 领域 知识 点 概要");
        qs.add("对话 风格 指南 中文 实用");
        return qs;
    }

    private void saveSystemNote(Long conversationId, String content, String metadata) {
        // 使用领域服务确保顺序号与对话活跃时间正确更新
        ConversationMessage sys = new ConversationMessage();
        sys.setConversationId(conversationId);
        sys.setRole(com.nexusvoice.domain.conversation.constant.MessageRole.SYSTEM);
        sys.setContent(content);
        sys.setStatus("sent");
        sys.setSentAt(LocalDateTime.now());
        sys.setMetadata(metadata);
        conversationDomainService.addMessageToConversation(conversationId, sys);
    }
    
    private String makeMetadata(String type, String payload) {
        try {
            com.fasterxml.jackson.databind.node.ObjectNode node = objectMapper.createObjectNode();
            node.put("type", type);
            node.put("version", "1.0");
            node.put("timestamp", System.currentTimeMillis());
            
            // 先验证payload是否为有效JSON，如果不是则作为字符串处理
            try {
                node.set("payload", objectMapper.readTree(payload));
            } catch (JsonProcessingException jsonError) {
                log.warn("payload不是有效JSON，作为字符串处理：{}", jsonError.getMessage());
                // 清理payload中的无效字符
                String cleanPayload = cleanJsonString(payload);
                node.put("payload", cleanPayload);
            }
            return node.toString();
        } catch (Exception e) {
            log.error("创建metadata失败：{}", e.getMessage(), e);
            // 回退为安全的简单格式
            String cleanType = cleanJsonString(type);
            String cleanPayload = cleanJsonString(payload);
            return "{\"type\":\"" + cleanType + "\",\"payload\":\"" + cleanPayload + "\"}";
        }
    }
    
    /**
     * 清理字符串中的无效字符，确保JSON安全
     */
    private String cleanJsonString(String input) {
        if (input == null) return "";
        
        // 移除控制字符和无效的Unicode字符
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                // 跳过控制字符（除了tab、换行、回车）
                continue;
            }
            // 转义特殊JSON字符
            if (c == '"') {
                cleaned.append("\\\"");
            } else if (c == '\\') {
                cleaned.append("\\\\");
            } else if (c == '\b') {
                cleaned.append("\\b");
            } else if (c == '\f') {
                cleaned.append("\\f");
            } else if (c == '\n') {
                cleaned.append("\\n");
            } else if (c == '\r') {
                cleaned.append("\\r");
            } else if (c == '\t') {
                cleaned.append("\\t");
            } else {
                cleaned.append(c);
            }
        }
        return cleaned.toString();
    }


    private String toJson(Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            // 验证生成的JSON是否有效
            objectMapper.readTree(json); // 如果无效会抛异常
            return json;
        } catch (Exception e) {
            log.warn("对象JSON序列化失败，使用空对象：{}", e.getMessage());
            return "{}";
        }
    }

    private String nonNull(String s) { return s == null ? "" : s; }

    private String safeStr(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }
}
