package com.nexusvoice.infrastructure.ai.strategy.impl;

import com.nexusvoice.domain.ai.model.AiModelInfo;
import com.nexusvoice.domain.ai.model.EnhancementContext;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.strategy.ChatStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * 基础聊天策略
 * 直接调用AI模型，不进行任何增强
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Component
public class BasicChatStrategy implements ChatStrategy {
    
    @Autowired
    private ChatLanguageModel chatLanguageModel;
    
    @Override
    public ChatResponse execute(ChatRequest request, AiModelInfo modelInfo, EnhancementContext context) {
        try {
            log.info("执行基础聊天策略，模型：{}", modelInfo.getModelIdentifier());
            
            // 转换消息格式
            List<dev.langchain4j.data.message.ChatMessage> messages = convertMessages(request.getMessages());
            
            // 调用模型
            Response<AiMessage> response = chatLanguageModel.generate(messages);
            
            // 构建响应
            String content = response.content().text();
            
            // 估算令牌数（简单估算）
            ChatResponse.TokenUsage usage = ChatResponse.TokenUsage.builder()
                    .promptTokens(estimateTokenCount(buildPromptText(request.getMessages())))
                    .completionTokens(estimateTokenCount(content))
                    .totalTokens(estimateTokenCount(buildPromptText(request.getMessages()) + content))
                    .build();
            
            return ChatResponse.success(content, modelInfo.getModelIdentifier(), usage, null);
            
        } catch (Exception e) {
            log.error("基础聊天策略执行失败", e);
            return ChatResponse.error("聊天处理失败：" + e.getMessage());
        }
    }
    
    @Override
    public boolean supports(ChatRequest request, AiModelInfo modelInfo) {
        // 基础策略支持所有请求（作为默认策略）
        return true;
    }
    
    @Override
    public String getName() {
        return "基础聊天策略";
    }
    
    @Override
    public int getPriority() {
        return 999; // 最低优先级，作为默认策略
    }
    
    /**
     * 转换消息格式
     */
    private List<dev.langchain4j.data.message.ChatMessage> convertMessages(List<ChatMessage> messages) {
        List<dev.langchain4j.data.message.ChatMessage> langchainMessages = new ArrayList<>();
        
        for (ChatMessage message : messages) {
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
    
    /**
     * 构建提示文本
     */
    private String buildPromptText(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage message : messages) {
            sb.append(message.getContent()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 估算令牌数
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：中文约2字符/令牌，英文约4字符/令牌
        return (int) Math.ceil(text.length() / 3.0);
    }
}
