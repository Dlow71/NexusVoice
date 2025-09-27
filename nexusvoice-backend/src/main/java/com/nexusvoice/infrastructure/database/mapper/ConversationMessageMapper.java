package com.nexusvoice.infrastructure.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.database.entity.ConversationMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话消息数据库映射器
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessageEntity> {

    /**
     * 根据对话ID查找消息，按序号排序
     */
    @Select("SELECT * FROM conversation_messages WHERE conversation_id = #{conversationId} AND deleted = 0 " +
            "ORDER BY sequence ASC")
    List<ConversationMessageEntity> findByConversationIdOrderBySequence(@Param("conversationId") Long conversationId);

    /**
     * 根据对话ID和角色查找消息
     */
    @Select("SELECT * FROM conversation_messages WHERE conversation_id = #{conversationId} AND role = #{role} AND deleted = 0 " +
            "ORDER BY sequence ASC")
    List<ConversationMessageEntity> findByConversationIdAndRole(@Param("conversationId") Long conversationId, 
                                                               @Param("role") String role);

    /**
     * 查找对话中最近的消息
     */
    @Select("SELECT * FROM conversation_messages WHERE conversation_id = #{conversationId} AND deleted = 0 " +
            "ORDER BY sequence DESC LIMIT #{limit}")
    List<ConversationMessageEntity> findRecentByConversationId(@Param("conversationId") Long conversationId, 
                                                             @Param("limit") Integer limit);

    /**
     * 查找对话的最后一条消息
     */
    @Select("SELECT * FROM conversation_messages WHERE conversation_id = #{conversationId} AND deleted = 0 " +
            "ORDER BY sequence DESC LIMIT 1")
    ConversationMessageEntity findLastMessageByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 获取对话中下一个消息序号
     */
    @Select("SELECT COALESCE(MAX(sequence), 0) + 1 FROM conversation_messages WHERE conversation_id = #{conversationId}")
    Integer getNextSequenceByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 统计对话指定角色的消息数量
     */
    @Select("SELECT COUNT(*) FROM conversation_messages WHERE conversation_id = #{conversationId} AND role = #{role} AND deleted = 0")
    Long countByConversationIdAndRole(@Param("conversationId") Long conversationId, @Param("role") String role);

    /**
     * 统计对话的总令牌数
     */
    @Select("SELECT COALESCE(SUM(token_count), 0) FROM conversation_messages WHERE conversation_id = #{conversationId} AND deleted = 0")
    Long sumTokenCountByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 删除对话的所有消息
     */
    @Update("UPDATE conversation_messages SET deleted = 1, updated_at = NOW() WHERE conversation_id = #{conversationId}")
    void deleteByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 批量删除消息
     */
    @Update("<script>" +
            "UPDATE conversation_messages SET deleted = 1, updated_at = NOW() WHERE id IN " +
            "<foreach collection='messageIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void deleteByIds(@Param("messageIds") List<Long> messageIds);

    /**
     * 更新消息内容
     */
    @Update("UPDATE conversation_messages SET content = #{content}, updated_at = NOW() WHERE id = #{messageId}")
    void updateContent(@Param("messageId") Long messageId, @Param("content") String content);

    /**
     * 更新消息状态
     */
    @Update("UPDATE conversation_messages SET status = #{status}, updated_at = NOW() WHERE id = #{messageId}")
    void updateStatus(@Param("messageId") Long messageId, @Param("status") String status);

    /**
     * 更新消息令牌数量
     */
    @Update("UPDATE conversation_messages SET token_count = #{tokenCount}, updated_at = NOW() WHERE id = #{messageId}")
    void updateTokenCount(@Param("messageId") Long messageId, @Param("tokenCount") Integer tokenCount);

    /**
     * 批量更新消息状态
     */
    @Update("<script>" +
            "UPDATE conversation_messages SET status = #{status}, updated_at = NOW() WHERE id IN " +
            "<foreach collection='messageIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void updateStatusByIds(@Param("messageIds") List<Long> messageIds, @Param("status") String status);

    /**
     * 删除指定时间之前的消息
     */
    @Update("UPDATE conversation_messages SET deleted = 1, updated_at = NOW() WHERE created_at < #{dateTime}")
    void deleteMessagesBefore(@Param("dateTime") LocalDateTime dateTime);

    /**
     * 检查对话是否包含指定序号的消息
     */
    @Select("SELECT COUNT(*) > 0 FROM conversation_messages WHERE conversation_id = #{conversationId} AND sequence = #{sequence} AND deleted = 0")
    boolean existsByConversationIdAndSequence(@Param("conversationId") Long conversationId, @Param("sequence") Integer sequence);
}
