package com.nexusvoice.infrastructure.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.nexusvoice.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 对话消息数据库实体
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("conversation_messages")
public class ConversationMessageEntity extends BaseEntity {

    /**
     * 对话ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 消息角色
     */
    @TableField("role")
    private String role;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 消息序号
     */
    @TableField("sequence")
    private Integer sequence;

    /**
     * 令牌数量
     */
    @TableField("token_count")
    private Integer tokenCount;

    /**
     * 消息状态
     */
    @TableField("status")
    private String status;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 消息元数据（JSON格式）
     */
    @TableField("metadata")
    private String metadata;

    /**
     * 消息发送时间
     */
    @TableField("sent_at")
    private LocalDateTime sentAt;
}
