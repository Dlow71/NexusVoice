package com.nexusvoice.application.rtc.service;

import com.nexusvoice.application.rtc.dto.InterruptRequestDto;
import com.nexusvoice.application.rtc.dto.RtcSessionCreateRequest;
import com.nexusvoice.application.rtc.dto.RtcSessionCreateResponse;
import com.nexusvoice.domain.config.service.SystemConfigService;
import com.nexusvoice.domain.rtc.enums.InterruptMode;
import com.nexusvoice.domain.rtc.enums.RtcSessionState;
import com.nexusvoice.domain.rtc.model.RtcSession;
import com.nexusvoice.domain.rtc.repository.RtcSessionRepository;
import com.nexusvoice.domain.role.model.Role;
import com.nexusvoice.domain.role.repository.RoleRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.infrastructure.rtc.grpc.GrpcAsrClient;
import com.nexusvoice.infrastructure.rtc.grpc.GrpcTtsClient;
import com.nexusvoice.infrastructure.rtc.grpc.asr.*;
import com.nexusvoice.infrastructure.rtc.grpc.common.*;
import com.nexusvoice.infrastructure.rtc.grpc.tts.*;
import com.nexusvoice.infrastructure.rtc.kurento.KurentoMediaService;
import com.nexusvoice.infrastructure.rtc.kurento.WebRtcEndpointManager;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * RTC会话应用服务
 * 
 * 职责：
 * - 会话创建与管理
 * - ASR→LLM→TTS主链路编排
 * - 半双工对话流程（说完播）
 * - 软/硬中断控制
 * - 权限验证与限制检查
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rtc.enabled", havingValue = "true", matchIfMissing = false)
public class RtcSessionApplicationService {
    
    private final RtcSessionRepository rtcSessionRepository;
    private final RoleRepository roleRepository;
    private final SystemConfigService systemConfigService;
    private final DynamicAiModelBeanManager modelBeanManager;
    private final KurentoMediaService kurentoMediaService;
    private final WebRtcEndpointManager webRtcEndpointManager;
    private final GrpcAsrClient grpcAsrClient;
    private final GrpcTtsClient grpcTtsClient;
    
    @Value("${rtc.max-concurrent-sessions:3}")
    private int maxConcurrentSessions;
    
    @Value("${rtc.session-timeout-minutes:30}")
    private int sessionTimeoutMinutes;
    
    @Value("${server.port:8081}")
    private int serverPort;
    
    /**
     * 初始化清理定时任务
     */
    @PostConstruct
    public void init() {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        // 每5分钟执行一次清理
        cleanupScheduler.scheduleAtFixedRate(this::cleanupTimeoutSessions, 5, 5, TimeUnit.MINUTES);
        log.info("RTC会话清理任务已启动");
    }
    
