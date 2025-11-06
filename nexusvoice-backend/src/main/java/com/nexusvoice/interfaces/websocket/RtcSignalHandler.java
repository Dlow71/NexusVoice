package com.nexusvoice.interfaces.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nexusvoice.application.rtc.dto.InterruptRequestDto;
import com.nexusvoice.application.rtc.service.RtcSessionApplicationService;
import com.nexusvoice.domain.rtc.enums.SignalMessageType;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.rtc.kurento.WebRtcEndpointManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RTC信令WebSocket处理器
 * 
 * 职责：
 * - 处理WebRTC信令消息（OFFER/ANSWER/ICE_CANDIDATE）
 * - 处理打断消息（INTERRUPT）
 * - 处理状态更新（STATE_UPDATE）
 * - 心跳保活
 * 
 * 信令消息格式：
 * {
 *   "type": "OFFER|ANSWER|ICE_CANDIDATE|INTERRUPT|STATE_UPDATE|HEARTBEAT",
 *   "sessionId": "uuid",
 *   "seq": 123,
 *   "ts": 1698765432000,
 *   "idempotencyKey": "xxx",
 *   "payload": { ... }
 * }
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rtc.enabled", havingValue = "true", matchIfMissing = false)
public class RtcSignalHandler extends TextWebSocketHandler {
    
    private final RtcSessionApplicationService rtcSessionApplicationService;
    private final WebRtcEndpointManager webRtcEndpointManager;
    private final ObjectMapper objectMapper;
    
    // 活跃WebSocket会话（wsSessionId -> WebSocketSession）
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    // RTC会话ID映射（wsSessionId -> rtcSessionId）
    private final Map<String, String> sessionIdMapping = new ConcurrentHashMap<>();
    
    // 已处理的幂等键（使用Caffeine缓存，5分钟自动过期，避免内存泄漏）
    private final Cache<String, Long> processedIdempotencyKeys = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(10000)
            .build();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String wsSessionId = session.getId();
        activeSessions.put(wsSessionId, session);
        
        // 从会话属性获取用户信息（由WebSocketJwtInterceptor设置）
        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");
        
        log.info("RTC信令WebSocket连接建立: wsSessionId={}, userId={}, username={}", 
                wsSessionId, userId, username);
        
        // 发送连接成功消息
        sendMessage(session, Map.of(
                "type", "CONNECTED",
                "message", "RTC信令连接成功，欢迎 " + username + "！"
        ));
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String wsSessionId = session.getId();
        Long userId = (Long) session.getAttributes().get("userId");
        
        if (userId == null) {
            sendError(session, "用户未认证");
            return;
        }
        
