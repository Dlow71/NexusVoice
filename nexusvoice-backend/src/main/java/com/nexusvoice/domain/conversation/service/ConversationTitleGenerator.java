package com.nexusvoice.domain.conversation.service;

import com.nexusvoice.domain.conversation.model.ConversationMessage;

import java.util.List;

/**
 * 对话标题生成器接口
 * 定义生成对话标题的领域服务契约
 *
 * @author NexusVoice
 * @since 2025-01-26
 */
public interface ConversationTitleGenerator {

    /**
     * 根据对话消息生成标题
     *
     * @param messages 对话消息列表（通常是前2-4条消息）
     * @return 生成的标题（5-15个字）
     */
    String generateTitle(List<ConversationMessage> messages);

    /**
     * 验证是否可以生成标题
     * 至少需要一轮对话（用户消息+AI回复）
     *
     * @param messages 对话消息列表
     * @return 是否可以生成标题
     */
    default boolean canGenerateTitle(List<ConversationMessage> messages) {
        if (messages == null || messages.size() < 2) {
            return false;
        }
        
        // 至少需要一条用户消息和一条AI回复
        boolean hasUserMessage = false;
        boolean hasAiMessage = false;
        
        for (ConversationMessage msg : messages) {
            if (msg.isFromUser()) {
                hasUserMessage = true;
            } else if (msg.isFromAssistant()) {
                hasAiMessage = true;
            }
        }
        
        return hasUserMessage && hasAiMessage;
    }
}
