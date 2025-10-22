package com.nexusvoice.infrastructure.config;

import com.nexusvoice.interfaces.websocket.ChatStreamHandler;
import com.nexusvoice.infrastructure.websocket.WebSocketJwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

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
                .setAllowedOrigins("*"); // 允许所有来源（生产环境需要限制）
                // 注意：移除.withSockJS()，使用原生WebSocket协议
                // 支持子协议传递token：Bearer.{token}
    }
    
    /**
     * 配置WebSocket消息大小限制
     * 增加文本消息和二进制消息的最大大小，以支持Base64编码的图片传输
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 设置文本消息缓冲区大小为20MB（支持多张图片）
        container.setMaxTextMessageBufferSize(20 * 1024 * 1024);
        // 设置二进制消息缓冲区大小为20MB
        container.setMaxBinaryMessageBufferSize(20 * 1024 * 1024);
        // 设置会话空闲超时时间（30分钟）
        container.setMaxSessionIdleTimeout(30 * 60 * 1000L);
        return container;
    }
}
