package com.nexusvoice.infrastructure.config;

import com.nexusvoice.interfaces.websocket.ChatStreamHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 配置聊天流式接口
 * 
 * @author NexusVoice
 * @since 2025-09-25
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatStreamHandler chatStreamHandler;

    @Autowired
    public WebSocketConfig(ChatStreamHandler chatStreamHandler) {
        this.chatStreamHandler = chatStreamHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册流式聊天WebSocket端点
        registry.addHandler(chatStreamHandler, "/ws/chat/stream")
                .setAllowedOriginPatterns("*") // 生产环境应该收紧域名限制
                .withSockJS(); // 支持SockJS降级
    }
}
