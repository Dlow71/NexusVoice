package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.repository.AiApiKeyRepository;
import com.nexusvoice.infrastructure.database.mapper.AiApiKeyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI API密钥仓储实现
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Repository
public class AiApiKeyRepositoryImpl implements AiApiKeyRepository {
    
    @Autowired
    private AiApiKeyMapper aiApiKeyMapper;
    
    @Override
    public Optional<AiApiKey> findById(Long id) {
        AiApiKey apiKey = aiApiKeyMapper.selectById(id);
        return Optional.ofNullable(apiKey);
    }
    
    @Override
    public List<AiApiKey> findAvailableByModel(String providerCode, String modelCode) {
        LambdaQueryWrapper<AiApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiKey::getProviderCode, providerCode)
                .eq(AiApiKey::getModelCode, modelCode)
                .eq(AiApiKey::getStatus, 1)
                .eq(AiApiKey::getDeleted, 0)
                .orderByAsc(AiApiKey::getLastUsedAt);
        
        return aiApiKeyMapper.selectList(wrapper);
    }
    
    @Override
    public List<AiApiKey> findAllByModel(String providerCode, String modelCode) {
        LambdaQueryWrapper<AiApiKey> wrapper = new LambdaQueryWrapper<>();
        
        // 如果providerCode和modelCode都为null，查询所有
        if (providerCode != null && modelCode != null) {
            wrapper.eq(AiApiKey::getProviderCode, providerCode)
                    .eq(AiApiKey::getModelCode, modelCode);
        }
        
        wrapper.eq(AiApiKey::getDeleted, 0)
                .orderByDesc(AiApiKey::getStatus)
                .orderByAsc(AiApiKey::getLastUsedAt);
        
        return aiApiKeyMapper.selectList(wrapper);
    }
    
    @Override
    public AiApiKey save(AiApiKey apiKey) {
        if (apiKey.getId() == null) {
            // 新增
            apiKey.setCreatedAt(LocalDateTime.now());
            aiApiKeyMapper.insert(apiKey);
        } else {
            // 更新
            apiKey.setUpdatedAt(LocalDateTime.now());
            aiApiKeyMapper.updateById(apiKey);
        }
        return apiKey;
    }
    
    @Override
    public void saveAll(List<AiApiKey> apiKeys) {
        for (AiApiKey apiKey : apiKeys) {
            save(apiKey);
        }
    }
    
    @Override
    public void updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<AiApiKey> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKey::getId, id)
                .set(AiApiKey::getStatus, status)
                .set(AiApiKey::getUpdatedAt, LocalDateTime.now());
        
        aiApiKeyMapper.update(null, wrapper);
        log.info("更新API密钥状态，ID：{}，状态：{}", id, status);
    }
    
    @Override
    public void updateUsageStats(Long id, Integer tokens, BigDecimal cost) {
        aiApiKeyMapper.updateUsageStats(id, tokens, cost, LocalDateTime.now());
    }
    
    @Override
    public void markFailed(Long id) {
        aiApiKeyMapper.markFailed(id, LocalDateTime.now());
    }
    
    @Override
    public void markSuccess(Long id) {
        LambdaUpdateWrapper<AiApiKey> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKey::getId, id)
                .set(AiApiKey::getFailCount, 0)
                .set(AiApiKey::getLastSuccessTime, LocalDateTime.now())
                .set(AiApiKey::getStatus, 1);
        
        aiApiKeyMapper.update(null, wrapper);
    }
    
    @Override
    public void updateLastUsedTime(Long id, LocalDateTime time) {
        LambdaUpdateWrapper<AiApiKey> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKey::getId, id)
                .set(AiApiKey::getLastUsedAt, time);
        
        aiApiKeyMapper.update(null, wrapper);
    }
    
    @Override
    public void resetDailyQuota(Long id) {
        LambdaUpdateWrapper<AiApiKey> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKey::getId, id)
                .set(AiApiKey::getDailyTokensUsed, 0);
        
        aiApiKeyMapper.update(null, wrapper);
    }
    
    @Override
    public void resetMonthlyQuota(Long id) {
        LambdaUpdateWrapper<AiApiKey> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKey::getId, id)
                .set(AiApiKey::getMonthlyRequests, 0)
                .set(AiApiKey::getMonthlyTokensUsed, 0)
                .set(AiApiKey::getMonthlyCost, BigDecimal.ZERO)
                .set(AiApiKey::getMonthlyResetDate, LocalDate.now());
        
        aiApiKeyMapper.update(null, wrapper);
    }
    
    @Override
    public void resetAllDailyQuota() {
        aiApiKeyMapper.resetAllDailyQuota();
        log.info("重置所有API密钥的每日配额");
    }
    
    @Override
    public void resetMonthlyQuotaByDate(LocalDate date) {
        aiApiKeyMapper.resetMonthlyQuota();
        log.info("重置月度配额，日期：{}", date);
    }
    
    @Override
    public void updateHealthCheckTime(Long id, LocalDateTime time) {
        LambdaUpdateWrapper<AiApiKey> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKey::getId, id)
                .set(AiApiKey::getHealthCheckTime, time);
        
        aiApiKeyMapper.update(null, wrapper);
    }
    
    @Override
    public List<AiApiKey> findNeedHealthCheck(int minutesSinceLastCheck) {
        LocalDateTime checkTime = LocalDateTime.now().minusMinutes(minutesSinceLastCheck);
        
        LambdaQueryWrapper<AiApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiKey::getDeleted, 0)
                .and(w -> w.isNull(AiApiKey::getHealthCheckTime)
                         .or()
                         .lt(AiApiKey::getHealthCheckTime, checkTime))
                .orderByAsc(AiApiKey::getHealthCheckTime);
        
        return aiApiKeyMapper.selectList(wrapper);
    }
    
    @Override
    public List<AiApiKey> findNeedCircuitBreakerRecovery(int minutesSinceFail) {
        LocalDateTime recoverTime = LocalDateTime.now().minusMinutes(minutesSinceFail);
        
        LambdaQueryWrapper<AiApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiKey::getDeleted, 0)
                .eq(AiApiKey::getStatus, 0)
                .ge(AiApiKey::getFailCount, 5)
                .le(AiApiKey::getLastFailTime, recoverTime);
        
        return aiApiKeyMapper.selectList(wrapper);
    }
    
    @Override
    public void delete(Long id) {
        LambdaUpdateWrapper<AiApiKey> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKey::getId, id)
                .set(AiApiKey::getDeleted, 1)
                .set(AiApiKey::getUpdatedAt, LocalDateTime.now());
        
        aiApiKeyMapper.update(null, wrapper);
        log.info("逻辑删除API密钥，ID：{}", id);
    }
    
    @Override
    public int countAvailable(String providerCode, String modelCode) {
        LambdaQueryWrapper<AiApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiKey::getProviderCode, providerCode)
                .eq(AiApiKey::getModelCode, modelCode)
                .eq(AiApiKey::getStatus, 1)
                .eq(AiApiKey::getDeleted, 0);
        
        return aiApiKeyMapper.selectCount(wrapper).intValue();
    }
}
