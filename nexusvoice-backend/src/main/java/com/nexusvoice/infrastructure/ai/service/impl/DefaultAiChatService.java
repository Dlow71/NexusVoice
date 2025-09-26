package com.nexusvoice.infrastructure.ai.service.impl;

import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;

import java.util.function.Consumer;

/**
 * 默认的 AI 聊天服务占位实现
 * 当未配置或未启用真实的 AI 服务实现时，提供可用的占位实现。
 * Bean 的声明由配置类负责（见 AiServiceConfig）。
 */
public class DefaultAiChatService implements AiChatService {

    private static final String UNAVAILABLE_MSG = "AI服务未启用或配置不完整，请检查 langchain4j.open-ai.api-key 配置";

    @Override
    public ChatResponse chat(ChatRequest request) {
        return ChatResponse.error(UNAVAILABLE_MSG);
    }

    @Override
    public void streamChat(ChatRequest request,
                           Consumer<StreamChatResponse> onNext,
                           Consumer<Throwable> onError,
                           Runnable onComplete) {
        try {
            onNext.accept(StreamChatResponse.error(UNAVAILABLE_MSG));
        } catch (Exception ignore) {
        } finally {
            onComplete.run();
        }
    }

    @Override
    public String getModelName() {
        return "unconfigured";
    }

    @Override
    public boolean isModelAvailable() {
        return false;
    }

    @Override
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / 3.0);
    }
}
