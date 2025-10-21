package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.conversation.constant.MessageRole;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.infrastructure.persistence.po.ConversationMessagePO;
import org.springframework.stereotype.Component;

/**
 * ConversationMessage领域对象与PO转换器
 *
 * @author NexusVoice
 * @since 2025-01-21
 */
@Component
public class ConversationMessagePOConverter {

    /**
     * Domain转PO
     */
    public ConversationMessagePO toPO(ConversationMessage domain) {
        if (domain == null) {
            return null;
        }

        ConversationMessagePO po = new ConversationMessagePO();
        po.setId(domain.getId());
        po.setConversationId(domain.getConversationId());
        po.setRole(convertRoleToString(domain.getRole()));
        po.setContent(domain.getContent());
        po.setAudioUrl(domain.getAudioUrl());
        po.setSequence(domain.getSequence());
        po.setTokenCount(domain.getTokenCount());
        po.setStatus(domain.getStatus());
        po.setErrorMessage(domain.getErrorMessage());
        po.setMetadata(domain.getMetadata());
        po.setSentAt(domain.getSentAt());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setDeleted(domain.getDeleted());
        return po;
    }

    /**
     * PO转Domain
     */
    public ConversationMessage toDomain(ConversationMessagePO po) {
        if (po == null) {
            return null;
        }

        ConversationMessage domain = new ConversationMessage();
        domain.setId(po.getId());
        domain.setConversationId(po.getConversationId());
        domain.setRole(convertStringToRole(po.getRole()));
        domain.setContent(po.getContent());
        domain.setAudioUrl(po.getAudioUrl());
        domain.setSequence(po.getSequence());
        domain.setTokenCount(po.getTokenCount());
        domain.setStatus(po.getStatus());
        domain.setErrorMessage(po.getErrorMessage());
        domain.setMetadata(po.getMetadata());
        domain.setSentAt(po.getSentAt());
        domain.setCreatedAt(po.getCreatedAt());
        domain.setUpdatedAt(po.getUpdatedAt());
        domain.setDeleted(po.getDeleted());
        return domain;
    }

    /**
     * 角色枚举转String（小写）
     */
    private String convertRoleToString(MessageRole role) {
        if (role == null) {
            return "user"; // 默认用户
        }
        return role.name().toLowerCase();
    }

    /**
     * String转角色枚举
     */
    private MessageRole convertStringToRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return MessageRole.USER;
        }
        try {
            return MessageRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MessageRole.USER;
        }
    }
}
