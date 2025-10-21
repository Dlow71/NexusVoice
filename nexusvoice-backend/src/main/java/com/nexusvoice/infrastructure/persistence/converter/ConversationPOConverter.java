package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.conversation.constant.ConversationStatus;
import com.nexusvoice.domain.conversation.model.Conversation;
import com.nexusvoice.infrastructure.persistence.po.ConversationPO;
import org.springframework.stereotype.Component;

/**
 * Conversation领域对象与PO转换器
 *
 * @author NexusVoice
 * @since 2025-01-21
 */
@Component
public class ConversationPOConverter {

    /**
     * Domain转PO
     */
    public ConversationPO toPO(Conversation domain) {
        if (domain == null) {
            return null;
        }

        ConversationPO po = new ConversationPO();
        po.setId(domain.getId());
        po.setTitle(domain.getTitle());
        po.setUserId(domain.getUserId());
        po.setRoleId(domain.getRoleId());
        po.setModelName(domain.getModelName());
        po.setStatus(convertStatusToString(domain.getStatus()));
        po.setSystemPrompt(domain.getSystemPrompt());
        po.setConfigParams(domain.getConfigParams());
        po.setLastActiveAt(domain.getLastActiveAt());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setDeleted(domain.getDeleted());
        return po;
    }

    /**
     * PO转Domain
     */
    public Conversation toDomain(ConversationPO po) {
        if (po == null) {
            return null;
        }

        Conversation domain = new Conversation();
        domain.setId(po.getId());
        domain.setTitle(po.getTitle());
        domain.setUserId(po.getUserId());
        domain.setRoleId(po.getRoleId());
        domain.setModelName(po.getModelName());
        domain.setStatus(convertStringToStatus(po.getStatus()));
        domain.setSystemPrompt(po.getSystemPrompt());
        domain.setConfigParams(po.getConfigParams());
        domain.setLastActiveAt(po.getLastActiveAt());
        domain.setCreatedAt(po.getCreatedAt());
        domain.setUpdatedAt(po.getUpdatedAt());
        domain.setDeleted(po.getDeleted());
        return domain;
    }

    /**
     * 状态枚举转String
     */
    private String convertStatusToString(ConversationStatus status) {
        if (status == null) {
            return "ACTIVE"; // 默认活跃
        }
        return status.name();
    }

    /**
     * String转状态枚举
     */
    private ConversationStatus convertStringToStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return ConversationStatus.ACTIVE;
        }
        try {
            return ConversationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ConversationStatus.ACTIVE;
        }
    }
}