    /**
     * 关闭清理定时任务
     */
    @PreDestroy
    public void destroy() {
        if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
            cleanupScheduler.shutdown();
            try {
                if (!cleanupScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    cleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("RTC会话清理任务已关闭");
        }
    }
    
    /**
     * 清理超时会话缓存（防止资源泄漏）
     */
    private void cleanupTimeoutSessions() {
        try {
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(sessionTimeoutMinutes);
            
            // 查找超时会话
            List<RtcSession> activeSessions = rtcSessionRepository.findByState(RtcSessionState.LISTENING);
            activeSessions.addAll(rtcSessionRepository.findByState(RtcSessionState.RECOGNIZING));
            activeSessions.addAll(rtcSessionRepository.findByState(RtcSessionState.THINKING));
            activeSessions.addAll(rtcSessionRepository.findByState(RtcSessionState.SPEAKING));
            
            for (RtcSession session : activeSessions) {
                if (session.getUpdatedAt().isBefore(timeoutThreshold)) {
                    String sessionId = session.getSessionId();
                    log.warn("检测到超时会话，强制清理: sessionId={}, 最后活动时间={}", 
                            sessionId, session.getUpdatedAt());
                    
                    // 清理缓存
                    streamObservers.remove(sessionId);
                    accumulatedTexts.remove(sessionId);
                    ttsSegmentCounters.remove(sessionId);
                    
                    // 释放Media资源
                    try {
                        kurentoMediaService.releaseSession(sessionId);
                    } catch (Exception e) {
                        log.error("释放Kurento资源失败: sessionId={}", sessionId, e);
                    }
                    
                    // 记录错误信息但保持终止状态
                    session.setLastErrorCode("TIMEOUT");
                    session.setLastErrorMessage("会话超时自动清理");
                    session.transitionTo(RtcSessionState.TERMINATED);
                    session.setEndedAt(LocalDateTime.now());
                    rtcSessionRepository.save(session);
                }
            }
        } catch (Exception e) {
            log.error("清理超时会话失败", e);
        }
    }
    
    // 会话流观察者缓存（sessionId -> {asr, tts}）
    private final ConcurrentHashMap<String, StreamObservers> streamObservers = new ConcurrentHashMap<>();
    
    // 会话累积文本缓存（sessionId -> StringBuffer，线程安全）
    private final ConcurrentHashMap<String, StringBuffer> accumulatedTexts = new ConcurrentHashMap<>();
    
    // 会话TTS片段计数器（sessionId -> AtomicInteger）
    private final ConcurrentHashMap<String, AtomicInteger> ttsSegmentCounters = new ConcurrentHashMap<>();
    
    // 定时任务：清理超时会话缓存
    private ScheduledExecutorService cleanupScheduler;
    
    /**
     * 创建RTC会话
     * 
     * @param request 创建请求
     * @param userId 用户ID
     * @return 创建响应
     */
    @Transactional
    public RtcSessionCreateResponse createSession(RtcSessionCreateRequest request, Long userId) {
        log.info("创建RTC会话: userId={}, roleId={}", userId, request.getRoleId());
        
        // 1. 检查并发限制
        int activeCount = rtcSessionRepository.countActiveSessionsByUserId(userId);
        if (activeCount >= maxConcurrentSessions) {
            throw new BizException(ErrorCodeEnum.BAD_REQUEST, 
                    String.format("超过最大并发会话数限制（%d）", maxConcurrentSessions));
        }
        
        // 2. 验证角色权限（如果指定了角色）
        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new BizException(ErrorCodeEnum.NOT_FOUND, "角色不存在"));
            
            // 验证角色访问权限（这里简化，实际可根据role.getUserId或公开性判断）
            if (role.getUserId() != null && !role.getUserId().equals(userId)) {
                throw new BizException(ErrorCodeEnum.FORBIDDEN, "无权访问此角色");
            }
        }
        
        // 3. 创建领域实体
        RtcSession session = new RtcSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setRoleId(request.getRoleId());
        session.setConversationId(request.getConversationId());
        session.setState(RtcSessionState.IDLE);
        session.setInterruptCount(0);
        session.setCurrentTtsSegmentId(0);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setDeleted(false);
        
        // 4. 保存会话
        session = rtcSessionRepository.save(session);
        
        log.info("RTC会话创建成功: sessionId={}, id={}", session.getSessionId(), session.getId());
        
