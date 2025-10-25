package com.nexusvoice.infrastructure.streaming.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;
import com.nexusvoice.infrastructure.streaming.StreamChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

/**
 * SSE (Server-Sent Events) 通道实现
 * 基于Spring SseEmitter封装的流式输出通道
 * 
 * 特点：
 * - 单向推送：服务端主动推送，客户端只能接收
 * - 轻量级：基于HTTP协议，无需建立WebSocket连接
 * - 自动重连：浏览器原生支持断线重连
 * - 适用场景：纯流式输出，不需要客户端主动发送消息
 * 
 * @author NexusVoice
 * @since 2025-10-25
 */
@Slf4j
public class SseChannel implements StreamChannel {
    
    private final SseEmitter emitter;
    private final ObjectMapper objectMapper;
    private final String sessionId;
    private final Long userId;
    private volatile boolean closed = false;
    
    /**
     * 构造SSE通道
     * 
     * @param emitter SSE发射器
     * @param objectMapper JSON序列化工具
     * @param userId 用户ID
     */
    public SseChannel(SseEmitter emitter, ObjectMapper objectMapper, Long userId) {
        this.emitter = emitter;
        this.objectMapper = objectMapper;
        this.sessionId = "sse-" + UUID.randomUUID().toString();
        this.userId = userId;
        
        // 注册SSE连接生命周期回调
        emitter.onCompletion(() -> {
            closed = true;
            log.debug("SSE连接正常关闭，sessionId={}", sessionId);
        });
        
        emitter.onTimeout(() -> {
            closed = true;
            log.warn("SSE连接超时，sessionId={}", sessionId);
        });
        
        emitter.onError(throwable -> {
            closed = true;
            log.error("SSE连接错误，sessionId={}", sessionId, throwable);
        });
        
        log.debug("SSE通道已创建，sessionId={}，用户ID={}", sessionId, userId);
    }
    
    @Override
    public void sendMessage(StreamChatResponse response) {
        if (closed) {
            log.debug("SSE连接已关闭，跳过发送消息：sessionId={}", sessionId);
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(response);
            // SSE格式：event: message\ndata: {json}\n\n
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(json));
        } catch (IOException e) {
            log.warn("发送SSE消息失败：sessionId={}，错误：{}", sessionId, e.getMessage());
            closed = true;
        } catch (Exception e) {
            log.error("发送SSE消息异常：sessionId={}", sessionId, e);
            closed = true;
        }
    }
    
    @Override
    public void sendError(String errorMessage) {
        sendMessage(StreamChatResponse.error(errorMessage));
        // SSE错误后关闭连接
        close();
    }
    
    @Override
    public boolean isOpen() {
        return !closed;
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                emitter.complete();
                log.debug("SSE连接已完成：sessionId={}", sessionId);
            } catch (Exception e) {
                log.warn("完成SSE发射器失败：sessionId={}", sessionId, e);
            }
        }
    }
    
    @Override
    public String getSessionId() {
        return sessionId;
    }
    
    @Override
    public Long getUserId() {
        return userId;
    }
    
    @Override
    public ChannelType getChannelType() {
        return ChannelType.SSE;
    }
}
