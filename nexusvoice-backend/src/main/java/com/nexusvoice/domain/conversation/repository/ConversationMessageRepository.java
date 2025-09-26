package com.nexusvoice.domain.conversation.repository;

import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.constant.MessageRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 对话消息仓储接口
 * 定义对话消息数据的持久化操作
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
public interface ConversationMessageRepository {

    /**
     * 保存消息
     */
    ConversationMessage save(ConversationMessage message);

    /**
     * 批量保存消息
     */
    List<ConversationMessage> saveAll(List<ConversationMessage> messages);

    /**
     * 根据ID查找消息
     */
    Optional<ConversationMessage> findById(Long messageId);

    /**
     * 根据对话ID查找所有消息
     */
    List<ConversationMessage> findByConversationId(Long conversationId);

    /**
     * 根据对话ID查找消息，按序号排序
     */
    List<ConversationMessage> findByConversationIdOrderBySequence(Long conversationId);

    /**
     * 根据对话ID和角色查找消息
     */
    List<ConversationMessage> findByConversationIdAndRole(Long conversationId, MessageRole role);

    /**
     * 分页查找对话的消息
     */
    List<ConversationMessage> findByConversationIdWithPaging(Long conversationId, Integer page, Integer size);

    /**
     * 查找对话中最近的消息
     */
    List<ConversationMessage> findRecentByConversationId(Long conversationId, Integer limit);

    /**
     * 查找对话的最后一条消息
     */
    Optional<ConversationMessage> findLastMessageByConversationId(Long conversationId);

    /**
     * 获取对话中下一个消息序号
     */
    Integer getNextSequenceByConversationId(Long conversationId);

    /**
     * 统计对话的消息总数
     */
    Long countByConversationId(Long conversationId);

    /**
     * 统计对话指定角色的消息数量
     */
    Long countByConversationIdAndRole(Long conversationId, MessageRole role);

    /**
     * 统计对话的总令牌数
     */
    Long sumTokenCountByConversationId(Long conversationId);

    /**
     * 删除消息（物理删除）
     */
    void deleteById(Long messageId);

    /**
     * 删除对话的所有消息
     */
    void deleteByConversationId(Long conversationId);

    /**
     * 批量删除消息
     */
    void deleteByIds(List<Long> messageIds);

    /**
     * 更新消息内容
     */
    void updateContent(Long messageId, String content);

    /**
     * 更新消息状态
     */
    void updateStatus(Long messageId, String status);

    /**
     * 更新消息令牌数量
     */
    void updateTokenCount(Long messageId, Integer tokenCount);

    /**
     * 批量更新消息状态
     */
    void updateStatusByIds(List<Long> messageIds, String status);

    /**
     * 删除指定时间之前的消息
     */
    void deleteMessagesBefore(LocalDateTime dateTime);

    /**
     * 检查消息是否存在
     */
    boolean existsById(Long messageId);

    /**
     * 检查对话是否包含指定序号的消息
     */
    boolean existsByConversationIdAndSequence(Long conversationId, Integer sequence);
}
