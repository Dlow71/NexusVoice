package com.nexusvoice.infrastructure.streaming.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;
import com.nexusvoice.infrastructure.streaming.StreamChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * WebSocket通道实现
 * 基于Spring WebSocket封装的流式输出通道
 * 
 * 特点：
 * - 双向通信：支持客户端主动发送消息
 * - 长连接：适合实时交互场景
 * - 线程安全：使用synchronized保证并发安全
 * 
 * @author NexusVoice
 * @since 2025-10-25
 */
@Slf4j
public class WebSocketChannel implements StreamChannel {
    
    private final WebSocketSession session;
    private final ObjectMapper objectMapper;
    private final Long userId;
    
    /**
     * 构造WebSocket通道
     * 
     * @param session WebSocket会话对象
     * @param objectMapper JSON序列化工具
     * @param userId 用户ID
     */
    public WebSocketChannel(WebSocketSession session, ObjectMapper objectMapper, Long userId) {
        this.session = session;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }
    
    @Override
    public void sendMessage(StreamChatResponse response) {
        try {
            // 使用synchronized避免并发写入同一Session导致异常
            synchronized (session) {
                if (session.isOpen()) {
                    String json = objectMapper.writeValueAsString(response);
                    session.sendMessage(new TextMessage(json));
                } else {
                    log.debug("WebSocket会话已关闭，跳过发送消息：sessionId={}", getSessionId());
                }
            }
        } catch (IllegalStateException e) {
            // 会话已关闭，静默处理（正常流程）
            log.debug("WebSocket会话已关闭，无法发送消息：sessionId={}", getSessionId());
        } catch (IOException e) {
            log.warn("发送WebSocket消息失败：sessionId={}，错误：{}", getSessionId(), e.getMessage());
        } catch (Exception e) {
            log.error("发送WebSocket消息异常：sessionId={}", getSessionId(), e);
        }
    }
    
    @Override
    public void sendError(String errorMessage) {
        sendMessage(StreamChatResponse.error(errorMessage));
    }
    
    @Override
    public boolean isOpen() {
        return session != null && session.isOpen();
    }
    
    @Override
    public void close() {
        try {
            if (session != null && session.isOpen()) {
                session.close(CloseStatus.NORMAL);
                log.debug("WebSocket通道已关闭：sessionId={}", getSessionId());
            }
        } catch (IOException e) {
            log.warn("关闭WebSocket会话失败：sessionId={}", getSessionId(), e);
        }
    }
    
    @Override
    public String getSessionId() {
        return session != null ? session.getId() : "unknown";
    }
    
    @Override
    public Long getUserId() {
        return userId;
    }
    
    @Override
    public ChannelType getChannelType() {
        return ChannelType.WEBSOCKET;
    }
}
