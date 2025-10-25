package com.nexusvoice.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.conversation.constant.MessageRole;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.model.MessageAttachment;
import com.nexusvoice.infrastructure.persistence.po.ConversationMessagePO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ConversationMessage领域对象与PO转换器
 *
 * @author NexusVoice
 * @since 2025-01-21
 */
@Slf4j
@Component
public class ConversationMessagePOConverter {

    private final ObjectMapper objectMapper;

    public ConversationMessagePOConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
        
        // 序列化附件列表为JSON
        if (domain.hasAttachments()) {
            try {
                String attachmentsJson = objectMapper.writeValueAsString(domain.getAttachments());
                po.setAttachmentUrls(attachmentsJson);
                po.setAttachmentCount(domain.getAttachmentCount());
                log.info("=== 序列化附件成功，attachmentCount={}, JSON长度={}", 
                        domain.getAttachmentCount(), attachmentsJson.length());
                log.info("附件JSON: {}", attachmentsJson);
            } catch (JsonProcessingException e) {
                log.error("序列化附件列表失败", e);
                po.setAttachmentUrls(null);
                po.setAttachmentCount(0);
            }
        } else {
            po.setAttachmentUrls(null);
            po.setAttachmentCount(0);
            log.info("=== 消息没有附件");
        }
        
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
        
        // 反序列化附件列表
        if (po.getAttachmentUrls() != null && !po.getAttachmentUrls().trim().isEmpty()) {
            try {
                log.info("=== 开始反序列化附件，JSON长度={}", po.getAttachmentUrls().length());
                log.info("附件JSON: {}", po.getAttachmentUrls());
                List<MessageAttachment> attachments = objectMapper.readValue(
                    po.getAttachmentUrls(),
                    new TypeReference<List<MessageAttachment>>() {}
                );
                domain.setAttachments(attachments);
                log.info("=== 反序列化附件成功，数量={}", attachments.size());
            } catch (JsonProcessingException e) {
                log.error("反序列化附件列表失败，附件JSON: {}", po.getAttachmentUrls(), e);
                domain.setAttachments(new ArrayList<>());
            }
        } else {
            domain.setAttachments(new ArrayList<>());
            log.info("=== 消息没有附件数据");
        }
        
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
