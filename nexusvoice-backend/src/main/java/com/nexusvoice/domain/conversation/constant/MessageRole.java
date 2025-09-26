package com.nexusvoice.domain.conversation.constant;

/**
 * 消息角色枚举
 * 对应OpenAI API中的role字段
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
public enum MessageRole {
    /**
     * 系统消息 - 用于设置AI的行为和上下文
     */
    SYSTEM,

    /**
     * 用户消息 - 来自用户的输入
     */
    USER,

    /**
     * 助手消息 - 来自AI的回复
     */
    ASSISTANT,

    /**
     * 功能消息 - 函数调用的结果
     */
    FUNCTION,

    /**
     * 工具消息 - 工具调用的结果
     */
    TOOL
}