        try {
            // 解析信令消息
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.get("type").asText();
            String rtcSessionId = root.has("sessionId") ? root.get("sessionId").asText() : null;
            Long seq = root.has("seq") ? root.get("seq").asLong() : null;
            String idempotencyKey = root.has("idempotencyKey") ? root.get("idempotencyKey").asText() : null;
            
            log.debug("收到RTC信令消息: type={}, sessionId={}, seq={}", type, rtcSessionId, seq);
            
            // 幂等性检查（按用户作用域）
            String scopedKey = idempotencyKey != null ? (userId + ":" + idempotencyKey) : null;
            if (scopedKey != null && processedIdempotencyKeys.getIfPresent(scopedKey) != null) {
                log.warn("重复的幂等键，忽略消息: idempotencyKey={}", idempotencyKey);
                return;
            }
            
            // 建立会话映射
            if (rtcSessionId != null) {
                sessionIdMapping.put(wsSessionId, rtcSessionId);
            }
            
            // 处理消息
            SignalMessageType messageType = SignalMessageType.fromCode(type);
            if (messageType == null) {
                sendError(session, "未知的消息类型: " + type);
                return;
            }
            
            switch (messageType) {
                case OFFER:
                    handleOffer(session, rtcSessionId, root, userId);
                    break;
                case ICE_CANDIDATE:
                    handleIceCandidate(session, rtcSessionId, root, userId);
                    break;
                case INTERRUPT:
                    handleInterrupt(session, rtcSessionId, root, userId);
                    break;
                case HEARTBEAT:
                    handleHeartbeat(session);
                    break;
                default:
                    log.warn("不支持的消息类型: {}", messageType);
            }
            
            // 记录幂等键（5分钟后自动过期）
            if (scopedKey != null) {
                processedIdempotencyKeys.put(scopedKey, System.currentTimeMillis());
            }
            
        } catch (BizException e) {
            log.warn("处理RTC信令失败: wsSessionId={}, error={}", wsSessionId, e.getMessage());
            sendError(session, e.getMessage());
        } catch (Exception e) {
            log.error("处理RTC信令异常: wsSessionId={}", wsSessionId, e);
            sendError(session, "信令处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理SDP Offer
     */
    private void handleOffer(WebSocketSession session, String rtcSessionId, JsonNode root, Long userId) {
        String sdpOffer = root.get("sdp").asText();
        
        log.info("处理SDP Offer: sessionId={}", rtcSessionId);
        
        // 调用Application层处理
        String sdpAnswer = rtcSessionApplicationService.processSdpOffer(rtcSessionId, sdpOffer, userId);
        
        // 返回Answer
        sendMessage(session, Map.of(
                "type", "ANSWER",
                "sessionId", rtcSessionId,
                "sdp", sdpAnswer,
                "seq", root.get("seq").asLong() + 1,
                "ts", System.currentTimeMillis()
        ));
        
        // SDP协商完成后自动启动ASR流（幂等保护在服务内部处理）
        try {
            rtcSessionApplicationService.startAsrStream(rtcSessionId, userId);
        } catch (Exception e) {
            log.error("自动启动ASR流失败: sessionId={}", rtcSessionId, e);
        }

        log.info("SDP Answer已发送: sessionId={}", rtcSessionId);
    }
    
    /**
     * 处理ICE候选者
     */
    private void handleIceCandidate(WebSocketSession session, String rtcSessionId, JsonNode root, Long userId) {
        JsonNode candidateNode = root.get("candidate");
        String candidate = candidateNode.get("candidate").asText();
        String sdpMid = candidateNode.get("sdpMid").asText();
        int sdpMLineIndex = candidateNode.get("sdpMLineIndex").asInt();
        
        log.debug("处理ICE候选者: sessionId={}, candidate={}", rtcSessionId, candidate);
        
        // 添加ICE候选者到WebRtcEndpoint
        webRtcEndpointManager.addIceCandidate(rtcSessionId, candidate, sdpMid, sdpMLineIndex);
        
        // 发送ACK
        sendMessage(session, Map.of(
                "type", "ICE_CANDIDATE_ACK",
                "sessionId", rtcSessionId,
                "seq", root.get("seq").asLong(),
                "ts", System.currentTimeMillis()
        ));
    }
    
    /**
     * 处理打断
     */
    private void handleInterrupt(WebSocketSession session, String rtcSessionId, JsonNode root, Long userId) {
        String mode = root.get("mode").asText();
        String reason = root.has("reason") ? root.get("reason").asText() : "user_barge_in";
        
        log.info("处理打断信号: sessionId={}, mode={}, reason={}", rtcSessionId, mode, reason);
        
        InterruptRequestDto request = new InterruptRequestDto();
        request.setMode(mode);
        request.setReason(reason);
        
        // 调用Application层处理
        rtcSessionApplicationService.interrupt(rtcSessionId, request, userId);
        
        // 发送打断确认
        sendMessage(session, Map.of(
                "type", "INTERRUPT_ACK",
                "sessionId", rtcSessionId,
                "mode", mode,
                "seq", root.get("seq").asLong(),
                "ts", System.currentTimeMillis()
        ));
    }
    
    /**
     * 处理心跳
     */
    private void handleHeartbeat(WebSocketSession session) {
        sendMessage(session, Map.of(
                "type", "HEARTBEAT_ACK",
                "ts", System.currentTimeMillis()
        ));
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String wsSessionId = session.getId();
        String rtcSessionId = sessionIdMapping.remove(wsSessionId);
        activeSessions.remove(wsSessionId);
        
        log.info("RTC信令WebSocket连接关闭: wsSessionId={}, rtcSessionId={}, status={}", 
                wsSessionId, rtcSessionId, status);
        
        // 如果有RTC会话，触发清理
        if (rtcSessionId != null) {
            Long userId = (Long) session.getAttributes().get("userId");
            if (userId != null) {
                try {
                    rtcSessionApplicationService.endSession(rtcSessionId, userId);
                } catch (Exception e) {
                    log.error("清理RTC会话失败: rtcSessionId={}", rtcSessionId, e);
                }
            }
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String wsSessionId = session.getId();
        log.error("RTC信令WebSocket传输错误: wsSessionId={}", wsSessionId, exception);
        
        sendError(session, "连接出现错误: " + exception.getMessage());
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("发送WebSocket消息失败: sessionId={}", session.getId(), e);
        }
    }
    
    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        sendMessage(session, Map.of(
                "type", "ERROR",
                "message", errorMessage,
                "ts", System.currentTimeMillis()
        ));
    }
}







