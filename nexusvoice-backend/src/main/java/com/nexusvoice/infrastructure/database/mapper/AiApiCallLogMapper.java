package com.nexusvoice.infrastructure.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.domain.ai.model.AiApiCallLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI API调用日志Mapper接口
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Mapper
public interface AiApiCallLogMapper extends BaseMapper<AiApiCallLog> {
    
    /**
     * 统计指定时间段的总费用
     */
    @Select("SELECT COALESCE(SUM(total_cost), 0) FROM ai_api_call_logs " +
            "WHERE request_time >= #{startTime} AND request_time <= #{endTime}")
    BigDecimal sumCostByTimeRange(@Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间段的总token使用量
     */
    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM ai_api_call_logs " +
            "WHERE request_time >= #{startTime} AND request_time <= #{endTime}")
    Long sumTokensByTimeRange(@Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按模型统计使用次数
     */
    @Select("SELECT provider_code || ':' || model_code as model_key, COUNT(*) as count " +
            "FROM ai_api_call_logs " +
            "WHERE request_time >= #{startTime} AND request_time <= #{endTime} " +
            "GROUP BY provider_code, model_code")
    List<Map<String, Object>> countByModel(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按用户统计使用次数
     */
    @Select("SELECT user_id, COUNT(*) as count " +
            "FROM ai_api_call_logs " +
            "WHERE request_time >= #{startTime} AND request_time <= #{endTime} AND user_id IS NOT NULL " +
            "GROUP BY user_id")
    List<Map<String, Object>> countByUser(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 计算平均响应时间
     */
    @Select("SELECT AVG(response_time_ms) FROM ai_api_call_logs " +
            "WHERE provider_code = #{providerCode} AND model_code = #{modelCode} " +
            "AND request_time >= #{startTime} AND request_time <= #{endTime} " +
            "AND status = 1")
    Double avgResponseTime(@Param("providerCode") String providerCode,
                          @Param("modelCode") String modelCode,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 计算成功率
     */
    @Select("SELECT " +
            "CAST(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS DOUBLE PRECISION) / COUNT(*) * 100 " +
            "FROM ai_api_call_logs " +
            "WHERE provider_code = #{providerCode} AND model_code = #{modelCode} " +
            "AND request_time >= #{startTime} AND request_time <= #{endTime}")
    Double calculateSuccessRate(@Param("providerCode") String providerCode,
                               @Param("modelCode") String modelCode,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 删除过期日志
     */
    @Delete("DELETE FROM ai_api_call_logs " +
            "WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '1 day' * #{daysToKeep}")
    void deleteOldLogs(@Param("daysToKeep") int daysToKeep);
}
