package com.nexusvoice.interfaces.api.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.application.conversation.dto.ChatRequestDto;
import com.nexusvoice.infrastructure.streaming.StreamChannel;
import com.nexusvoice.infrastructure.streaming.impl.SseChannel;
import com.nexusvoice.interfaces.websocket.ChatStreamHandler;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * SSE流式聊天控制器
 * 基于Server-Sent Events实现轻量级流式输出
 * 
 * SSE特点：
 * - 单向推送：服务端主动推送，客户端只能接收
 * - 轻量级：基于HTTP协议，无需建立WebSocket连接
 * - 自动重连：浏览器原生支持断线重连
 * - 适用场景：纯流式输出，不需要客户端主动发送消息
 * 
 * @author NexusVoice
 * @since 2025-10-25
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "SSE流式聊天", description = "基于Server-Sent Events的轻量级流式聊天接口")
@Validated
public class SseStreamController {
    
    private final ChatStreamHandler chatStreamHandler;
    private final ObjectMapper objectMapper;
    private final ExecutorService sseVirtualThreadExecutor;
    
    public SseStreamController(
            ChatStreamHandler chatStreamHandler, 
            ObjectMapper objectMapper,
            @Qualifier("sseVirtualThreadExecutor") ExecutorService sseVirtualThreadExecutor) {
        this.chatStreamHandler = chatStreamHandler;
        this.objectMapper = objectMapper;
        this.sseVirtualThreadExecutor = sseVirtualThreadExecutor;
    }
    
    /**
     * SSE流式聊天接口
     * 
     * 使用方式：
     * 1. 前端发送POST请求，body包含聊天请求参数
     * 2. 后端返回SseEmitter，开始流式推送
     * 3. 前端通过EventSource监听message事件接收数据
     * 
     * 注意事项：
     * - SSE基于HTTP长连接，默认30分钟超时
     * - 支持所有ChatRequestDto参数（联网搜索、图像输入、TTS等）
     * - 完全复用WebSocket的业务逻辑，保证功能一致性
     * 
     * @param request 聊天请求参数
     * @return SSE发射器，用于流式推送响应
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireAuth
    @Operation(
        summary = "SSE流式聊天", 
        description = "使用Server-Sent Events进行流式AI对话，支持联网搜索、图像输入、分段TTS等所有功能。" +
                     "相比WebSocket更轻量，适合纯流式输出场景。客户端使用EventSource接收消息。"
    )
    public SseEmitter streamChat(@Valid @RequestBody ChatRequestDto request) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow(() -> 
            new RuntimeException("用户未认证"));
        
        log.info("SSE流式聊天请求，用户ID：{}，对话ID：{}，联网搜索：{}，语音合成：{}",
                userId, request.getConversationId(), 
                request.getEnableWebSearch() != null && request.getEnableWebSearch(),
                request.getEnableAudio() != null && request.getEnableAudio());
        
        // 创建SSE发射器（30分钟超时）
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 创建SSE通道抽象
        StreamChannel channel = new SseChannel(emitter, objectMapper, userId);
        
        // 异步处理（使用SSE专用虚拟线程池，避免阻塞HTTP线程）
        CompletableFuture.runAsync(() -> {
            try {
                // 调用ChatStreamHandler的核心方法（完全复用WebSocket逻辑）
                chatStreamHandler.handleStreamChatInternal(channel, request, userId);
            } catch (Exception e) {
                log.error("SSE流式聊天处理失败，用户ID：{}", userId, e);
                channel.sendError("系统繁忙，请稍后重试");
                channel.close();
            }
        }, sseVirtualThreadExecutor);  // ⬅️ 指定使用SSE虚拟线程池
        
        return emitter;
    }
}
