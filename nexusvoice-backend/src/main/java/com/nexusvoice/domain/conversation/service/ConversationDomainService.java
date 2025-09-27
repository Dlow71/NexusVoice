package com.nexusvoice.domain.conversation.service;

import com.nexusvoice.domain.conversation.model.Conversation;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.repository.ConversationRepository;
import com.nexusvoice.domain.conversation.repository.ConversationMessageRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 对话领域服务
 * 实现对话相关的业务规则和领域逻辑
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Service
public class ConversationDomainService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;

    public ConversationDomainService(ConversationRepository conversationRepository,
                                   ConversationMessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * 创建新对话
     */
    public Conversation createConversation(Long userId, String title, String modelName, String systemPrompt) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        conversation.setModelName(modelName);
        conversation.setSystemPrompt(systemPrompt);
        conversation.activate();

        return conversationRepository.save(conversation);
    }

    /**
     * 向对话添加消息
     */
    public ConversationMessage addMessageToConversation(Long conversationId, ConversationMessage message) {
        // 验证对话是否存在
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "对话不存在"));

        // 设置消息序号
        Integer nextSequence = messageRepository.getNextSequenceByConversationId(conversationId);
        message.setSequence(nextSequence);
        message.setConversationId(conversationId);

        // 保存消息
        ConversationMessage savedMessage = messageRepository.save(message);

        // 更新对话的最后活跃时间
        conversation.activate();
        conversationRepository.save(conversation);

        return savedMessage;
    }

    /**
     * 获取对话的完整消息历史
     */
    public List<ConversationMessage> getConversationHistory(Long conversationId) {
        // 验证对话是否存在
        if (!conversationRepository.existsById(conversationId)) {
            throw new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "对话不存在");
        }

        return messageRepository.findByConversationIdOrderBySequence(conversationId);
    }

    /**
     * 验证用户是否拥有对话权限
     */
    public void validateConversationAccess(Long conversationId, Long userId) {
        if (!conversationRepository.existsByIdAndUserId(conversationId, userId)) {
            throw new BizException(ErrorCodeEnum.PERMISSION_DENIED, "您没有访问此对话的权限");
        }
    }

    /**
     * 检查对话消息数量是否超过限制
     */
    public void checkMessageCountLimit(Long conversationId, int maxMessages) {
        Long messageCount = messageRepository.countByConversationId(conversationId);
        if (messageCount >= maxMessages) {
            throw new BizException(ErrorCodeEnum.DATA_VALIDATION_ERROR, 
                String.format("对话消息数量已达上限（%d条），请创建新对话", maxMessages));
        }
    }

    /**
     * 计算对话的总令牌数
     */
    public Long calculateTotalTokens(Long conversationId) {
        Long totalTokens = messageRepository.sumTokenCountByConversationId(conversationId);
        return totalTokens != null ? totalTokens : 0L;
    }

    /**
     * 检查令牌数量是否超过限制
     */
    public void checkTokenLimit(Long conversationId, int maxTokens) {
        Long totalTokens = calculateTotalTokens(conversationId);
        if (totalTokens > maxTokens) {
            throw new BizException(ErrorCodeEnum.DATA_VALIDATION_ERROR,
                String.format("对话令牌数量已超过限制（%d），请创建新对话或清理历史消息", maxTokens));
        }
    }

    /**
     * 自动生成对话标题
     * 基于第一条用户消息的内容
     */
    public String generateConversationTitle(Long conversationId) {
        List<ConversationMessage> messages = messageRepository.findByConversationIdOrderBySequence(conversationId);
        
        // 找到第一条用户消息
        for (ConversationMessage message : messages) {
            if (message.isFromUser() && message.getContent() != null && !message.getContent().trim().isEmpty()) {
                String content = message.getContent().trim();
                // 取前20个字符作为标题
                if (content.length() > 20) {
                    return content.substring(0, 20) + "...";
                }
                return content;
            }
        }
        
        return "新对话";
    }
}
