package com.nexusvoice.infrastructure.ai.service.impl;

import com.nexusvoice.domain.ai.enums.AiProviderEnum;
import com.nexusvoice.domain.ai.model.AiModelInfo;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;
import com.nexusvoice.infrastructure.ai.strategy.ChatStrategy;
import com.nexusvoice.infrastructure.ai.template.AbstractAiChatService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * OpenAI聊天服务实现（新架构）
 * 基于模板方法模式和策略模式
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "nexusvoice.ai.provider.openai.enabled", havingValue = "true", matchIfMissing = true)
public class OpenAiChatServiceNew extends AbstractAiChatService {
    
    @Autowired
    private ChatLanguageModel chatLanguageModel;
    
    @Autowired
    private OpenAiStreamingChatModel streamingChatModel;
    
    @Value("${nexusvoice.ai.provider.openai.default-model:gpt-oss-20b}")
    private String defaultModel;
    
    @Override
    protected ChatResponse executeChat(ChatRequest request, AiModelInfo modelInfo, ChatStrategy strategy) {
        try {
            // 确保模型可用
            if (chatLanguageModel == null) {
                log.error("OpenAI ChatLanguageModel未配置");
                return ChatResponse.error("OpenAI服务未正确配置");
            }
            
            // 使用策略执行聊天
            ChatResponse response = strategy.execute(request, modelInfo, 
                    createEnhancementContext(request));
            
            // 添加提供商信息
            if (response.getSuccess()) {
                response.setMetadata("provider", AiProviderEnum.OPENAI.getName());
                response.setMetadata("modelCode", modelInfo.getModelCode());
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("OpenAI聊天执行失败", e);
            return ChatResponse.error("OpenAI服务执行失败：" + e.getMessage());
        }
    }
    
    @Override
    protected void doStreamChat(ChatRequest request, 
                               AiModelInfo modelInfo,
                               Consumer<StreamChatResponse> onNext, 
                               Consumer<Throwable> onError, 
                               Runnable onComplete) {
        try {
            if (streamingChatModel == null) {
                onError.accept(new RuntimeException("OpenAI流式服务未配置"));
                return;
            }
            
            // 转换消息格式
            List<dev.langchain4j.data.message.ChatMessage> messages = convertMessages(request.getMessages());
            
            // 创建流式响应处理器
            AtomicInteger index = new AtomicInteger(0);
            String responseId = "stream_" + System.currentTimeMillis();
            
            StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    try {
                        StreamChatResponse response = StreamChatResponse.content(
                                token,
                                index.getAndIncrement()
                        );
                        response.setId(responseId);
                        response.setModel(modelInfo.getModelIdentifier());
                        onNext.accept(response);
                    } catch (Exception e) {
                        log.error("处理流式响应时发生错误", e);
                        onError.accept(e);
                    }
                }
                
                @Override
                public void onComplete(Response<AiMessage> response) {
                    try {
                        StreamChatResponse endResponse = StreamChatResponse.end(
                                response.finishReason() != null ? 
                                        response.finishReason().toString() : "stop"
                        );
                        endResponse.setId(responseId);
                        endResponse.setModel(modelInfo.getModelIdentifier());
                        onNext.accept(endResponse);
                        onComplete.run();
                    } catch (Exception e) {
                        log.error("完成流式响应时发生错误", e);
                        onError.accept(e);
                    }
                }
                
                @Override
                public void onError(Throwable throwable) {
                    log.error("流式聊天请求失败", throwable);
                    onError.accept(throwable);
                }
            };
            
            // 发送开始信号
            StreamChatResponse startResponse = StreamChatResponse.start(
                    responseId, modelInfo.getModelIdentifier());
            onNext.accept(startResponse);
            
            // 开始流式请求
            streamingChatModel.generate(messages, handler);
            
        } catch (Exception e) {
            log.error("启动OpenAI流式聊天失败", e);
            onError.accept(e);
        }
    }
    
    @Override
    public AiProviderEnum getProviderType() {
        return AiProviderEnum.OPENAI;
    }
    
    @Override
    public String getModelName() {
        return defaultModel;
    }
    
    @Override
    public boolean isModelAvailable() {
        try {
            if (chatLanguageModel == null) {
                return false;
            }
            
            // 简单的健康检查
            List<dev.langchain4j.data.message.ChatMessage> testMessages = List.of(
                    SystemMessage.from("你是一个AI助手"),
                    UserMessage.from("测试连接")
            );
            Response<AiMessage> response = chatLanguageModel.generate(testMessages);
            return response != null && response.content() != null;
        } catch (Exception e) {
            log.warn("OpenAI模型可用性检查失败", e);
            return false;
        }
    }
    
    @Override
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：中文约2字符/令牌，英文约4字符/令牌
        return (int) Math.ceil(text.length() / 3.0);
    }
    
    /**
     * 转换消息格式为LangChain4j格式
     */
    private List<dev.langchain4j.data.message.ChatMessage> convertMessages(
            List<com.nexusvoice.infrastructure.ai.model.ChatMessage> messages) {
        List<dev.langchain4j.data.message.ChatMessage> langchainMessages = new ArrayList<>();
        
        for (com.nexusvoice.infrastructure.ai.model.ChatMessage message : messages) {
            switch (message.getRole()) {
                case SYSTEM:
                    langchainMessages.add(SystemMessage.from(message.getContent()));
                    break;
                case USER:
                    langchainMessages.add(UserMessage.from(message.getContent()));
                    break;
                case ASSISTANT:
                    langchainMessages.add(AiMessage.from(message.getContent()));
                    break;
                default:
                    log.warn("忽略不支持的消息角色：{}", message.getRole());
                    break;
            }
        }
        
        return langchainMessages;
    }
}
