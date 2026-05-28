package com.nexusvoice.domain.ai.repository;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.application.user.dto.PageResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI API密钥仓储接口
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
public interface AiApiKeyRepository {
    
    /**
     * 根据ID查询
     */
    Optional<AiApiKey> findById(Long id);
    
    /**
     * 查询指定模型的所有可用密钥
     */
    List<AiApiKey> findAvailableByModel(String providerCode, String modelCode);
    
    /**
     * 查询指定模型的所有密钥（包括不可用的）
     */
    List<AiApiKey> findAllByModel(String providerCode, String modelCode);

    /**
     * 分页查询所有密钥
     */
    PageResult<AiApiKey> pageAll(Integer page, Integer size, String keyword,
                                 String providerCode, String modelCode, Integer status);
    
    /**
     * 保存密钥
     */
    AiApiKey save(AiApiKey apiKey);
    
    /**
     * 批量保存
     */
    void saveAll(List<AiApiKey> apiKeys);
    
    /**
     * 更新密钥状态
     */
    void updateStatus(Long id, Integer status);
    
    /**
     * 更新使用统计
     */
    void updateUsageStats(Long id, Integer tokens, java.math.BigDecimal cost);
    
    /**
     * 标记失败
     */
    void markFailed(Long id);
    
    /**
     * 标记成功
     */
    void markSuccess(Long id);
    
    /**
     * 更新最后使用时间
     */
    void updateLastUsedTime(Long id, LocalDateTime time);
    
    /**
     * 重置日限额
     */
    void resetDailyQuota(Long id);
    
    /**
     * 重置月限额
     */
    void resetMonthlyQuota(Long id);
    
    /**
     * 批量重置所有密钥的日限额
     */
    void resetAllDailyQuota();
    
    /**
     * 批量重置需要重置的月限额
     */
    void resetMonthlyQuotaByDate(LocalDate date);
    
    /**
     * 更新健康检查时间
     */
    void updateHealthCheckTime(Long id, LocalDateTime time);
    
    /**
     * 查询需要健康检查的密钥
     */
    List<AiApiKey> findNeedHealthCheck(int minutesSinceLastCheck);
    
    /**
     * 查询需要熔断恢复的密钥
     */
    List<AiApiKey> findNeedCircuitBreakerRecovery(int minutesSinceFail);
    
    /**
     * 删除密钥（逻辑删除）
     */
    void delete(Long id);
    
    /**
     * 统计指定模型的可用密钥数量
     */
    int countAvailable(String providerCode, String modelCode);
}
