package com.nexusvoice.infrastructure.config;

import com.nexusvoice.interfaces.websocket.ChatStreamHandler;
import com.nexusvoice.infrastructure.websocket.WebSocketJwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 配置聊天流式接口，集成JWT认证
 * 
 * @author NexusVoice
 * @since 2025-09-25
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatStreamHandler chatStreamHandler;
    private final WebSocketJwtInterceptor webSocketJwtInterceptor;

    @Autowired
    public WebSocketConfig(ChatStreamHandler chatStreamHandler, 
                          WebSocketJwtInterceptor webSocketJwtInterceptor) {
        this.chatStreamHandler = chatStreamHandler;
        this.webSocketJwtInterceptor = webSocketJwtInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册流式聊天WebSocket端点，添加JWT认证拦截器
        registry.addHandler(chatStreamHandler, "/ws/chat/stream")
                .setAllowedOriginPatterns("*") // 生产环境应该收紧域名限制
                .addInterceptors(webSocketJwtInterceptor) // 添加JWT认证拦截器
                .withSockJS(); // 支持SockJS降级
    }
}
