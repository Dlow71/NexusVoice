package com.nexusvoice.interfaces.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.application.conversation.dto.ChatRequestDto;
import com.nexusvoice.application.conversation.service.ConversationApplicationService;
import com.nexusvoice.application.tts.dto.TTSRequestDTO;
import com.nexusvoice.application.tts.dto.TTSResponseDTO;
import com.nexusvoice.application.tts.service.TTSService;
import com.nexusvoice.application.role.service.RoleApplicationService;
import com.nexusvoice.domain.conversation.model.Conversation;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.repository.ConversationRepository;
import com.nexusvoice.domain.conversation.repository.ConversationMessageRepository;
import com.nexusvoice.domain.conversation.service.ConversationDomainService;
import com.nexusvoice.domain.role.model.Role;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.domain.config.repository.SystemConfigRepository;
import com.nexusvoice.domain.config.model.SystemConfig;
import com.nexusvoice.utils.MarkdownTextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 流式聊天WebSocket处理器
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Slf4j
@Component
public class ChatStreamHandler implements WebSocketHandler {

    private final AiChatService aiChatService;
    private final ConversationApplicationService conversationApplicationService;
    private final ConversationRepository conversationRepository;
    private final ConversationDomainService conversationDomainService;
    private final ConversationMessageRepository conversationMessageRepository;
    private final RoleApplicationService roleApplicationService;
    private final TTSService ttsService;
    private final SystemConfigRepository systemConfigRepository;
    private final ObjectMapper objectMapper;
    
    // 存储会话信息
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    // 单flight保护：同一Session同一时间仅处理一个请求
    private final ConcurrentMap<String, Boolean> streamingSessions = new ConcurrentHashMap<>();
    
