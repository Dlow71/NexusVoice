package com.nexusvoice.application.conversation.assembler;

import com.nexusvoice.application.conversation.dto.ConversationListDto;
import com.nexusvoice.application.conversation.dto.ConversationMessageWithRoleDto;
import com.nexusvoice.application.conversation.dto.RoleInfoDto;
import com.nexusvoice.domain.conversation.model.Conversation;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.role.model.Role;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话相关的数据转换器
 *
 * @author NexusVoice
 * @since 2025-09-27
 */
public class ConversationAssembler {

    /**
     * Role转换为RoleInfoDto
     *
     * @param role 角色实体
     * @return 角色信息DTO
     */
    public static RoleInfoDto toRoleInfoDto(Role role) {
        if (role == null) {
            return null;
        }

        return RoleInfoDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .avatarUrl(role.getAvatarUrl())
                .voiceType(role.getVoiceType())
                .isPublic(role.getIsPublic())
                .greetingMessage(role.getGreetingMessage())
                .greetingAudioUrl(role.getGreetingAudioUrl())
                .build();
    }

    /**
     * Conversation转换为ConversationListDto（包含角色信息）
     *
     * @param conversation 对话实体
     * @param role 绑定的角色实体
     * @param lastMessage 最后一条消息预览
     * @param messageCount 消息数量
     * @return 对话列表DTO
     */
    public static ConversationListDto toConversationListDto(Conversation conversation, Role role, 
                                                          String lastMessage, Integer messageCount) {
        if (conversation == null) {
            return null;
        }

        return ConversationListDto.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .modelName(conversation.getModelName())
                .status(conversation.getStatus().name())
                .lastMessage(lastMessage)
                .messageCount(messageCount)
                .lastActiveAt(conversation.getLastActiveAt())
                .createdAt(conversation.getCreatedAt())
                .roleId(conversation.getRoleId())
                .role(toRoleInfoDto(role))
                .build();
    }

    /**
     * ConversationMessage转换为ConversationMessageWithRoleDto
     *
     * @param message 消息实体
     * @param conversationRole 对话绑定的角色实体
     * @return 包含角色信息的消息DTO
     */
    public static ConversationMessageWithRoleDto toConversationMessageWithRoleDto(ConversationMessage message, Role conversationRole) {
        if (message == null) {
            return null;
        }

        return ConversationMessageWithRoleDto.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .role(message.getRole())
                .content(message.getContent())
                .audioUrl(message.getAudioUrl())
                .sequence(message.getSequence())
                .tokenCount(message.getTokenCount())
                .status(message.getStatus())
                .errorMessage(message.getErrorMessage())
                .metadata(message.getMetadata())
                .sentAt(message.getSentAt())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .conversationRoleId(conversationRole != null ? conversationRole.getId() : null)
                .conversationRole(toRoleInfoDto(conversationRole))
                .build();
    }

    /**
     * 批量转换ConversationMessage为ConversationMessageWithRoleDto
     *
     * @param messages 消息列表
     * @param conversationRole 对话绑定的角色实体
     * @return 包含角色信息的消息DTO列表
     */
    public static List<ConversationMessageWithRoleDto> toConversationMessageWithRoleDtoList(List<ConversationMessage> messages, Role conversationRole) {
        if (messages == null) {
            return null;
        }

        return messages.stream()
                .map(message -> toConversationMessageWithRoleDto(message, conversationRole))
                .collect(Collectors.toList());
    }
}
