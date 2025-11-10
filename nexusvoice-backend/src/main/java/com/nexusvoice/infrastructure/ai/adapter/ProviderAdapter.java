package com.nexusvoice.infrastructure.ai.adapter;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.model.AiProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

/**
 * AI服务提供商适配器接口
 * 定义不同协议的模型创建方式
 * 
 * 设计思想：
 * - 每个协议（openai_compatible、anthropic、dashscope等）对应一个适配器实现
 * - 适配器封装协议特定的模型创建逻辑
 * - 通过Protocol字段动态选择对应的适配器
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
public interface ProviderAdapter {
    
    /**
     * 创建聊天模型
     *
     * @param provider AI服务提供商配置
     * @param model AI模型配置
     * @param apiKey API密钥配置
     * @return 聊天模型实例
     */
    ChatLanguageModel createChatModel(AiProvider provider, AiModel model, AiApiKey apiKey);
    
    /**
     * 创建流式聊天模型
     *
     * @param provider AI服务提供商配置
     * @param model AI模型配置
     * @param apiKey API密钥配置
     * @return 流式聊天模型实例
     */
    StreamingChatLanguageModel createStreamingChatModel(AiProvider provider, AiModel model, AiApiKey apiKey);
    
    /**
     * 创建Embedding模型
     *
     * @param provider AI服务提供商配置
     * @param model AI模型配置
     * @param apiKey API密钥配置
     * @return Embedding模型实例
     */
    EmbeddingModel createEmbeddingModel(AiProvider provider, AiModel model, AiApiKey apiKey);
    
    /**
     * 获取适配器支持的协议类型
     *
     * @return 协议类型代码（如：openai_compatible、anthropic等）
     */
    String getSupportedProtocol();
}
