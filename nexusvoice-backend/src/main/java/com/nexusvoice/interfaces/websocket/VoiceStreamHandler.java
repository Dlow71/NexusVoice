package com.nexusvoice.interfaces.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.application.voice.dto.VoiceRealtimeEventDto;
import com.nexusvoice.application.voice.dto.VoiceSessionRuntimeConfigDto;
import com.nexusvoice.application.voice.dto.VoiceSessionRuntimeUpdateRequest;
import com.nexusvoice.application.voice.dto.VoiceTurnResultDto;
import com.nexusvoice.application.voice.service.VoiceSessionApplicationService;
import com.nexusvoice.domain.voice.model.VoiceSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 语音实时WebSocket入口。
 * 当前阶段优先打通语音会话、文本轮次与TTS分段回放骨架。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceStreamHandler extends TextWebSocketHandler {

    private final VoiceSessionApplicationService voiceSessionApplicationService;
    private final ObjectMapper objectMapper;

    private final Map<String, String> wsToVoiceSession = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            sendError(session, null, "用户未认证");
            return;
        }

        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = root.path("type").asText();
        String voiceSessionId = root.path("voiceSessionId").asText(null);
        JsonNode payload = root.path("payload");

        switch (type) {
            case "SESSION_INIT" -> handleSessionInit(session, voiceSessionId, payload, userId);
            case "TEXT_UTTERANCE" -> handleTextUtterance(session, voiceSessionId, payload, userId);
            case "AUDIO_CHUNK" -> handleAudioChunk(session, voiceSessionId, payload, userId);
            case "AUDIO_END" -> handleAudioEnd(session, voiceSessionId, payload, userId);
            case "UPDATE_CONFIG" -> handleUpdateConfig(session, voiceSessionId, payload, userId);
            case "INTERRUPT" -> handleInterrupt(session, voiceSessionId, userId);
            case "PING" -> sendEvent(session, VoiceRealtimeEventDto.builder()
                    .type("PONG")
                    .voiceSessionId(voiceSessionId)
                    .seq(nextSeq())
                    .ts(System.currentTimeMillis())
                    .payload(Map.of("ok", true))
                    .build());
            default -> sendError(session, voiceSessionId, "未知语音事件类型: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        wsToVoiceSession.remove(session.getId());
        log.info("语音WebSocket连接关闭: wsSessionId={}, status={}", session.getId(), status);
    }

    private void handleSessionInit(WebSocketSession session,
                                   String voiceSessionId,
                                   JsonNode payload,
                                   Long userId) {
        String clientCapabilities = payload.isMissingNode() ? null : payload.path("clientCapabilities").toString();
        VoiceSessionRuntimeConfigDto runtimeConfig = voiceSessionApplicationService.initializeRealtimeSession(
                voiceSessionId,
                clientCapabilities,
                userId
        );
        wsToVoiceSession.put(session.getId(), voiceSessionId);

        sendEvent(session, VoiceRealtimeEventDto.builder()
                .type("SESSION_READY")
                .voiceSessionId(voiceSessionId)
                .seq(nextSeq())
                .ts(System.currentTimeMillis())
                .payload(Map.of(
                        "state", runtimeConfig.getState(),
                        "conversationId", runtimeConfig.getConversationId()
                ))
                .build());
        sendContextStatus(session, voiceSessionId, runtimeConfig);
    }

    private void handleTextUtterance(WebSocketSession session,
                                     String voiceSessionId,
                                     JsonNode payload,
                                     Long userId) {
        String text = payload.path("text").asText(null);
        if (text == null || text.isBlank()) {
            sendError(session, voiceSessionId, "语音文本不能为空");
            return;
        }

        sendEvent(session, VoiceRealtimeEventDto.builder()
                .type("USER_TRANSCRIPT_FINAL")
                .voiceSessionId(voiceSessionId)
                .seq(nextSeq())
                .ts(System.currentTimeMillis())
                .payload(Map.of("text", text.trim()))
                .build());
        sendAssistantAudioStatus(session, voiceSessionId, null, "ASSISTANT_AUDIO_GENERATING", "语音生成中", null);
        sendStateChanged(session, voiceSessionId, "READY", "UNDERSTANDING", null);

        VoiceTurnResultDto result = voiceSessionApplicationService.processTextUtterance(voiceSessionId, text, userId);
        publishTurnResult(session, voiceSessionId, result);
        sendContextStatus(session, voiceSessionId, voiceSessionApplicationService.getRuntimeConfig(voiceSessionId, userId));
        sendStateChanged(session, voiceSessionId, "UNDERSTANDING", "READY", result.getTurnNo());
    }

    private void handleAudioChunk(WebSocketSession session,
                                  String voiceSessionId,
                                  JsonNode payload,
                                  Long userId) {
        voiceSessionApplicationService.appendAudioChunk(
                voiceSessionId,
                payload.path("base64Audio").asText(null),
                payload.path("filename").asText(null),
                payload.path("contentType").asText(null),
                userId
        );
        sendStateChanged(session, voiceSessionId, "READY", "USER_SPEAKING", null);
    }

    private void handleAudioEnd(WebSocketSession session,
                                String voiceSessionId,
                                JsonNode payload,
                                Long userId) {
        sendStateChanged(session, voiceSessionId, "USER_SPEAKING", "UNDERSTANDING", null);
        VoiceTurnResultDto result = voiceSessionApplicationService.completeAudioInput(
                voiceSessionId,
                payload.path("filename").asText(null),
                payload.path("contentType").asText(null),
                payload.path("asrModelKey").asText(null),
                userId
        );
        sendEvent(session, VoiceRealtimeEventDto.builder()
                .type("USER_TRANSCRIPT_FINAL")
                .voiceSessionId(voiceSessionId)
                .turnNo(result.getTurnNo())
                .seq(nextSeq())
                .ts(System.currentTimeMillis())
                .payload(Map.of("text", result.getUserTranscript()))
                .build());
        sendAssistantAudioStatus(session, voiceSessionId, result.getTurnNo(), "ASSISTANT_AUDIO_GENERATING", "语音生成中", null);

        publishTurnResult(session, voiceSessionId, result);
        sendContextStatus(session, voiceSessionId, voiceSessionApplicationService.getRuntimeConfig(voiceSessionId, userId));
        sendStateChanged(session, voiceSessionId, "UNDERSTANDING", "READY", result.getTurnNo());
    }

    private void publishTurnResult(WebSocketSession session,
                                   String voiceSessionId,
                                   VoiceTurnResultDto result) {
        if (result.getReasoningContent() != null && !result.getReasoningContent().isBlank()) {
            sendEvent(session, VoiceRealtimeEventDto.builder()
                    .type("THINKING_START")
                    .voiceSessionId(voiceSessionId)
                    .turnNo(result.getTurnNo())
                    .seq(nextSeq())
                    .ts(System.currentTimeMillis())
                    .payload(Map.of("turnNo", result.getTurnNo()))
                    .build());
            sendEvent(session, VoiceRealtimeEventDto.builder()
                    .type("THINKING_DELTA")
                    .voiceSessionId(voiceSessionId)
                    .turnNo(result.getTurnNo())
                    .seq(nextSeq())
                    .ts(System.currentTimeMillis())
                    .payload(Map.of("delta", result.getReasoningContent()))
                    .build());
            sendEvent(session, VoiceRealtimeEventDto.builder()
                    .type("THINKING_END")
                    .voiceSessionId(voiceSessionId)
                    .turnNo(result.getTurnNo())
                    .seq(nextSeq())
                    .ts(System.currentTimeMillis())
                    .payload(Map.of("turnNo", result.getTurnNo()))
                    .build());
        }

        sendEvent(session, VoiceRealtimeEventDto.builder()
                .type("ASSISTANT_TEXT_FINAL")
                .voiceSessionId(voiceSessionId)
                .turnNo(result.getTurnNo())
                .seq(nextSeq())
                .ts(System.currentTimeMillis())
                .payload(Map.of(
                        "displayText", result.getDisplayText(),
                        "spokenText", result.getSpokenText(),
                        "citations", result.getCitations() != null ? result.getCitations() : java.util.List.of()
                ))
                .build());

        if ("GENERATED".equalsIgnoreCase(result.getAudioStatus())) {
            sendAssistantAudioStatus(
                    session,
                    voiceSessionId,
                    result.getTurnNo(),
                    "ASSISTANT_AUDIO_READY",
                    result.getAudioStatusMessage(),
                    result.getAudioSegments() != null ? result.getAudioSegments().size() : 0
            );
        } else {
            sendAssistantAudioStatus(
                    session,
                    voiceSessionId,
                    result.getTurnNo(),
                    "ASSISTANT_AUDIO_FAILED",
                    result.getAudioStatusMessage(),
                    0
            );
        }

        if (result.getAudioSegments() != null && !result.getAudioSegments().isEmpty()) {
            for (VoiceTurnResultDto.AudioSegmentDto segment : result.getAudioSegments()) {
                sendEvent(session, VoiceRealtimeEventDto.builder()
                        .type("ASSISTANT_AUDIO_SEGMENT")
                        .voiceSessionId(voiceSessionId)
                        .turnNo(result.getTurnNo())
                        .seq(nextSeq())
                        .ts(System.currentTimeMillis())
                        .payload(Map.of(
                                "segmentIndex", segment.getSegmentIndex(),
                                "displayText", segment.getDisplayText(),
                                "spokenText", segment.getSpokenText(),
                                "audioUrl", segment.getAudioUrl(),
                                "isLast", Boolean.TRUE.equals(segment.getIsLast())
                        ))
                        .build());
            }
            sendEvent(session, VoiceRealtimeEventDto.builder()
                    .type("ASSISTANT_AUDIO_END")
                    .voiceSessionId(voiceSessionId)
                    .turnNo(result.getTurnNo())
                    .seq(nextSeq())
                    .ts(System.currentTimeMillis())
                    .payload(Map.of("turnNo", result.getTurnNo()))
                    .build());
        }
    }

    private void sendAssistantAudioStatus(WebSocketSession session,
                                          String voiceSessionId,
                                          Integer turnNo,
                                          String type,
                                          String message,
                                          Integer segmentCount) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        if (message != null && !message.isBlank()) {
            payload.put("message", message);
        }
        if (segmentCount != null) {
            payload.put("segmentCount", segmentCount);
        }
        sendEvent(session, VoiceRealtimeEventDto.builder()
                .type(type)
                .voiceSessionId(voiceSessionId)
                .turnNo(turnNo)
                .seq(nextSeq())
                .ts(System.currentTimeMillis())
                .payload(payload)
                .build());
    }

    private void handleUpdateConfig(WebSocketSession session,
                                    String voiceSessionId,
                                    JsonNode payload,
                                    Long userId) {
        VoiceSessionRuntimeUpdateRequest request = objectMapper.convertValue(payload, VoiceSessionRuntimeUpdateRequest.class);
        VoiceSessionRuntimeConfigDto runtimeConfig = voiceSessionApplicationService.updateRuntimeConfig(
                voiceSessionId,
                request,
                userId
        );
        sendEvent(session, VoiceRealtimeEventDto.builder()
                .type("RUNTIME_CONFIG_UPDATED")
                .voiceSessionId(voiceSessionId)
                .seq(nextSeq())
                .ts(System.currentTimeMillis())
                .payload(runtimeConfig)
                .build());
        sendContextStatus(session, voiceSessionId, runtimeConfig);
    }

    private void handleInterrupt(WebSocketSession session, String voiceSessionId, Long userId) {
        voiceSessionApplicationService.interrupt(voiceSessionId, userId);
        sendStateChanged(session, voiceSessionId, "RESPONDING_AUDIO", "READY", null);
    }

    private void sendContextStatus(WebSocketSession session,
                                   String voiceSessionId,
                                   VoiceSessionRuntimeConfigDto runtimeConfig) {
        var snapshot = runtimeConfig.getRuntimeConfig() != null ? runtimeConfig.getRuntimeConfig().getContextSnapshot() : null;
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("estimatedUsedTokens", snapshot != null ? snapshot.getEstimatedInputTokens() : 0);
        payload.put("windowTokens", snapshot != null ? snapshot.getModelContextWindow() : 0);
        payload.put("usageRatio", buildRatio(snapshot));
        payload.put("strategy", runtimeConfig.getRuntimeConfig() != null && runtimeConfig.getRuntimeConfig().getPolicy() != null
                ? runtimeConfig.getRuntimeConfig().getPolicy().getContextStrategy() : null);
        payload.put("compactApplied", snapshot != null
                && snapshot.getCompactSummaryTokens() != null
                && snapshot.getCompactSummaryTokens() > 0);
        sendEvent(session, VoiceRealtimeEventDto.builder()
                .type("CONTEXT_STATUS")
                .voiceSessionId(voiceSessionId)
                .seq(nextSeq())
                .ts(System.currentTimeMillis())
                .payload(payload)
                .build());
    }

    private double buildRatio(com.nexusvoice.application.conversation.dto.ConversationContextSnapshotDto snapshot) {
        if (snapshot == null || snapshot.getModelContextWindow() == null || snapshot.getModelContextWindow() <= 0) {
            return 0D;
        }
        int used = snapshot.getEstimatedInputTokens() != null ? snapshot.getEstimatedInputTokens() : 0;
        return Math.min(1D, Math.max(0D, (double) used / snapshot.getModelContextWindow()));
    }

    private void sendStateChanged(WebSocketSession session, String voiceSessionId, String from, String to, Integer turnNo) {
        sendEvent(session, VoiceRealtimeEventDto.builder()
                .type("STATE_CHANGED")
                .voiceSessionId(voiceSessionId)
                .turnNo(turnNo)
                .seq(nextSeq())
                .ts(System.currentTimeMillis())
                .payload(Map.of("from", from, "to", to))
                .build());
    }

    private void sendError(WebSocketSession session, String voiceSessionId, String message) {
        sendEvent(session, VoiceRealtimeEventDto.builder()
                .type("ERROR")
                .voiceSessionId(voiceSessionId)
                .seq(nextSeq())
                .ts(System.currentTimeMillis())
                .payload(Map.of("message", message))
                .build());
    }

    private void sendEvent(WebSocketSession session, VoiceRealtimeEventDto event) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
        } catch (IOException e) {
            log.error("发送语音实时事件失败: wsSessionId={}, eventType={}", session.getId(), event.getType(), e);
        }
    }

    private long nextSeq() {
        return sequence.incrementAndGet();
    }
}
