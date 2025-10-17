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
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.domain.config.service.SystemConfigService;
import com.nexusvoice.utils.MarkdownTextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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

    private final ConversationApplicationService conversationApplicationService;
    private final ConversationRepository conversationRepository;
    private final ConversationDomainService conversationDomainService;
    private final RoleApplicationService roleApplicationService;
    private final TTSService ttsService;
    private final SystemConfigService systemConfigService;
    private final DynamicAiModelBeanManager modelBeanManager;
    private final ObjectMapper objectMapper;
    
    // 存储会话信息
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    // 单flight保护：同一Session同一时间仅处理一个请求
    private final ConcurrentMap<String, Boolean> streamingSessions = new ConcurrentHashMap<>();
    // 正在处理的TTS任务计数器，用于优雅关闭
    private final ConcurrentMap<String, AtomicInteger> sessionTaskCounts = new ConcurrentHashMap<>();
    
    // JDK 21虚拟线程执行器 - 用于I/O密集型任务（如TTS调用）
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    // 专用的心跳任务执行器（使用单个虚拟线程）
    private final ExecutorService heartbeatExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    public ChatStreamHandler(ConversationApplicationService conversationApplicationService,
                           ConversationRepository conversationRepository,
                           ConversationDomainService conversationDomainService,
                           ConversationMessageRepository conversationMessageRepository,
                           RoleApplicationService roleApplicationService,
                           TTSService ttsService,
                           SystemConfigService systemConfigService,
                           DynamicAiModelBeanManager modelBeanManager,
                           ObjectMapper objectMapper) {
        this.conversationApplicationService = conversationApplicationService;
        this.conversationRepository = conversationRepository;
        this.conversationDomainService = conversationDomainService;
        // conversationMessageRepository不需要存储，不使用
        this.roleApplicationService = roleApplicationService;
        this.ttsService = ttsService;
        this.systemConfigService = systemConfigService;
        this.modelBeanManager = modelBeanManager;
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
        
        // 等待正在执行的TTS任务完成（最多等待5秒）
        AtomicInteger taskCount = sessionTaskCounts.get(sessionId);
        if (taskCount != null && taskCount.get() > 0) {
            log.info("等待{}个TTS任务完成，会话ID：{}", taskCount.get(), sessionId);
            long waitStart = System.currentTimeMillis();
            while (taskCount.get() > 0 && System.currentTimeMillis() - waitStart < 5000) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            // 超时后强制记录警告
            if (taskCount.get() > 0) {
                log.warn("等待TTS任务超时，强制清理，会话ID：{}，剩余任务数：{}", sessionId, taskCount.get());
            }
        }
        
        activeSessions.remove(sessionId);
        streamingSessions.remove(sessionId);
        sessionTaskCounts.remove(sessionId);
        
        log.info("WebSocket连接关闭，会话ID：{}，状态：{}", sessionId, closeStatus);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * 优雅关闭：清理资源
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭WebSocket处理器，清理资源...");
        
        // 1. 关闭所有活动会话
        activeSessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.close(CloseStatus.GOING_AWAY);
                }
            } catch (Exception e) {
                log.warn("关闭WebSocket会话失败：{}", e.getMessage());
            }
        });
        
        // 2. 等待所有TTS任务完成（最多等待10秒）
        long shutdownStart = System.currentTimeMillis();
        while (!sessionTaskCounts.isEmpty() && 
               System.currentTimeMillis() - shutdownStart < 10000) {
            int totalTasks = sessionTaskCounts.values().stream()
                    .mapToInt(AtomicInteger::get).sum();
            if (totalTasks > 0) {
                log.info("等待{}个TTS任务完成...", totalTasks);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                break;
            }
        }
        
        // 3. 关闭线程池
        virtualThreadExecutor.shutdown();
        heartbeatExecutor.shutdown();
        
        log.info("WebSocket处理器资源清理完成");
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
            // 使用StringBuffer保证线程安全（多个TTS线程会并发append）
            StringBuffer responseContent = new StringBuffer();

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
                    ? new SegmentAggregator(sessionId, ttsGroupId, selectedVoiceType, modelName, firstMinChars, minChars, maxChars, firstGateMs, ttsMaxConcurrency, heartbeatMs, lateUpdateEnabled,
                    (segText) -> responseContent.append(segText),
                    (resp) -> sendMessage(session, resp))
                    : null;

            // 动态获取AI服务（与HTTP完全一致）
            AiChatService aiChatService = getAiChatService(aiRequest.getModel());
            
            aiChatService.streamChat(aiRequest,
                    // onNext - 处理流式数据
                    (streamResponse) -> {
                        try {
                            if (streamResponse.getType() == StreamChatResponse.StreamMessageType.END) {
                                // END信号由onComplete处理，这里只记录日志
                                log.debug("收到AI模型的END信号，对话ID：{}", conversation.getId());
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
                        // 发送END信号给客户端，避免客户端一直等待
                        StreamChatResponse errorEndResp = StreamChatResponse.end("error");
                        errorEndResp.setConversationId(conversation.getId());
                        errorEndResp.setResponseTimeMs(System.currentTimeMillis() - startTime);
                        sendMessage(session, errorEndResp);
                        // 清理会话状态（包括任务计数器）
                        cleanupSession(sessionId, singleFlightEnabled);
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
                                    // 清理会话状态（包括任务计数器）
                                    cleanupSession(sessionId, singleFlightEnabled);
                                }
                            });
                        } catch (Exception e) {
                            log.error("流式完成阶段处理失败", e);
                            // 清理会话状态（包括任务计数器）
                            cleanupSession(sessionId, singleFlightEnabled);
                        }
                    }
            );
            
        } catch (BizException e) {
            log.error("流式聊天业务异常，用户ID：{}", userId, e);
            sendErrorMessage(session, e.getMessage());
            // 发送END信号给客户端
            sendMessage(session, StreamChatResponse.end("error"));
            // 清理会话状态（包括任务计数器）
            cleanupSession(session.getId(), singleFlightEnabled);
        } catch (Exception e) {
            log.error("流式聊天系统异常，用户ID：{}", userId, e);
            sendErrorMessage(session, "系统繁忙，请稍后重试");
            // 发送END信号给客户端
            sendMessage(session, StreamChatResponse.end("error"));
            // 清理会话状态（包括任务计数器）
            cleanupSession(session.getId(), singleFlightEnabled);
        }
    }

    /**
     * 获取AI聊天服务
     * 根据模型名称动态获取对应的服务实例（与HTTP完全一致）
     */
    private AiChatService getAiChatService(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            // 从系统配置获取默认模型
            modelName = systemConfigService.getDefaultAiModel();
            log.debug("WebSocket使用默认AI模型：{}", modelName);
        }
        
        // 兼容旧格式（没有provider前缀的）
        if (!modelName.contains(":")) {
            String defaultProvider = systemConfigService.getDefaultAiModelProvider();
            modelName = defaultProvider + ":" + modelName;
            log.debug("WebSocket自动添加模型厂商前缀：{}", modelName);
        }
        
        return modelBeanManager.getServiceByModelKey(modelName);
    }
    
    /**
     * 估算token数量
     * 简单的估算方法：平均 3-4 个字符一个token
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 中文字符通常占更多token，英文相对较少
        // 这里使用简单的估算：大约3个字符=1个token
        return (int) Math.ceil(text.length() / 3.0);
    }
    
    /**
     * 简单读取整数配置
     */
    private int getIntConfig(String key, int defaultVal, int min, int max) {
        try {
            int value = systemConfigService.getInt(key, defaultVal);
            if (value < min) return min;
            if (value > max) return max;
            return value;
        } catch (Exception e) {
            log.warn("读取整型配置失败，key={}，使用默认值 {}", key, defaultVal, e);
            return defaultVal;
        }
    }

    /**
     * 分段聚合器：按阈值切分文本、并发TTS并按序发送TTS_SEGMENT
     */
    private class SegmentAggregator {
        private final String sessionId; // WebSocket会话sessionId，用于任务计数
        private final String groupId;   // TTS分组ID，用于消息分组
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
        private final AtomicInteger nextIndex = new AtomicInteger(0);
        private final AtomicInteger produced = new AtomicInteger(0);
        private volatile boolean finished = false;
        private final CompletableFuture<Void> done = new CompletableFuture<>();
        private final AtomicBoolean heartbeatRunning = new AtomicBoolean(false);
        private CompletableFuture<?> heartbeatTask;
        private CompletableFuture<?> firstSegmentFallbackTask; // 首段超时任务

        SegmentAggregator(String sessionId, String groupId, String voiceType, String model, int firstMinChars, int minChars, int maxChars, int firstGateMs, int concurrency, int heartbeatMs, boolean lateUpdate,
                          Consumer<String> appendTotal, Consumer<StreamChatResponse> sender) {
            this.sessionId = sessionId;
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
                    scheduleSegment(text, produced.getAndIncrement());
                }
                finished = true;
            }
            // 尝试在完成后刷新
            flush();
            return done;
        }

        private void tryCut() {
            while (true) {
                int currentProduced = produced.get();
                int need = (currentProduced == 0) ? firstMinChars : minChars;
                if (buf.length() < need) return;
                int cut = findCutIndex(buf, need, maxChars);
                if (cut <= 0) return;
                String text = buf.substring(0, cut);
                buf.delete(0, cut);
                scheduleSegment(text, produced.getAndIncrement());
            }
        }

        private void scheduleSegment(String text, int index) {
            segText.put(index, text);
            appendTotal.accept(text);
            // 增加任务计数（使用WebSocket的sessionId）
            sessionTaskCounts.computeIfAbsent(sessionId, k -> new AtomicInteger(0)).incrementAndGet();
            
            if (index == 0) {
                startHeartbeat();
                // 首段fallback：超过firstGateMs仍未获取音频，先释放文本段
                firstSegmentFallbackTask = CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(firstGateMs);
                        if (!segAudio.containsKey(0)) {
                            segAudio.put(0, null);
                            flush();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.debug("首段fallback任务被中断");
                    }
                }, virtualThreadExecutor);
            }

            // 使用虚拟线程执行器代替默认线程池
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
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("分段TTS被中断：index={}", index);
                    segAudio.put(index, null);
                } catch (Exception e) {
                    log.warn("分段TTS失败：index={}，错误：{}", index, e.getMessage(), e);
                    segAudio.put(index, null);
                } finally {
                    if (acquired) permits.release();
                    flush();
                    // 减少任务计数
                    AtomicInteger counter = sessionTaskCounts.get(sessionId);
                    if (counter != null) {
                        counter.decrementAndGet();
                    }
                }
            }, virtualThreadExecutor); // 使用虚拟线程执行器
        }

        private synchronized void flush() {
            // 首段同步门：只有当 index=0 有音频（或超时）才发
            int currentNextIndex = nextIndex.get();
            int currentProduced = produced.get();
            if (currentNextIndex == 0 && currentProduced > 0 && !segAudio.containsKey(0)) {
                return;
            }
            while (segText.containsKey(currentNextIndex)) {
                // 对于非首段，允许音频为空（失败也发文本）
                if (currentNextIndex == 0 && !segAudio.containsKey(0)) break;
                String text = segText.remove(currentNextIndex);
                String audio = segAudio.getOrDefault(currentNextIndex, null);
                StreamChatResponse seg = StreamChatResponse.ttsSegment(groupId, currentNextIndex, text, audio, model);
                try {
                    sender.accept(seg);
                } catch (Exception e) {
                    log.warn("发送TTS_SEGMENT失败：index={}，错误：{}", currentNextIndex, e.getMessage());
                }
                audioDelivered.put(currentNextIndex, audio != null);
                nextIndex.incrementAndGet();
                currentNextIndex = nextIndex.get();
            }
            // 如果已完成且全部发送，标记done
            if (finished && currentNextIndex >= currentProduced) {
                done.complete(null);
                stopHeartbeat();
            }
            // 删除过早停止心跳的逻辑，让心跳持续到全部完成
        }

        private void maybeSendLateUpdate(int index, String audioUrl) {
            if (!lateUpdate) return;
            if (audioUrl == null) return;
            // 已经发出过该段，并且之前未带音频
            Boolean delivered = audioDelivered.get(index);
            int currentNextIndex = nextIndex.get();
            if (index < currentNextIndex && (delivered == null || !delivered)) {
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
            if (!heartbeatRunning.compareAndSet(false, true)) return;
            
            heartbeatTask = CompletableFuture.runAsync(() -> {
                while (heartbeatRunning.get()) {
                    try {
                        sender.accept(StreamChatResponse.heartbeat());
                    } catch (Exception e) {
                        log.debug("发送心跳失败：{}", e.getMessage());
                    }
                    try {
                        Thread.sleep(heartbeatMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, heartbeatExecutor); // 使用专用的心跳执行器
        }

        private void stopHeartbeat() {
            heartbeatRunning.set(false);
            if (heartbeatTask != null) {
                heartbeatTask.cancel(true); // 主动取消心跳任务
                heartbeatTask = null;
            }
            // 同时取消首段fallback任务
            if (firstSegmentFallbackTask != null) {
                firstSegmentFallbackTask.cancel(true);
                firstSegmentFallbackTask = null;
            }
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
     * 清理会话状态（统一清理入口）
     * 
     * 设计策略：
     * 1. 此方法在请求完成或失败时调用，用于清理streamingSessions
     * 2. sessionTaskCounts不在此处删除，因为可能还有TTS任务在执行
     * 3. TTS任务完成时会自动decrementAndGet()，让计数器自然减到0
     * 4. 连接关闭时，在afterConnectionClosed()中等待任务后强制删除sessionTaskCounts
     * 
     * 这样设计的原因：
     * - 避免任务执行中删除计数器导致NPE
     * - 让计数器自然减到0，便于监控和调试
     * - 支持任务在请求完成后继续执行（异步处理）
     */
    private void cleanupSession(String sessionId, boolean clearStreamingFlag) {
        try {
            if (clearStreamingFlag) {
                streamingSessions.remove(sessionId);
            }
            // 记录当前未完成的任务数（用于调试）
            AtomicInteger taskCount = sessionTaskCounts.get(sessionId);
            if (taskCount != null && taskCount.get() > 0) {
                log.debug("请求已完成，但还有{}个TTS任务在处理，会话ID：{}", taskCount.get(), sessionId);
            }
        } catch (Exception e) {
            log.warn("清理会话状态失败，会话ID：{}", sessionId, e);
        }
    }
    
    /**
     * 发送消息到WebSocket
     */
    private void sendMessage(WebSocketSession session, StreamChatResponse response) {
        try {
            synchronized (session) { // 避免并发写同一Session导致异常
                if (session.isOpen()) {
                    String json = objectMapper.writeValueAsString(response);
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IllegalStateException e) {
            // 会话已关闭，静默忽略
            log.debug("会话已关闭，无法发送消息：sessionId={}", session.getId());
        } catch (IOException e) {
            log.warn("发送WebSocket消息失败：sessionId={}，错误：{}", session.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("发送WebSocket消息异常：sessionId={}", session.getId(), e);
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
            // 所有默认值从系统配置获取
            String title = requestDto.getTitle() != null ? requestDto.getTitle() : systemConfigService.getDefaultConversationTitle();
            String modelName = requestDto.getModelName() != null ? requestDto.getModelName() : systemConfigService.getDefaultAiModel();
            String systemPrompt = requestDto.getSystemPrompt() != null ? requestDto.getSystemPrompt() : systemConfigService.getDefaultSystemPrompt();
            
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
        
        // 处理模型名称（与HTTP完全一致）
        String modelName = requestDto.getModelName() != null ? requestDto.getModelName() : conversation.getModelName();
        // 支持新的模型格式：provider:model，如果没有provider默认使用openai
        if (modelName != null && !modelName.contains(":")) {
            modelName = "openai:" + modelName;
        }

        // 完全对标HTTP实现，添加RAG和知识库支持
        return ChatRequest.builder()
                .messages(messages)
                .model(modelName)
                .temperature(requestDto.getTemperature() != null ? requestDto.getTemperature() : 0.7)
                .maxTokens(requestDto.getMaxTokens() != null ? requestDto.getMaxTokens() : 2000)
                .stream(true)
                .userId(conversation.getUserId())
                .conversationId(conversation.getId())
                .enableWebSearch(enableWebSearch)
                .enableRag(requestDto.getEnableRag() != null ? requestDto.getEnableRag() : false)
                .knowledgeBaseIds(requestDto.getKnowledgeBaseIds())
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
            used += estimateTokenCount(systemPrompt);
        }

        // 优化：确保至少保留最近3条消息（即便超出token预算）
        List<ConversationMessage> buffer = new ArrayList<>();
        int minKeep = 3; // 至少保留的消息数
        
        for (int i = history.size() - 1; i >= 0 && buffer.size() < 20; i--) {
            ConversationMessage msg = history.get(i);
            String content = msg.getContent();
            if (content == null || content.isEmpty()) continue;
            
            int t = estimateTokenCount(content);
            
            // 如果是前几条必须保留的消息，或者token预算还有空间
            if (buffer.size() < minKeep || used + t <= budget) {
                used += t;
                buffer.add(msg);
            } else if (used + t > budget) {
                // token预算已满，且已经有最小保留数，停止添加
                break;
            }
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
     * 构建系统提示词，集成角色信息（与HTTP完全一致）
     */
    private String buildSystemPrompt(Conversation conversation, ChatRequestDto requestDto, Role role) {
        StringBuilder sb = new StringBuilder();
        
        // 1. 优先使用请求中的系统提示词
        if (requestDto.getSystemPrompt() != null && !requestDto.getSystemPrompt().trim().isEmpty()) {
            sb.append(requestDto.getSystemPrompt().trim());
        }
        // 2. 其次使用对话中保存的系统提示词
        else if (conversation.getSystemPrompt() != null && !conversation.getSystemPrompt().trim().isEmpty()) {
            sb.append(conversation.getSystemPrompt().trim());
        }
        // 3. 最后使用默认提示词
        else {
            sb.append("你是一个有用的AI助手");
        }
        
        // 4. 如果指定了角色，集成角色的人设信息
        if (role != null) {
            sb.append("\n\n");
            sb.append("=== 角色设定 ===\n");
            
            // 添加角色描述
            if (role.getDescription() != null && !role.getDescription().trim().isEmpty()) {
                sb.append("角色描述：").append(role.getDescription().trim()).append("\n");
            }
            
            // 添加角色人设提示词
            if (role.getPersonaPrompt() != null && !role.getPersonaPrompt().trim().isEmpty()) {
                sb.append("人设要求：").append(role.getPersonaPrompt().trim()).append("\n");
            }
            
            sb.append("请严格按照以上角色设定进行对话，保持角色的一致性。");
            
            log.info("集成角色信息到系统提示词，角色ID：{}，角色名称：{}", role.getId(), role.getName());
        }
        
        // 5. 添加全局回复风格要求（与HTTP完全一致）
        sb.append("\n\n");
        sb.append("【重要】回复风格要求：请保持回答简洁精炼，直击要点，避免冗长的解释和不必要的铺垫。");
        
        return sb.toString();
    }

    private boolean getBooleanConfig(String key, boolean defaultVal) {
        try {
            return systemConfigService.getBoolean(key, defaultVal);
        } catch (Exception e) {
            log.warn("读取系统配置失败，key={}，使用默认值 {}", key, defaultVal, e);
            return defaultVal;
        }
    }

}
