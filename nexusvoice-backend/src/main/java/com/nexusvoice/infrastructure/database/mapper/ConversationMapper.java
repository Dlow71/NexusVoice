package com.nexusvoice.infrastructure.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.database.entity.ConversationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话数据库映射器
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Mapper
public interface ConversationMapper extends BaseMapper<ConversationEntity> {

    /**
     * 根据用户ID查找最近的对话
     */
    @Select("SELECT * FROM conversations WHERE user_id = #{userId} AND deleted = 0 " +
            "ORDER BY last_active_at DESC LIMIT #{limit}")
    List<ConversationEntity> findRecentByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 根据用户ID和关键词搜索对话
     */
    @Select("SELECT * FROM conversations WHERE user_id = #{userId} AND deleted = 0 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR system_prompt LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY last_active_at DESC")
    List<ConversationEntity> searchByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * 统计用户指定状态的对话数量
     */
    @Select("SELECT COUNT(*) FROM conversations WHERE user_id = #{userId} AND status = #{status} AND deleted = 0")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 批量更新对话状态
     */
    @Update("<script>" +
            "UPDATE conversations SET status = #{status}, updated_at = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void updateStatusByIds(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 删除指定时间之前的已归档对话
     */
    @Update("UPDATE conversations SET deleted = 1, updated_at = NOW() " +
            "WHERE status = 'ARCHIVED' AND updated_at < #{dateTime}")
    void deleteArchivedConversationsBefore(@Param("dateTime") LocalDateTime dateTime);

    /**
     * 检查对话是否属于指定用户
     */
    @Select("SELECT COUNT(*) > 0 FROM conversations WHERE id = #{conversationId} AND user_id = #{userId} AND deleted = 0")
    boolean existsByIdAndUserId(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
