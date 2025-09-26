package com.nexusvoice.infrastructure.ai.model;

import com.nexusvoice.domain.conversation.constant.MessageRole;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * AI聊天消息模型
 * 用于与LangChain4j交互的消息格式
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 消息角色
     */
    private MessageRole role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 创建系统消息
     */
    public static ChatMessage system(String content) {
        return new ChatMessage(MessageRole.SYSTEM, content);
    }

    /**
     * 创建用户消息
     */
    public static ChatMessage user(String content) {
        return new ChatMessage(MessageRole.USER, content);
    }

    /**
     * 创建助手消息
     */
    public static ChatMessage assistant(String content) {
        return new ChatMessage(MessageRole.ASSISTANT, content);
    }
}
