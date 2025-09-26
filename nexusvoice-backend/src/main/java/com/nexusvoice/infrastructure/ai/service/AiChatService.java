package com.nexusvoice.infrastructure.ai.service;

import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;

import java.util.function.Consumer;

/**
 * AI聊天服务接口
 * 封装与大模型的交互逻辑
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
public interface AiChatService {

    /**
     * 同步聊天请求
     * 适用于HTTP API调用
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式聊天请求
     * 适用于WebSocket流式输出
     */
    void streamChat(ChatRequest request, Consumer<StreamChatResponse> onNext, Consumer<Throwable> onError, Runnable onComplete);

    /**
     * 获取模型信息
     */
    String getModelName();

    /**
     * 检查模型是否可用
     */
    boolean isModelAvailable();

    /**
     * 估算文本的令牌数量
     */
    int estimateTokenCount(String text);
}
