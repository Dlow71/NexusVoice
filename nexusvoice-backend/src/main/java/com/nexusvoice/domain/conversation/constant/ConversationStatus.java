package com.nexusvoice.domain.conversation.constant;

/**
 * 对话状态枚举
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
public enum ConversationStatus {
    /**
     * 活跃状态 - 可以继续对话
     */
    ACTIVE,

    /**
     * 归档状态 - 对话已结束，但保留记录
     */
    ARCHIVED,

    /**
     * 删除状态 - 逻辑删除
     */
    DELETED
}
