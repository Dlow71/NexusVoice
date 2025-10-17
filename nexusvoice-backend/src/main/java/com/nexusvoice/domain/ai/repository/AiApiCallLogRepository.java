package com.nexusvoice.domain.ai.repository;

import com.nexusvoice.domain.ai.model.AiApiCallLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI API调用日志仓储接口
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
public interface AiApiCallLogRepository {
    
    /**
     * 保存调用日志
     */
    AiApiCallLog save(AiApiCallLog log);
    
    /**
     * 批量保存
     */
    void saveAll(List<AiApiCallLog> logs);
    
    /**
     * 根据ID查询
     */
    Optional<AiApiCallLog> findById(Long id);
    
    /**
     * 查询用户的调用记录
     */
    List<AiApiCallLog> findByUserId(Long userId, int limit);
    
    /**
     * 查询对话的调用记录
     */
    List<AiApiCallLog> findByConversationId(Long conversationId);
    
    /**
     * 查询指定时间段的调用记录
     */
    List<AiApiCallLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计指定时间段的调用次数
     */
    Long countByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计指定时间段的总费用
     */
    java.math.BigDecimal sumCostByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计指定时间段的总token使用量
     */
    Long sumTokensByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 按模型统计使用情况
     */
    Map<String, Long> countByModel(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 按用户统计使用情况
     */
    Map<Long, Long> countByUser(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取指定API密钥的调用记录
     */
    List<AiApiCallLog> findByApiKeyId(Long apiKeyId, int limit);
    
    /**
     * 获取失败的调用记录
     */
    List<AiApiCallLog> findFailedCalls(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 计算平均响应时间
     */
    Double avgResponseTime(String providerCode, String modelCode, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 计算成功率
     */
    Double calculateSuccessRate(String providerCode, String modelCode, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 清理过期日志
     */
    void deleteOldLogs(int daysToKeep);
}
