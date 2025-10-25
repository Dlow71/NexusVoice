package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 对话消息持久化对象
 * 对应数据库表conversation_messages
 *
 * @author NexusVoice
 * @since 2025-01-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("conversation_messages")
public class ConversationMessagePO extends BasePO {

    /**
     * 对话ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 消息角色（user/assistant/system）
     */
    @TableField("role")
    private String role;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * AI回复语音地址
     */
    @TableField("audio_url")
    private String audioUrl;

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

    /**
     * 附件URL列表（JSON格式）
     */
    @TableField("attachment_urls")
    private String attachmentUrls;

    /**
     * 附件数量
     */
    @TableField("attachment_count")
    private Integer attachmentCount;
}
