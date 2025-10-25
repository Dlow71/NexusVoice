package com.nexusvoice.infrastructure.streaming;

import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;

/**
 * 流式输出通道抽象接口
 * 支持WebSocket、SSE等多种传输协议，实现流式聊天的协议解耦
 * 
 * 设计理念：
 * - 协议无关：业务逻辑与具体传输协议完全分离
 * - 易于扩展：新增协议只需实现此接口
 * - 统一管理：会话生命周期和错误处理统一化
 * 
 * @author NexusVoice
 * @since 2025-10-25
 */
public interface StreamChannel {
    
    /**
     * 发送流式响应消息
     * 
     * @param response 流式响应消息，包含内容增量、TTS片段等
     */
    void sendMessage(StreamChatResponse response);
    
    /**
     * 发送错误消息
     * 
     * @param errorMessage 错误描述信息
     */
    void sendError(String errorMessage);
    
    /**
     * 通道是否仍然打开
     * 
     * @return true-通道打开，false-通道已关闭
     */
    boolean isOpen();
    
    /**
     * 关闭通道并释放资源
     */
    void close();
    
    /**
     * 获取会话ID（用于日志记录和会话管理）
     * 
     * @return 会话唯一标识
     */
    String getSessionId();
    
    /**
     * 获取用户ID
     * 
     * @return 用户ID
     */
    Long getUserId();
    
    /**
     * 获取通道类型
     * 
     * @return 通道类型枚举
     */
    ChannelType getChannelType();
    
    /**
     * 流式通道类型枚举
     */
    enum ChannelType {
        /**
         * WebSocket协议 - 支持双向通信，适合实时交互场景
         */
        WEBSOCKET("WebSocket", "双向实时通信协议"),
        
        /**
         * Server-Sent Events协议 - 单向流式输出，轻量级，适合纯流式场景
         */
        SSE("Server-Sent Events", "服务器推送事件协议");
        
        private final String name;
        private final String description;
        
        ChannelType(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
