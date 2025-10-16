package com.nexusvoice.domain.ai.repository;

import com.nexusvoice.domain.ai.model.AiModelInfo;
import com.nexusvoice.domain.ai.enums.AiProviderEnum;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;
import java.util.List;
import java.util.function.Consumer;

/**
 * AI服务仓储接口
 * 定义与AI服务交互的契约
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
public interface AiServiceRepository {
    
    /**
     * 执行同步聊天
     */
    ChatResponse chat(ChatRequest request, AiModelInfo modelInfo);
    
    /**
     * 执行流式聊天
     */
    void streamChat(ChatRequest request, 
                   AiModelInfo modelInfo,
                   Consumer<StreamChatResponse> onNext, 
                   Consumer<Throwable> onError, 
                   Runnable onComplete);
    
    /**
     * 获取支持的模型列表
     */
    List<AiModelInfo> getSupportedModels(AiProviderEnum provider);
    
    /**
     * 检查模型是否可用
     */
    boolean isModelAvailable(AiModelInfo modelInfo);
    
    /**
     * 估算令牌数量
     */
    int estimateTokenCount(String text, AiModelInfo modelInfo);
}
