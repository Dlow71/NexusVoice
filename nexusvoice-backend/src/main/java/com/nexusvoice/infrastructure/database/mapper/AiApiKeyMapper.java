package com.nexusvoice.infrastructure.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.domain.ai.model.AiApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI API密钥Mapper接口
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Mapper
public interface AiApiKeyMapper extends BaseMapper<AiApiKey> {
    
    /**
     * 更新使用统计
     */
    @Update("UPDATE ai_api_keys SET " +
            "total_requests = total_requests + 1, " +
            "total_tokens_used = total_tokens_used + #{tokens}, " +
            "total_cost = total_cost + #{cost}, " +
            "monthly_requests = monthly_requests + 1, " +
            "monthly_tokens_used = monthly_tokens_used + #{tokens}, " +
            "monthly_cost = monthly_cost + #{cost}, " +
            "daily_tokens_used = daily_tokens_used + #{tokens}, " +
            "last_used_at = #{time}, " +
            "last_success_time = #{time}, " +
            "fail_count = 0 " +
            "WHERE id = #{id}")
    void updateUsageStats(@Param("id") Long id, 
                         @Param("tokens") Integer tokens, 
                         @Param("cost") BigDecimal cost,
                         @Param("time") LocalDateTime time);
    
    /**
     * 标记失败
     */
    @Update("UPDATE ai_api_keys SET " +
            "fail_count = fail_count + 1, " +
            "last_fail_time = #{time}, " +
            "status = CASE WHEN fail_count >= 4 THEN 0 ELSE status END " +
            "WHERE id = #{id}")
    void markFailed(@Param("id") Long id, @Param("time") LocalDateTime time);
    
    /**
     * 重置所有日限额
     */
    @Update("UPDATE ai_api_keys SET daily_tokens_used = 0 WHERE deleted = 0")
    void resetAllDailyQuota();
    
    /**
     * 重置月限额
     */
    @Update("UPDATE ai_api_keys SET " +
            "monthly_requests = 0, " +
            "monthly_tokens_used = 0, " +
            "monthly_cost = 0, " +
            "monthly_reset_date = CURRENT_DATE " +
            "WHERE deleted = 0 AND (monthly_reset_date IS NULL OR monthly_reset_date < CURRENT_DATE)")
    void resetMonthlyQuota();
}
