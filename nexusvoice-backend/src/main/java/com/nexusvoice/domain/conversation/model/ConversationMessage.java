package com.nexusvoice.domain.conversation.model;

import com.nexusvoice.domain.common.BaseDomainEntity;
import com.nexusvoice.domain.conversation.constant.MessageRole;

import java.time.LocalDateTime;

/**
 * 对话消息实体
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
public class ConversationMessage extends BaseDomainEntity {

    /**
     * 对话ID
     */
    private Long conversationId;
    /**
     * 消息角色（用户、助手、系统）
     */
    private MessageRole role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * AI回复语音地址
     */
    private String audioUrl;

    /**
     * 消息序号（在对话中的顺序）
     */
    private Integer sequence;

    /**
     * 令牌数量
     */
    private Integer tokenCount;

    /**
     * 消息状态（发送中、已发送、失败等）
     */
    private String status;

    /**
     * 错误信息（如果发送失败）
     */
    private String errorMessage;

    /**
     * 消息元数据（JSON格式）
     * 可包含模型响应的其他信息，如finish_reason、usage等
     */
    private String metadata;

    /**
     * 消息发送时间
     */
    private LocalDateTime sentAt;

    /**
     * 创建用户消息
     */
    public static ConversationMessage createUserMessage(Long conversationId, String content, Integer sequence) {
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setRole(MessageRole.USER);
        message.setContent(content);
        message.setSequence(sequence);
        message.setStatus("sent");
        message.setSentAt(LocalDateTime.now());
        return message;
    }

    /**
     * 创建AI回复消息
     */
    public static ConversationMessage createAssistantMessage(Long conversationId, String content, Integer sequence) {
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setRole(MessageRole.ASSISTANT);
        message.setContent(content);
        message.setSequence(sequence);
        message.setStatus("sent");
        message.setSentAt(LocalDateTime.now());
        return message;
    }

    /**
     * 创建AI回复消息（包含音频URL）
     */
    public static ConversationMessage createAssistantMessage(Long conversationId, String content, Integer sequence, String audioUrl) {
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setRole(MessageRole.ASSISTANT);
        message.setContent(content);
        message.setSequence(sequence);
        message.setAudioUrl(audioUrl);
        message.setStatus("sent");
        message.setSentAt(LocalDateTime.now());
        return message;
    }

    /**
     * 创建系统消息
     */
    public static ConversationMessage createSystemMessage(Long conversationId, String content, Integer sequence) {
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setRole(MessageRole.SYSTEM);
        message.setContent(content);
        message.setSequence(sequence);
        message.setStatus("sent");
        message.setSentAt(LocalDateTime.now());
        return message;
    }

    /**
     * 标记消息为发送中
     */
    public void markAsSending() {
        this.status = "sending";
        this.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 标记消息发送成功
     */
    public void markAsSent() {
        this.status = "sent";
        this.sentAt = LocalDateTime.now();
        this.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 标记消息发送失败
     */
    public void markAsFailed(String errorMessage) {
        this.status = "failed";
        this.errorMessage = errorMessage;
        this.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 更新令牌数量
     */
    public void updateTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
        this.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 检查消息是否来自用户
     */
    public boolean isFromUser() {
        return MessageRole.USER.equals(this.role);
    }

    /**
     * 检查消息是否来自AI助手
     */
    public boolean isFromAssistant() {
        return MessageRole.ASSISTANT.equals(this.role);
    }
    
    // Getter and Setter methods
    public Long getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
    
    public MessageRole getRole() {
        return role;
    }
    
    public void setRole(MessageRole role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getAudioUrl() {
        return audioUrl;
    }
    
    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }
    
    public Integer getSequence() {
        return sequence;
    }
    
    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }
    
    public Integer getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
