package com.nexusvoice.domain.conversation.repository;

import com.nexusvoice.domain.conversation.constant.ConversationStatus;
import com.nexusvoice.domain.conversation.model.Conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 对话仓储接口
 * 定义对话数据的持久化操作
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
public interface ConversationRepository {

    /**
     * 保存对话
     */
    Conversation save(Conversation conversation);

    /**
     * 根据ID查找对话
     */
    Optional<Conversation> findById(Long conversationId);

    /**
     * 根据ID和用户ID查找对话
     */
    Optional<Conversation> findByIdAndUserId(Long conversationId, Long userId);

    /**
     * 根据用户ID查找所有对话
     */
    List<Conversation> findByUserId(Long userId);

    /**
     * 根据用户ID和状态查找对话
     */
    List<Conversation> findByUserIdAndStatus(Long userId, ConversationStatus status);

    /**
     * 分页查找用户的对话
     */
    List<Conversation> findByUserIdWithPaging(Long userId, Integer page, Integer size);

    /**
     * 查找用户最近的对话
     */
    List<Conversation> findRecentByUserId(Long userId, Integer limit);

    /**
     * 根据用户ID和关键词搜索对话
     */
    List<Conversation> searchByUserIdAndKeyword(Long userId, String keyword);

    /**
     * 统计用户的对话总数
     */
    Long countByUserId(Long userId);

    /**
     * 统计用户指定状态的对话数量
     */
    Long countByUserIdAndStatus(Long userId, ConversationStatus status);

    /**
     * 删除对话（物理删除）
     */
    void deleteById(Long conversationId);

    /**
     * 逻辑删除对话
     */
    void logicalDeleteById(Long conversationId);

    /**
     * 批量更新对话状态
     */
    void updateStatusByIds(List<Long> conversationIds, ConversationStatus status);

    /**
     * 删除指定时间之前的已归档对话
     */
    void deleteArchivedConversationsBefore(LocalDateTime dateTime);

    /**
     * 检查对话是否存在
     */
    boolean existsById(Long conversationId);

    /**
     * 检查对话是否属于指定用户
     */
    boolean existsByIdAndUserId(Long conversationId, Long userId);
}