    public ChatStreamHandler(AiChatService aiChatService,
                           ConversationApplicationService conversationApplicationService,
                           ConversationRepository conversationRepository,
                           ConversationDomainService conversationDomainService,
                           ConversationMessageRepository conversationMessageRepository,
                           RoleApplicationService roleApplicationService,
                           TTSService ttsService,
                           SystemConfigRepository systemConfigRepository,
                           ObjectMapper objectMapper) {
        this.aiChatService = aiChatService;
        this.conversationApplicationService = conversationApplicationService;
        this.conversationRepository = conversationRepository;
        this.conversationDomainService = conversationDomainService;
        this.conversationMessageRepository = conversationMessageRepository;
        this.roleApplicationService = roleApplicationService;
        this.ttsService = ttsService;
        this.systemConfigRepository = systemConfigRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        
        // 获取用户信息（已通过JWT认证拦截器验证）
        Long userId = getUserIdFromSession(session);
        String username = getUsernameFromSession(session);
        String roles = getUserRolesFromSession(session);
        
        log.info("WebSocket连接建立，会话ID：{}，用户ID：{}，用户名：{}，角色：{}", 
                sessionId, userId, username, roles);
        
        // 发送连接成功消息
        sendMessage(session, StreamChatResponse.builder()
                .type(StreamChatResponse.StreamMessageType.START)
                .delta("连接成功，欢迎 " + username + "！")
                .build());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        
        try {
            // 解析请求消息
            String payload = (String) message.getPayload();
            ChatRequestDto requestDto = objectMapper.readValue(payload, ChatRequestDto.class);
            
            log.info("收到流式聊天请求，会话ID：{}，对话ID：{}", sessionId, requestDto.getConversationId());
            
            // 从WebSocket会话属性中获取用户ID（已通过JWT认证拦截器验证）
            Long userId = getUserIdFromSession(session);
            if (userId == null) {
                sendErrorMessage(session, "用户未认证，请重新连接");
                return;
            }

            // 单flight保护（由system_config控制开关）
            boolean singleFlightEnabled = getBooleanConfig("websocket.single_flight.enabled", true);
            if (singleFlightEnabled) {
                Boolean prev = streamingSessions.putIfAbsent(sessionId, Boolean.TRUE);
                if (prev != null) {
                    sendErrorMessage(session, "上一个请求仍在处理中，请稍后再试");
                    return;
                }
            }
            
            // 处理流式聊天
            handleStreamChat(session, requestDto, userId, singleFlightEnabled);
            
        } catch (Exception e) {
            log.error("处理WebSocket消息失败，会话ID：{}", sessionId, e);
            sendErrorMessage(session, "处理消息失败：" + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("WebSocket传输错误，会话ID：{}", sessionId, exception);
        
        sendErrorMessage(session, "连接出现错误：" + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        activeSessions.remove(sessionId);
        // 清理单flight标记，避免异常断开导致残留
        streamingSessions.remove(sessionId);
        
        log.info("WebSocket连接关闭，会话ID：{}，状态：{}", sessionId, closeStatus);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 处理流式聊天
     */
    private void handleStreamChat(WebSocketSession session, ChatRequestDto requestDto, Long userId, boolean singleFlightEnabled) {
        try {
            long startTime = System.currentTimeMillis();
            String sessionId = session.getId();
            // 1. 获取或创建对话
            Conversation conversation = getOrCreateConversation(requestDto, userId);
            
            // 2. 验证权限和限制
            conversationDomainService.validateConversationAccess(conversation.getId(), userId);
            conversationDomainService.checkMessageCountLimit(conversation.getId(), 100);
            conversationDomainService.checkTokenLimit(conversation.getId(), 50000);

            // 2.1 解析角色（与HTTP一致）
            Role role = null;
            Long effectiveRoleId = requestDto.getRoleId() != null ? requestDto.getRoleId() : conversation.getRoleId();
            if (effectiveRoleId != null) {
                try {
                    role = roleApplicationService.getRoleForChat(effectiveRoleId, userId);
                    log.info("WS使用角色进行聊天，角色ID：{}，角色名称：{}", role.getId(), role.getName());
                } catch (Exception e) {
                    log.warn("WS获取角色信息失败，角色ID：{}，用户ID：{}，错误：{}", effectiveRoleId, userId, e.getMessage());
                }
            }
            
            // 3. 保存用户消息
            ConversationMessage userMessage = ConversationMessage.createUserMessage(
                    conversation.getId(), 
                    requestDto.getMessage(),
                    null
            );
            conversationDomainService.addMessageToConversation(conversation.getId(), userMessage);
            
            // 4. 构建AI请求
            ChatRequest aiRequest = buildStreamAiRequest(conversation, requestDto, role);
            // 注意：lambda中引用的本地变量需要是final或有效final，这里固定一份快照供后续lambda使用
            final Role roleSnapshot = role;
            
            // 5. 开始流式响应
            StringBuilder responseContent = new StringBuilder();
            final boolean[] receivedEnd = {false};

            // 是否启用“分段同步TTS”模式（当 enableAudio=true 时启用）
            final boolean segmentedTtsEnabled = requestDto.getEnableAudio() != null && requestDto.getEnableAudio();
            final String ttsGroupId = segmentedTtsEnabled ? UUID.randomUUID().toString() : null;
            final String modelName = aiRequest.getModel();
            final String selectedVoiceType = (roleSnapshot != null && roleSnapshot.getVoiceType() != null && !roleSnapshot.getVoiceType().trim().isEmpty())
                    ? roleSnapshot.getVoiceType().trim()
                    : "qiniu_zh_female_wwxkjx";

            // 分段参数（可后续接入系统配置）
            final int firstMinChars = getIntConfig("websocket.tts.segment.first_min_chars", 300, 100, 1000);
            final int minChars = getIntConfig("websocket.tts.segment.min_chars", 160, 60, 600);
            final int maxChars = getIntConfig("websocket.tts.segment.max_chars", 220, 80, 800);
            final int firstGateMs = getIntConfig("websocket.tts.first_gate_ms", 1500, 200, 5000);
            final int ttsMaxConcurrency = getIntConfig("websocket.tts.max_concurrency", 2, 1, 8);
            final int heartbeatMs = getIntConfig("websocket.stream.heartbeat.ms", 5000, 1000, 60000);
            final boolean lateUpdateEnabled = getBooleanConfig("websocket.tts.update_on_late_audio", true);

            // 分段器，仅在启用分段TTS时创建
            final SegmentAggregator aggregator = segmentedTtsEnabled
                    ? new SegmentAggregator(ttsGroupId, selectedVoiceType, modelName, firstMinChars, minChars, maxChars, firstGateMs, ttsMaxConcurrency, heartbeatMs, lateUpdateEnabled,
                    (segText) -> responseContent.append(segText),
                    (resp) -> sendMessage(session, resp))
                    : null;

            aiChatService.streamChat(aiRequest,
                    // onNext - 处理流式数据
                    (streamResponse) -> {
                        try {
                            if (streamResponse.getType() == StreamChatResponse.StreamMessageType.END) {
                                receivedEnd[0] = true;
                                return;
                            }
                            if (streamResponse.getType() == StreamChatResponse.StreamMessageType.START) {
                                // 模型开始事件直接转发
                                sendMessage(session, streamResponse);
                                return;
                            }
                            if (streamResponse.getType() == StreamChatResponse.StreamMessageType.CONTENT && streamResponse.getDelta() != null) {
                                String delta = streamResponse.getDelta();
                                if (segmentedTtsEnabled && aggregator != null) {
                                    aggregator.onDelta(delta);
                                } else {
                                    responseContent.append(delta);
                                    sendMessage(session, streamResponse);
                                }
                            }
                        } catch (Exception e) {
                            log.error("发送流式响应失败", e);
                        }
                    },
                    // onError - 处理错误
                    (error) -> {
                        log.error("流式聊天出错，对话ID：{}", conversation.getId(), error);
                        sendErrorMessage(session, "AI响应出错：" + error.getMessage());
                        if (singleFlightEnabled) {
                            streamingSessions.remove(sessionId);
                        }
                    },
                    // onComplete - 完成处理
                    () -> {
                        try {
                            CompletableFuture<Void> segmentsDone;
                            if (segmentedTtsEnabled && aggregator != null) {
                                segmentsDone = aggregator.finish();
                            } else {
                                segmentsDone = CompletableFuture.completedFuture(null);
                            }

                            // 等待分段全部发送完成后再落库并发送END
                            segmentsDone.whenComplete((v, ex) -> {
                                try {
                                    if (ex != null) {
                                        log.warn("分段TTS完成阶段出现异常：{}", ex.getMessage());
                                    }
                                    if (responseContent.length() > 0) {
                                        ConversationMessage aiMessage = ConversationMessage.createAssistantMessage(
                                                conversation.getId(),
                                                responseContent.toString(),
                                                null,
                                                null // 分段TTS场景不设置整段audioUrl
                                        );
                                        conversationDomainService.addMessageToConversation(conversation.getId(), aiMessage);

                                        log.info("流式聊天完成（{}），对话ID：{}，文本长度：{}",
                                                segmentedTtsEnabled ? "分段TTS" : "纯文本", conversation.getId(), responseContent.length());

                                        // 自动更新标题（与HTTP一致）
                                        if (conversation.getTitle() == null || "新对话".equals(conversation.getTitle())) {
                                            try {
                                                String generatedTitle = conversationDomainService.generateConversationTitle(conversation.getId());
                                                conversation.updateTitle(generatedTitle);
                                                conversationRepository.save(conversation);
                                            } catch (Exception titleEx) {
                                                log.warn("WS自动更新会话标题失败，对话ID：{}，错误：{}", conversation.getId(), titleEx.getMessage());
                                            }
                                        }

                                        // 统一发送END，附带元数据
                                        StreamChatResponse endResp = StreamChatResponse.end("stop");
                                        endResp.setConversationId(conversation.getId());
                                        endResp.setMessageId(aiMessage.getId());
                                        endResp.setModel(aiRequest.getModel());
                                        endResp.setResponseTimeMs(System.currentTimeMillis() - startTime);
                                        if (segmentedTtsEnabled) {
                                            endResp.setTtsGroupId(ttsGroupId);
                                            endResp.setTtsChunked(true);
                                        }
                                        sendMessage(session, endResp);
                                    }
                                } catch (Exception ee) {
                                    log.error("保存AI回复消息失败", ee);
                                } finally {
                                    if (singleFlightEnabled) {
                                        streamingSessions.remove(sessionId);
                                    }
                                }
                            });
                        } catch (Exception e) {
                            log.error("流式完成阶段处理失败", e);
                            if (singleFlightEnabled) {
                                streamingSessions.remove(sessionId);
                            }
                        }
                    }
            );
            
        } catch (BizException e) {
            log.error("流式聊天业务异常，用户ID：{}", userId, e);
            sendErrorMessage(session, e.getMessage());
            if (singleFlightEnabled) {
                streamingSessions.remove(session.getId());
            }
        } catch (Exception e) {
            log.error("流式聊天系统异常，用户ID：{}", userId, e);
            sendErrorMessage(session, "系统繁忙，请稍后重试");
            if (singleFlightEnabled) {
                streamingSessions.remove(session.getId());
            }
        }
    }

    /**
     * 简单读取整数配置
     */
    private int getIntConfig(String key, int defaultVal, int min, int max) {
        try {
            return systemConfigRepository.findByKey(key)
                    .filter(SystemConfig::getEnabled)
                    .map(SystemConfig::getConfigValue)
                    .map(v -> {
                        try {
                            int n = Integer.parseInt(v.trim());
                            if (n < min) return min;
                            if (n > max) return max;
                            return n;
                        } catch (Exception e) {
                            return defaultVal;
                        }
                    })
                    .orElse(defaultVal);
        } catch (Exception e) {
            log.warn("读取整型配置失败，key={}，使用默认值 {}", key, defaultVal, e);
            return defaultVal;
        }
    }

    /**
     * 分段聚合器：按阈值切分文本、并发TTS并按序发送TTS_SEGMENT
     */
    private class SegmentAggregator {
        private final String groupId;
        private final String voiceType;
        private final String model;
        private final int firstMinChars;
        private final int minChars;
        private final int maxChars;
        private final int firstGateMs;
        private final int heartbeatMs;
        private final boolean lateUpdate;
        private final Semaphore permits;
        private final Consumer<String> appendTotal;
        private final Consumer<StreamChatResponse> sender;

        private final StringBuilder buf = new StringBuilder();
        private final Map<Integer, String> segText = new ConcurrentHashMap<>();
        private final Map<Integer, String> segAudio = new ConcurrentHashMap<>();
        private final Map<Integer, Boolean> audioDelivered = new ConcurrentHashMap<>();
        private volatile int nextIndex = 0;
        private volatile int produced = 0;
        private volatile boolean finished = false;
        private final CompletableFuture<Void> done = new CompletableFuture<>();
        private volatile boolean heartbeatRunning = false;
        private CompletableFuture<?> heartbeatTask;

        SegmentAggregator(String groupId, String voiceType, String model, int firstMinChars, int minChars, int maxChars, int firstGateMs, int concurrency, int heartbeatMs, boolean lateUpdate,
                          Consumer<String> appendTotal, Consumer<StreamChatResponse> sender) {
            this.groupId = groupId;
            this.voiceType = voiceType;
            this.model = model;
            this.firstMinChars = firstMinChars;
            this.minChars = minChars;
            this.maxChars = maxChars;
            this.firstGateMs = firstGateMs;
            this.heartbeatMs = heartbeatMs;
            this.lateUpdate = lateUpdate;
            this.permits = new Semaphore(concurrency);
            this.appendTotal = appendTotal;
            this.sender = sender;
        }

        void onDelta(String delta) {
            if (delta == null || delta.isEmpty()) return;
            synchronized (buf) {
                buf.append(delta);
                tryCut();
            }
        }

        CompletableFuture<Void> finish() {
            synchronized (buf) {
                if (buf.length() > 0) {
                    String text = buf.toString();
                    buf.setLength(0);
                    scheduleSegment(text, produced++);
                }
                finished = true;
            }
            // 尝试在完成后刷新
            flush();
            return done;
        }

        private void tryCut() {
            while (true) {
                int need = (produced == 0) ? firstMinChars : minChars;
                if (buf.length() < need) return;
                int cut = findCutIndex(buf, need, maxChars);
                if (cut <= 0) return;
                String text = buf.substring(0, cut);
                buf.delete(0, cut);
                scheduleSegment(text, produced++);
            }
        }

        private void scheduleSegment(String text, int index) {
            segText.put(index, text);
            appendTotal.accept(text);
            if (index == 0) {
                startHeartbeat();
                // 首段fallback：超过firstGateMs仍未获取音频，先释放文本段
                CompletableFuture.delayedExecutor(firstGateMs, TimeUnit.MILLISECONDS).execute(() -> {
                    if (!segAudio.containsKey(0)) {
                        segAudio.put(0, null);
                        flush();
                    }
                });
            }

            CompletableFuture.runAsync(() -> {
                boolean acquired = false;
                try {
                    permits.acquire();
                    acquired = true;
                    String cleaned = MarkdownTextUtils.cleanForTTS(text);
                    TTSRequestDTO ttsReq = new TTSRequestDTO();
                    ttsReq.setText(cleaned);
                    ttsReq.setVoiceType(voiceType);
                    ttsReq.setEncoding("mp3");
                    ttsReq.setSpeedRatio(1.0);
                    TTSResponseDTO res = ttsService.textToSpeech(ttsReq);
                    String url = (res != null) ? res.getAudioData() : null;
                    segAudio.put(index, url);
                    // 若此前已经发送过该段文本且未带音频，则补发音频更新
                    maybeSendLateUpdate(index, url);
                } catch (Exception e) {
                    log.warn("分段TTS失败：index={}，错误：{}", index, e.getMessage());
                    segAudio.put(index, null);
                } finally {
                    if (acquired) permits.release();
                    flush();
                }
            });
        }

        private void flush() {
            // 首段同步门：只有当 index=0 有音频（或超时）才发
            if (nextIndex == 0 && produced > 0 && !segAudio.containsKey(0)) {
                return;
            }
            while (segText.containsKey(nextIndex)) {
                // 对于非首段，允许音频为空（失败也发文本）
                if (nextIndex == 0 && !segAudio.containsKey(0)) break;
                String text = segText.remove(nextIndex);
                String audio = segAudio.getOrDefault(nextIndex, null);
                StreamChatResponse seg = StreamChatResponse.ttsSegment(groupId, nextIndex, text, audio, model);
                try {
                    sender.accept(seg);
                } catch (Exception e) {
                    log.warn("发送TTS_SEGMENT失败：index={}，错误：{}", nextIndex, e.getMessage());
                }
                audioDelivered.put(nextIndex, audio != null);
                nextIndex++;
            }
            // 如果已完成且全部发送，标记done
            if (finished && nextIndex >= produced) {
                done.complete(null);
                stopHeartbeat();
            }
            if (nextIndex > 0) {
                stopHeartbeat();
            }
        }

        private void maybeSendLateUpdate(int index, String audioUrl) {
            if (!lateUpdate) return;
            if (audioUrl == null) return;
            // 已经发出过该段，并且之前未带音频
            Boolean delivered = audioDelivered.get(index);
            if (index < nextIndex && (delivered == null || !delivered)) {
                try {
                    StreamChatResponse upd = StreamChatResponse.ttsSegmentUpdate(groupId, index, audioUrl, model);
                    sender.accept(upd);
                    audioDelivered.put(index, true);
                } catch (Exception e) {
                    log.warn("发送TTS_SEGMENT_UPDATE失败：index={}，错误：{}", index, e.getMessage());
                }
            }
        }

        private void startHeartbeat() {
            if (heartbeatRunning) return;
            heartbeatRunning = true;
            heartbeatTask = CompletableFuture.runAsync(() -> {
                while (heartbeatRunning) {
                    try {
                        sender.accept(StreamChatResponse.heartbeat());
                    } catch (Exception ignore) {}
                    try {
                        Thread.sleep(heartbeatMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

        private void stopHeartbeat() {
            heartbeatRunning = false;
            heartbeatTask = null;
        }

        private int findCutIndex(StringBuilder sb, int minChars, int maxChars) {
            int len = sb.length();
            if (len < minChars) return -1;
            int limit = Math.min(len, maxChars);
            // 优先：句末标点/换行
            int idx = lastIndexOfAny(sb, new char[]{'。','！','？','!','?','\n','\r'}, limit);
            if (idx >= minChars) return idx;
            // 次选：逗号、顿号
            idx = lastIndexOfAny(sb, new char[]{'，','、',','}, limit);
            if (idx >= minChars) return idx;
            // 空格
            idx = lastIndexOf(sb, ' ', limit - 1);
            if (idx >= minChars) return idx;
            // 硬切
            return limit;
        }

        private int lastIndexOfAny(CharSequence s, char[] chars, int endExclusive) {
            int bound = Math.min(endExclusive, s.length());
            for (int i = bound - 1; i >= 0; i--) {
                char c = s.charAt(i);
                for (char target : chars) {
                    if (c == target) return i + 1;
                }
            }
            return -1;
        }

        private int lastIndexOf(CharSequence s, char ch, int fromInclusive) {
            int i = Math.min(fromInclusive, s.length() - 1);
            for (; i >= 0; i--) {
                if (s.charAt(i) == ch) return i + 1;
            }
            return -1;
        }
    }

    /**
     * 发送消息到WebSocket
     */
    private void sendMessage(WebSocketSession session, StreamChatResponse response) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(response);
                synchronized (session) { // 避免并发写同一Session导致异常
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        StreamChatResponse errorResponse = StreamChatResponse.error(errorMessage);
        sendMessage(session, errorResponse);
    }

    /**
     * 从WebSocket会话属性中获取用户ID
     * 用户信息已经在JWT认证拦截器中验证并存储到会话属性中
     */
    private Long getUserIdFromSession(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof Long) {
            return (Long) userId;
        } else if (userId instanceof String) {
            try {
                return Long.valueOf((String) userId);
            } catch (NumberFormatException e) {
                log.warn("无效的用户ID格式：{}", userId);
            }
        } else if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        
        log.warn("WebSocket会话中未找到有效的用户ID");
        return null;
    }
    
    /**
     * 从WebSocket会话属性中获取用户名
     */
    private String getUsernameFromSession(WebSocketSession session) {
        Object username = session.getAttributes().get("username");
        return username != null ? username.toString() : "未知用户";
    }
    
    /**
     * 从WebSocket会话属性中获取用户角色
     */
    private String getUserRolesFromSession(WebSocketSession session) {
        Object roles = session.getAttributes().get("roles");
        return roles != null ? roles.toString() : "ROLE_USER";
    }

    /**
     * 获取或创建对话
     */
    private Conversation getOrCreateConversation(ChatRequestDto requestDto, Long userId) {
        if (requestDto.getConversationId() != null) {
            return conversationRepository.findByIdAndUserId(requestDto.getConversationId(), userId)
                    .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "对话不存在"));
        } else {
            String title = requestDto.getTitle() != null ? requestDto.getTitle() : "新对话";
            String modelName = requestDto.getModelName() != null ? requestDto.getModelName() : "gpt-4o-mini";
            String systemPrompt = requestDto.getSystemPrompt() != null ? requestDto.getSystemPrompt() : "你是一个有用的AI助手";
            
            return conversationDomainService.createConversation(userId, title, modelName, systemPrompt, requestDto.getRoleId());
        }
    }

    /**
     * 构建流式AI请求
     */
    private ChatRequest buildStreamAiRequest(Conversation conversation, ChatRequestDto requestDto, Role role) {
        // 获取对话历史（限制数量）
        List<ConversationMessage> history = conversationApplicationService.getConversationHistoryForInternal(
                conversation.getId(), conversation.getUserId());
        
        List<ChatMessage> messages = new ArrayList<>();
        
        // 添加系统消息（与HTTP对齐：优先请求systemPrompt，其次会话，最后默认；并拼接角色人设）
        String systemPrompt = buildSystemPrompt(conversation, requestDto, role);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(ChatMessage.system(systemPrompt));
        }
        
        // 动态裁剪历史：与HTTP路径一致，基于简单token预算从尾部选择，最多20条
        addTrimmedHistory(messages, history, systemPrompt);

        // 与HTTP语义对齐：联网搜索仅按请求开关控制，默认false
        boolean enableWebSearch = requestDto.getEnableWebSearch() != null ? requestDto.getEnableWebSearch() : false;

        return ChatRequest.builder()
                .messages(messages)
                .model(requestDto.getModelName() != null ? requestDto.getModelName() : conversation.getModelName())
                .temperature(requestDto.getTemperature() != null ? requestDto.getTemperature() : 0.7)
                .maxTokens(requestDto.getMaxTokens() != null ? requestDto.getMaxTokens() : 2000)
                .stream(true)
                .userId(conversation.getUserId())
                .conversationId(conversation.getId())
                .enableWebSearch(enableWebSearch)
                .build();
    }

    /**
     * 根据简单 token 预算从尾部选择历史消息，避免上下文过长
     * 逻辑与HTTP同步：预算约2500 tokens（不含输出），最多20条历史
     */
    private void addTrimmedHistory(List<ChatMessage> target, List<ConversationMessage> history, String systemPrompt) {
        if (history == null || history.isEmpty()) return;
        int budget = 2500; // 粗略预算
        int used = 0;
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            used += aiChatService.estimateTokenCount(systemPrompt);
        }

        List<ConversationMessage> buffer = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0 && buffer.size() < 20; i--) {
            ConversationMessage msg = history.get(i);
            String content = msg.getContent();
            if (content == null || content.isEmpty()) continue;
            int t = aiChatService.estimateTokenCount(content);
            if (used + t > budget) break;
            used += t;
            buffer.add(msg);
        }
        // 正序追加到目标
        for (int i = buffer.size() - 1; i >= 0; i--) {
            ConversationMessage msg = buffer.get(i);
            switch (msg.getRole()) {
                case USER:
                    target.add(ChatMessage.user(msg.getContent()));
                    break;
                case ASSISTANT:
                    target.add(ChatMessage.assistant(msg.getContent()));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 构建系统提示词，集成角色信息（与HTTP一致）
     */
    private String buildSystemPrompt(Conversation conversation, ChatRequestDto requestDto, Role role) {
        StringBuilder sb = new StringBuilder();
        if (requestDto.getSystemPrompt() != null && !requestDto.getSystemPrompt().trim().isEmpty()) {
            sb.append(requestDto.getSystemPrompt().trim());
        } else if (conversation.getSystemPrompt() != null && !conversation.getSystemPrompt().trim().isEmpty()) {
            sb.append(conversation.getSystemPrompt().trim());
        } else {
            sb.append("你是一个有用的AI助手");
        }
        if (role != null) {
            sb.append("\n\n");
            sb.append("=== 角色设定 ===\n");
            if (role.getDescription() != null && !role.getDescription().trim().isEmpty()) {
                sb.append("角色描述：").append(role.getDescription().trim()).append("\n");
            }
            if (role.getPersonaPrompt() != null && !role.getPersonaPrompt().trim().isEmpty()) {
                sb.append("人设要求：").append(role.getPersonaPrompt().trim()).append("\n");
            }
            sb.append("请严格按照以上角色设定进行对话，保持角色的一致性。");
        }
        return sb.toString();
    }

    private boolean getBooleanConfig(String key, boolean defaultVal) {
        try {
            return systemConfigRepository.findByKey(key)
                    .filter(SystemConfig::getEnabled)
                    .map(SystemConfig::getConfigValue)
                    .map(val -> {
                        String v = val.trim().toLowerCase();
                        if ("true".equals(v) || "1".equals(v) || "yes".equals(v) || "on".equals(v)) return true;
                        if ("false".equals(v) || "0".equals(v) || "no".equals(v) || "off".equals(v)) return false;
                        return defaultVal;
                    })
                    .orElse(defaultVal);
        } catch (Exception e) {
            log.warn("读取系统配置失败，key={}，使用默认值 {}", key, defaultVal, e);
            return defaultVal;
        }
    }

    /**
     * 获取下一个消息序号
     */
    private Integer getNextSequence(Long conversationId) {
        try {
            return conversationMessageRepository.getNextSequenceByConversationId(conversationId);
        } catch (Exception e) {
            log.error("获取消息序号失败，对话ID：{}", conversationId, e);
            // 如果获取失败，返回一个安全的默认值
            Long messageCount = conversationMessageRepository.countByConversationId(conversationId);
            return messageCount.intValue() + 1;
        }
    }
}
