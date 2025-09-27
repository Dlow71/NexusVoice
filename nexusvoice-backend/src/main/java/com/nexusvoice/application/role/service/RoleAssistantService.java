package com.nexusvoice.application.role.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.application.role.assembler.RoleAssembler;
import com.nexusvoice.application.role.dto.RoleBriefDto;
import com.nexusvoice.application.role.dto.RoleCreateRequest;
import com.nexusvoice.application.role.dto.RoleDTO;
import com.nexusvoice.application.role.dto.RoleAssistantConfirmRequest;
import com.nexusvoice.application.role.dto.RoleResearchApplyRequest;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.repository.ConversationMessageRepository;
import com.nexusvoice.domain.conversation.repository.ConversationRepository;
import com.nexusvoice.domain.conversation.service.ConversationDomainService;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
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

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ConversationDomainService conversationDomainService;
    private final AiChatService aiChatService;
    private final RoleApplicationService roleApplicationService;
    private final com.nexusvoice.application.tts.service.TTSService ttsService;
    private final SearchRepository searchRepository;
    private final ObjectMapper objectMapper;

    public RoleAssistantService(ConversationRepository conversationRepository,
                                ConversationMessageRepository messageRepository,
                                ConversationDomainService conversationDomainService,
                                AiChatService aiChatService,
                                RoleApplicationService roleApplicationService,
                                com.nexusvoice.application.tts.service.TTSService ttsService,
                                SearchRepository searchRepository,
                                ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationDomainService = conversationDomainService;
        this.aiChatService = aiChatService;
        this.roleApplicationService = roleApplicationService;
        this.ttsService = ttsService;
        this.searchRepository = searchRepository;
        this.objectMapper = objectMapper;
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

        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .model(aiChatService.getModelName())
                .temperature(0.5)
                .maxTokens(1200)
                .userId(userId)
                .conversationId(conversationId)
                .enableWebSearch(enableWebSearch)
                .build();

        ChatResponse response = aiChatService.chat(request);
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
        // 新参数：voiceType（前端直传，优先级最高）
        if (request.getVoiceType() != null && !request.getVoiceType().isEmpty()) {
            draft.setVoiceType(request.getVoiceType());
        } else if (request.getOverrideVoiceType() != null && !request.getOverrideVoiceType().isEmpty()) {
            draft.setVoiceType(request.getOverrideVoiceType());
        }

        RoleBriefDto finalBrief = draft;

        // 深研模式（可选，默认false；给出保守上限）
        if (Boolean.TRUE.equals(request.getDeepResearch())) {
            int limit = (request.getResearchLimit() != null && request.getResearchLimit() > 0)
                    ? Math.min(request.getResearchLimit(), 20) : 12; // 默认12，上限20
            List<String> overrideQueries = (request.getResearchQueries() != null && !request.getResearchQueries().isEmpty())
                    ? request.getResearchQueries() : null;
            finalBrief = deepResearchEnhance(draft, limit, userId, conversationId, overrideQueries);
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

        ChatRequest enhanceReq = ChatRequest.builder()
                .messages(List.of(ChatMessage.system(system), ChatMessage.user(user)))
                .model(aiChatService.getModelName())
                .temperature(0.4)
                .maxTokens(1400)
                .userId(userId)
                .conversationId(conversationId)
                .enableWebSearch(false) // 已提供来源摘要，无需再联网
                .build();

        ChatResponse enhanced = aiChatService.chat(enhanceReq);
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
        ConversationMessage sys = ConversationMessage.builder()
                .conversationId(conversationId)
                .role(com.nexusvoice.domain.conversation.constant.MessageRole.SYSTEM)
                .content(content)
                .status("sent")
                .sentAt(LocalDateTime.now())
                .build();
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