        // 5. 构建响应
        return RtcSessionCreateResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .signalingUrl("ws://localhost:" + serverPort + "/ws/rtc/signal")
                .stunServer(systemConfigService.getString("rtc.kurento.stun-server", "stun:stun.l.google.com:19302"))
                .timeoutMinutes(sessionTimeoutMinutes)
                .build();
    }
    
    /**
     * 开始WebRTC连接
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    @Transactional
    public void startConnection(String sessionId, Long userId) {
        log.info("开始WebRTC连接: sessionId={}, userId={}", sessionId, userId);
        
        // 1. 获取会话并验证权限
        RtcSession session = rtcSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.NOT_FOUND, "RTC会话不存在"));
        
        if (!session.getUserId().equals(userId)) {
            throw new BizException(ErrorCodeEnum.FORBIDDEN, "无权访问此会话");
        }
        
        // 2. 验证状态
        if (session.getState() != RtcSessionState.IDLE) {
            throw new BizException(ErrorCodeEnum.BAD_REQUEST, 
                    "会话状态不允许此操作，当前状态：" + session.getState().getDescription());
        }
        
        // 3. 创建Kurento媒体管线
        try {
            MediaPipeline pipeline = kurentoMediaService.createPipeline(sessionId);
            WebRtcEndpoint webRtcEndpoint = kurentoMediaService.createWebRtcEndpoint(sessionId, pipeline);
            KurentoMediaService.RtpEndpointPair rtpEndpoints = kurentoMediaService.createRtpEndpoints(sessionId, pipeline);
            
            // 连接端点
            kurentoMediaService.connectEndpoints(sessionId, webRtcEndpoint, rtpEndpoints);
            
            // 更新会话
            session.setKmsPipelineId(pipeline.getId());
            session.setKmsWebrtcEndpointId(webRtcEndpoint.getId());
            session.transitionTo(RtcSessionState.CONNECTING);
            session.setStartedAt(LocalDateTime.now());
            
            rtcSessionRepository.save(session);
            
            log.info("Kurento媒体管线创建成功: sessionId={}, pipelineId={}", 
                    sessionId, pipeline.getId());
        } catch (Exception e) {
            log.error("创建Kurento媒体管线失败: sessionId={}", sessionId, e);
            session.recordError("KURENTO_ERROR", e.getMessage());
            rtcSessionRepository.save(session);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, 
                    "WebRTC连接失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理SDP Offer并返回Answer
     * 
     * @param sessionId 会话ID
     * @param sdpOffer SDP Offer
     * @param userId 用户ID
     * @return SDP Answer
     */
    @Transactional
    public String processSdpOffer(String sessionId, String sdpOffer, Long userId) {
        log.info("处理SDP Offer: sessionId={}", sessionId);
        
        // 1. 获取会话并验证权限
        RtcSession session = rtcSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.NOT_FOUND, "RTC会话不存在"));
        
        if (!session.getUserId().equals(userId)) {
            throw new BizException(ErrorCodeEnum.FORBIDDEN, "无权访问此会话");
        }
        
        // 2. 处理SDP协商
        try {
            String sdpAnswer = webRtcEndpointManager.processOffer(sessionId, sdpOffer);
            
            // 更新会话
            session.setRemoteSdp(sdpOffer);
            session.setLocalSdp(sdpAnswer);
            rtcSessionRepository.save(session);
            
            log.info("SDP协商成功: sessionId={}", sessionId);
            
            return sdpAnswer;
        } catch (Exception e) {
            log.error("SDP协商失败: sessionId={}", sessionId, e);
            session.recordError("SDP_ERROR", e.getMessage());
            rtcSessionRepository.save(session);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, 
                    "SDP协商失败: " + e.getMessage());
        }
    }
    
    /**
     * 启动ASR流式识别
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    public void startAsrStream(String sessionId, Long userId) {
        log.info("启动ASR流式识别: sessionId={}", sessionId);
        
        // 1. 获取会话
        RtcSession session = rtcSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.NOT_FOUND, "RTC会话不存在"));
        
        if (!session.getUserId().equals(userId)) {
            throw new BizException(ErrorCodeEnum.FORBIDDEN, "无权访问此会话");
        }

        // 幂等保护：如果已经有ASR流存在，则跳过
        StreamObservers observersExisting = streamObservers.get(sessionId);
        if (observersExisting != null && observersExisting.asrObserver != null) {
            log.warn("ASR流已存在，跳过重复启动: sessionId={}", sessionId);
            return;
        }
        
        // 2. 创建ASR配置
        AsrConfig asrConfig = AsrConfig.newBuilder()
                .setSessionId(sessionId)
                .setAudioFormat(AudioFormat.PCM_16BIT)
                .setSampleRate(16000)
                .setChannels(1)
                .setLanguageCode("zh-CN")
                .setEnableVad(true)
                .setEnablePunctuation(true)
                .setMaxAlternatives(1)
                .setVadConfig(VadConfig.newBuilder()
                        .setSpeechStartThresholdMs(100)
                        .setSpeechEndThresholdMs(200)
                        .setMinSpeechDurationMs(120)
                        .setMaxSilenceDurationMs(1000)
                        .build())
                .build();
        
        // 3. 创建ASR流式会话
        try {
            // 初始化累积文本（使用StringBuffer保证线程安全）
            accumulatedTexts.put(sessionId, new StringBuffer());
            
            StreamObserver<AsrRequest> asrObserver = grpcAsrClient.createStreamSession(
                    sessionId,
                    asrConfig,
                    result -> handleAsrResult(sessionId, result, userId),
                    vadEvent -> handleVadEvent(sessionId, vadEvent),
                    error -> handleAsrError(sessionId, error)
            );
            
            // 缓存观察者
            streamObservers.computeIfAbsent(sessionId, k -> new StreamObservers()).asrObserver = asrObserver;
            
            // 更新会话状态（使用单独事务）
            updateSessionStateInTransaction(session, RtcSessionState.LISTENING, sessionId, "asr");
            
            log.info("ASR流式会话启动成功: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("ASR流式会话启动失败: sessionId={}", sessionId, e);
            session.recordError("ASR_START_ERROR", e.getMessage());
            rtcSessionRepository.save(session);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, 
                    "ASR启动失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理ASR识别结果
     */
    private void handleAsrResult(String sessionId, RecognitionResult result, Long userId) {
        log.debug("ASR识别结果: sessionId={}, text={}, isFinal={}", 
                sessionId, result.getText(), result.getIsFinal());
        
        // 累积文本（StringBuffer线程安全）
        StringBuffer accumulated = accumulatedTexts.get(sessionId);
        if (accumulated != null) {
            accumulated.append(result.getText());
        }
        
        // 如果是最终结果，触发LLM处理
        if (result.getIsFinal() && result.getText() != null && !result.getText().isEmpty()) {
            String finalText = accumulated != null ? accumulated.toString() : result.getText();
            
            log.info("ASR最终识别结果: sessionId={}, text={}", sessionId, finalText);
            
            // 更新会话
            RtcSession session = rtcSessionRepository.findBySessionId(sessionId)
                    .orElse(null);
            if (session != null) {
                session.setCurrentAsrText(finalText);
                session.transitionTo(RtcSessionState.RECOGNIZING);
                rtcSessionRepository.save(session);
                
                // 触发LLM处理（半双工：说完才处理）
                processLlmRequest(sessionId, finalText, userId);
            }
            
            // 清空累积文本
            if (accumulated != null) {
                accumulated.setLength(0);
            }
        }
    }
    
    /**
     * 处理VAD事件
     */
    private void handleVadEvent(String sessionId, VadEvent event) {
        log.debug("VAD事件: sessionId={}, type={}", sessionId, event.getEventType());
        
        // 根据VAD事件更新会话状态
        RtcSession session = rtcSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session != null) {
            switch (event.getEventType()) {
                case SPEECH_START:
                    if (session.getState() == RtcSessionState.LISTENING) {
                        session.transitionTo(RtcSessionState.RECOGNIZING);
                        rtcSessionRepository.save(session);
                    }
                    break;
                case SPEECH_END:
                    // VAD检测到语音结束，等待ASR最终结果
                    break;
                default:
                    break;
            }
        }
    }
    
    /**
     * 处理ASR错误
     */
    private void handleAsrError(String sessionId, com.nexusvoice.infrastructure.rtc.grpc.common.ErrorInfo error) {
        log.error("ASR错误: sessionId={}, code={}, message={}", 
                sessionId, error.getErrorCode(), error.getErrorMessage());
        
        RtcSession session = rtcSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session != null) {
            session.recordError(error.getErrorCode(), error.getErrorMessage());
            rtcSessionRepository.save(session);
            
            // ASR流断开，自动触发会话清理
            if ("STREAM_ERROR".equals(error.getErrorCode())) {
                log.warn("ASR流异常断开，触发会话清理: sessionId={}", sessionId);
                try {
                    cleanupSessionResources(sessionId);
                } catch (Exception e) {
                    log.error("清理会话资源失败: sessionId={}", sessionId, e);
                }
            }
        }
    }
    
    /**
     * 处理LLM请求（半双工编排）
     */
    private void processLlmRequest(String sessionId, String userText, Long userId) {
        log.info("处理LLM请求: sessionId={}, text={}", sessionId, userText);
        
        RtcSession session = rtcSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session == null) {
            log.warn("会话不存在，跳过LLM处理: sessionId={}", sessionId);
            return;
        }
        
        try {
            // 1. 更新会话状态为思考中
            session.transitionTo(RtcSessionState.THINKING);
            rtcSessionRepository.save(session);
            
            // 2. 构建AI请求
            ChatRequest aiRequest = buildChatRequest(session, userText);
            
            // 3. 调用LLM（同步调用，半双工模式）
            String modelName = aiRequest.getModel();
            AiChatService aiChatService = modelBeanManager.getServiceByModelKey(modelName);
            com.nexusvoice.infrastructure.ai.model.ChatResponse aiResponse = aiChatService.chat(aiRequest);
            
            if (aiResponse.getSuccess()) {
                String llmResponse = aiResponse.getContent();
                
                log.info("LLM响应成功: sessionId={}, response={}", sessionId, llmResponse);
                
                // 4. 触发TTS合成
                startTtsSynthesis(sessionId, llmResponse, userId);
            } else {
                log.error("LLM响应失败: sessionId={}, error={}", sessionId, aiResponse.getErrorMessage());
                // 失败后回退到LISTENING状态，允许用户重试
                session.recordError("LLM_ERROR", aiResponse.getErrorMessage());
                session.transitionTo(RtcSessionState.LISTENING);
                rtcSessionRepository.save(session);
            }
        } catch (Exception e) {
            log.error("LLM处理异常: sessionId={}", sessionId, e);
            // 异常后回退到LISTENING状态
            session.recordError("LLM_EXCEPTION", e.getMessage());
            session.transitionTo(RtcSessionState.LISTENING);
            rtcSessionRepository.save(session);
        }
    }
    
    /**
     * 在事务中更新会话状态（仅对数据库操作加事务）
     */
    @Transactional
    protected void updateSessionStateInTransaction(RtcSession session, RtcSessionState newState, 
                                                   String grpcSessionId, String field) {
        session.transitionTo(newState);
        if (grpcSessionId != null && "asr".equals(field)) {
            session.setGrpcAsrSessionId(grpcSessionId);
        } else if (grpcSessionId != null && "tts".equals(field)) {
            session.setGrpcTtsSessionId(grpcSessionId);
        }
        rtcSessionRepository.save(session);
    }
    
    /**
     * 构建Chat请求
     */
    private ChatRequest buildChatRequest(RtcSession session, String userText) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 1. 添加系统提示词（如果有角色）
        if (session.getRoleId() != null) {
            Role role = roleRepository.findById(session.getRoleId()).orElse(null);
            if (role != null) {
                if (role.getPersonaPrompt() != null && !role.getPersonaPrompt().isEmpty()) {
                    messages.add(ChatMessage.system(role.getPersonaPrompt()));
                } else {
                    log.warn("角色没有设置系统提示词: roleId={}", session.getRoleId());
                }
            } else {
                // 角色已被删除，记录警告但继续执行
                log.warn("角色不存在或已被删除: roleId={}, sessionId={}", 
                        session.getRoleId(), session.getSessionId());
            }
        }
        
        // 2. 添加用户消息
        messages.add(ChatMessage.user(userText));
        
        // 3. 构建请求
        String modelName = systemConfigService.getString("rtc.default.model", "openai:gpt-4o-mini");
        
        return ChatRequest.builder()
                .messages(messages)
                .model(modelName)
                .temperature(0.7)
                .maxTokens(500) // RTC场景简短回复
                .userId(session.getUserId())
                .conversationId(session.getConversationId())
                .build();
    }
    
    /**
     * 启动TTS合成
     */
    private void startTtsSynthesis(String sessionId, String text, Long userId) {
        log.info("启动TTS合成: sessionId={}, text={}", sessionId, text);
        
        RtcSession session = rtcSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session == null) {
            return;
        }
        
        try {
            // 1. 更新会话状态
            session.transitionTo(RtcSessionState.SPEAKING);
            rtcSessionRepository.save(session);
            
            // 2. 初始化TTS片段计数器
            ttsSegmentCounters.put(sessionId, new AtomicInteger(0));
            
            // 3. 创建TTS配置
            TtsConfig ttsConfig = TtsConfig.newBuilder()
                    .setSessionId(sessionId)
                    .setVoiceId(systemConfigService.getString("rtc.audio.tts.voice-id", "zh_female_wwxkjx"))
                    .setLanguageCode("zh-CN")
                    .setAudioFormat(AudioFormat.PCM_16BIT)
                    .setSampleRate(48000)
                    .setChannels(1)
                    .setSpeed(1.0f)
                    .setVolume(0.8f)
                    .setPitch(1.0f)
                    .setEmotion(EmotionType.NEUTRAL)
                    .setEnableSsml(false)
                    .build();
            
            // 4. 创建TTS流式会话
            StreamObserver<TtsRequest> ttsObserver = grpcTtsClient.createStreamSession(
                    sessionId,
                    ttsConfig,
                    audioData -> handleTtsAudioData(sessionId, audioData),
                    progress -> handleTtsProgress(sessionId, progress),
                    error -> handleTtsError(sessionId, error)
            );
            
            // 缓存观察者
            StreamObservers observers = streamObservers.get(sessionId);
            if (observers != null) {
                observers.ttsObserver = ttsObserver;
            }
            
            // 记录TTS会话ID
            session.setGrpcTtsSessionId(sessionId);
            rtcSessionRepository.save(session);
            
            // 5. 发送文本（分段发送）
            List<String> segments = splitTextToSegments(text);
            for (int i = 0; i < segments.size(); i++) {
                grpcTtsClient.sendTextData(ttsObserver, segments.get(i), i, i == segments.size() - 1);
            }
            
            log.info("TTS文本已发送: sessionId={}, segments={}", sessionId, segments.size());
        } catch (Exception e) {
            log.error("TTS合成启动失败: sessionId={}", sessionId, e);
            session.recordError("TTS_START_ERROR", e.getMessage());
            rtcSessionRepository.save(session);
        }
    }
    
    /**
     * 处理TTS音频数据
     */
    private void handleTtsAudioData(String sessionId, TtsAudioData audioData) {
        log.debug("TTS音频数据: sessionId={}, segmentId={}, bytes={}", 
                sessionId, audioData.getSegmentId(), audioData.getAudioContent().size());
        
        // 音频数据通过grpc2rtp-tts桥接进程注入KMS，这里仅记录
        AtomicInteger counter = ttsSegmentCounters.get(sessionId);
        if (counter != null) {
            int count = counter.incrementAndGet();
            log.debug("TTS音频片段计数: sessionId={}, count={}", sessionId, count);
        }
    }
    
    /**
     * 处理TTS进度
     */
    private void handleTtsProgress(String sessionId, SynthesisProgress progress) {
        log.debug("TTS合成进度: sessionId={}, segmentId={}, progress={}%", 
                sessionId, progress.getSegmentId(), progress.getProgressPercentage());
    }
    
    /**
     * 处理TTS错误
     */
    private void handleTtsError(String sessionId, com.nexusvoice.infrastructure.rtc.grpc.common.ErrorInfo error) {
        log.error("TTS错误: sessionId={}, code={}, message={}", 
                sessionId, error.getErrorCode(), error.getErrorMessage());
        
        RtcSession session = rtcSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session != null) {
            session.recordError(error.getErrorCode(), error.getErrorMessage());
            rtcSessionRepository.save(session);
            
            // TTS流断开，自动触发会话清理
            if ("STREAM_ERROR".equals(error.getErrorCode())) {
                log.warn("TTS流异常断开，触发会话清理: sessionId={}", sessionId);
                try {
                    cleanupSessionResources(sessionId);
                } catch (Exception e) {
                    log.error("清理会话资源失败: sessionId={}", sessionId, e);
                }
            }
        }
    }
    
    /**
     * 执行打断
     * 
     * @param sessionId 会话ID
     * @param request 打断请求
     * @param userId 用户ID
     */
    @Transactional
    public void interrupt(String sessionId, InterruptRequestDto request, Long userId) {
        log.info("执行打断: sessionId={}, mode={}, reason={}", 
                sessionId, request.getMode(), request.getReason());
        
        // 1. 获取会话并验证权限
        RtcSession session = rtcSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.NOT_FOUND, "RTC会话不存在"));
        
        if (!session.getUserId().equals(userId)) {
            throw new BizException(ErrorCodeEnum.FORBIDDEN, "无权访问此会话");
        }
        
        // 2. 幂等性检查：只在SPEAKING状态才允许打断
        if (session.getState() != RtcSessionState.SPEAKING && session.getState() != RtcSessionState.THINKING) {
            log.warn("当前状态不允许打断: sessionId={}, state={}", sessionId, session.getState());
            return; // 幂等性：非播放状态不处理
        }
        
        // 3. 解析打断模式
        InterruptMode mode = InterruptMode.fromCode(request.getMode());
        if (mode == null) {
            throw new BizException(ErrorCodeEnum.BAD_REQUEST, "无效的打断模式");
        }
        
        // 4. 执行打断
        try {
            switch (mode) {
                case SOFT:
                    // 软中断：停止TTS源
                    performSoftInterrupt(sessionId);
                    break;
                case HARD:
                    // 硬中断：取消LLM+清空TTS
                    performHardInterrupt(sessionId);
                    break;
            }
            
            // 5. 更新会话（增加打断计数，回到LISTENING状态）
            session.incrementInterruptCount();
            session.transitionTo(RtcSessionState.LISTENING);
            rtcSessionRepository.save(session);
            
            log.info("打断执行成功: sessionId={}, mode={}", sessionId, mode);
        } catch (Exception e) {
            log.error("打断执行失败: sessionId={}", sessionId, e);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, 
                    "打断执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 软中断：停止TTS源
     */
    private void performSoftInterrupt(String sessionId) {
        log.info("执行软中断: sessionId={}", sessionId);
        
        StreamObservers observers = streamObservers.get(sessionId);
        if (observers != null && observers.ttsObserver != null) {
            // 发送TTS打断信号
            grpcTtsClient.sendInterrupt(observers.ttsObserver);
            
            log.info("TTS打断信号已发送: sessionId={}", sessionId);
        }
    }
    
    /**
     * 硬中断：取消LLM+清空TTS
     */
    private void performHardInterrupt(String sessionId) {
        log.info("执行硬中断: sessionId={}", sessionId);
        
        // 软中断+清空队列
        performSoftInterrupt(sessionId);
        
        // 清空累积文本
        StringBuffer accumulated = accumulatedTexts.get(sessionId);
        if (accumulated != null) {
            accumulated.setLength(0);
        }
        
        // 重置TTS片段计数
        AtomicInteger counter = ttsSegmentCounters.get(sessionId);
        if (counter != null) {
            counter.set(0);
        }
        
        log.info("硬中断完成: sessionId={}", sessionId);
    }
    
    /**
     * 结束会话
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    @Transactional
    public void endSession(String sessionId, Long userId) {
        log.info("结束RTC会话: sessionId={}, userId={}", sessionId, userId);
        
        // 1. 获取会话并验证权限
        RtcSession session = rtcSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.NOT_FOUND, "RTC会话不存在"));
        
        if (!session.getUserId().equals(userId)) {
            throw new BizException(ErrorCodeEnum.FORBIDDEN, "无权访问此会话");
        }
        
        // 2. 幂等性检查：如果已经结束，直接返回
        if (session.getState() == RtcSessionState.TERMINATED) {
            log.info("会话已经结束，忽略重复调用: sessionId={}", sessionId);
            return;
        }
        
        // 2. 关闭gRPC流
        StreamObservers observers = streamObservers.remove(sessionId);
        if (observers != null) {
            if (observers.asrObserver != null) {
                grpcAsrClient.endSession(observers.asrObserver, EndReason.NORMAL_END);
            }
            if (observers.ttsObserver != null) {
                grpcTtsClient.endSession(observers.ttsObserver, EndReason.NORMAL_END);
            }
        }
        
        // 3. 释放Kurento资源
        kurentoMediaService.releaseSession(sessionId);
        
        // 4. 清理缓存
        accumulatedTexts.remove(sessionId);
        ttsSegmentCounters.remove(sessionId);
        
        // 5. 更新会话
        session.transitionTo(RtcSessionState.TERMINATED);
        session.setEndedAt(LocalDateTime.now());
        rtcSessionRepository.save(session);
        
        log.info("RTC会话结束: sessionId={}", sessionId);
    }
    
    /**
     * 清理会话资源（用于异常情况）
     */
    private void cleanupSessionResources(String sessionId) {
        log.info("清理会话资源: sessionId={}", sessionId);
        
        // 关闭gRPC流
        StreamObservers observers = streamObservers.remove(sessionId);
        if (observers != null) {
            try {
                if (observers.asrObserver != null) {
                    grpcAsrClient.endSession(observers.asrObserver, EndReason.USER_INTERRUPT);
                }
            } catch (Exception e) {
                log.error("关闭ASR流失败: sessionId={}", sessionId, e);
            }
            
            try {
                if (observers.ttsObserver != null) {
                    grpcTtsClient.endSession(observers.ttsObserver, EndReason.USER_INTERRUPT);
                }
            } catch (Exception e) {
                log.error("关闭TTS流失败: sessionId={}", sessionId, e);
            }
        }
        
        // 释放Kurento资源
        try {
            kurentoMediaService.releaseSession(sessionId);
        } catch (Exception e) {
            log.error("释放Kurento资源失败: sessionId={}", sessionId, e);
        }
        
        // 清理缓存
        accumulatedTexts.remove(sessionId);
        ttsSegmentCounters.remove(sessionId);
    }
    
    /**
     * 分割文本为片段（短语可播）
     * 
     * 改进版：支持中英文标点，保留标点，处理连续标点
     */
    private List<String> splitTextToSegments(String text) {
        List<String> segments = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            return segments;
        }
        
        // 按中英文句子结束标点分割，保留标点
        String[] parts = text.split("(?<=[。！？；\\.!?;])\\s*");
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                // 过滤连续标点
                if (!trimmed.matches("^[，。！？、；,.!?;]+$")) {
                    segments.add(trimmed);
                }
            }
        }
        
        // 如果没有分割结果，返回原文
        if (segments.isEmpty()) {
            segments.add(text.trim());
        }
        
        log.debug("文本分段: 原文{}字，分为{}段", text.length(), segments.size());
        
        return segments;
    }
    
    /**
     * 流观察者缓存类
     */
    private static class StreamObservers {
        StreamObserver<AsrRequest> asrObserver;
        StreamObserver<TtsRequest> ttsObserver;
    }
}


