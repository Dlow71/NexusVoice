package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.repository.AiApiKeyRepository;
import com.nexusvoice.infrastructure.persistence.converter.AiApiKeyPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.AiApiKeyPOMapper;
import com.nexusvoice.infrastructure.persistence.po.AiApiKeyPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AI API密钥仓储实现
 * 使用PO层进行持久化操作
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Repository
public class AiApiKeyRepositoryImpl implements AiApiKeyRepository {
    
    private final AiApiKeyPOMapper mapper;
    private final AiApiKeyPOConverter converter;
    
    public AiApiKeyRepositoryImpl(AiApiKeyPOMapper mapper, AiApiKeyPOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }
    
    @Override
    public Optional<AiApiKey> findById(Long id) {
        AiApiKeyPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }
    
    @Override
    public List<AiApiKey> findAvailableByModel(String providerCode, String modelCode) {
        LambdaQueryWrapper<AiApiKeyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiKeyPO::getProviderCode, providerCode)
                .eq(AiApiKeyPO::getModelCode, modelCode)
                .eq(AiApiKeyPO::getStatus, 1)
                .eq(AiApiKeyPO::getDeleted, 0)
                .orderByAsc(AiApiKeyPO::getLastUsedAt);
        
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AiApiKey> findAllByModel(String providerCode, String modelCode) {
        LambdaQueryWrapper<AiApiKeyPO> wrapper = new LambdaQueryWrapper<>();
        
        // 如果providerCode和modelCode都为null，查询所有
        if (providerCode != null && modelCode != null) {
            wrapper.eq(AiApiKeyPO::getProviderCode, providerCode)
                    .eq(AiApiKeyPO::getModelCode, modelCode);
        }
        
        wrapper.eq(AiApiKeyPO::getDeleted, 0)
                .orderByDesc(AiApiKeyPO::getStatus)
                .orderByAsc(AiApiKeyPO::getLastUsedAt);
        
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public AiApiKey save(AiApiKey apiKey) {
        AiApiKeyPO po = converter.toPO(apiKey);
        if (po.getId() == null) {
            // 新增
            po.setCreatedAt(LocalDateTime.now());
            mapper.insert(po);
            apiKey.setId(po.getId());
        } else {
            // 更新
            po.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(po);
        }
        return converter.toDomain(po);
    }
    
    @Override
    public void saveAll(List<AiApiKey> apiKeys) {
        for (AiApiKey apiKey : apiKeys) {
            save(apiKey);
        }
    }
    
    @Override
    public void updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<AiApiKeyPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKeyPO::getId, id)
                .set(AiApiKeyPO::getStatus, status)
                .set(AiApiKeyPO::getUpdatedAt, LocalDateTime.now());
        
        mapper.update(null, wrapper);
        log.info("更新API密钥状态，ID：{}，状态：{}", id, status);
    }
    
    @Override
    public void updateUsageStats(Long id, Integer tokens, BigDecimal cost) {
        // 使用LambdaUpdateWrapper代替自定义方法
        AiApiKeyPO po = mapper.selectById(id);
        if (po != null) {
            AiApiKey domain = converter.toDomain(po);
            domain.updateUsageStats(tokens, cost);
            mapper.updateById(converter.toPO(domain));
        }
    }
    
    @Override
    public void markFailed(Long id) {
        AiApiKeyPO po = mapper.selectById(id);
        if (po != null) {
            AiApiKey domain = converter.toDomain(po);
            domain.markFailed();
            mapper.updateById(converter.toPO(domain));
        }
    }
    
    @Override
    public void markSuccess(Long id) {
        LambdaUpdateWrapper<AiApiKeyPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKeyPO::getId, id)
                .set(AiApiKeyPO::getFailCount, 0)
                .set(AiApiKeyPO::getLastSuccessTime, LocalDateTime.now())
                .set(AiApiKeyPO::getStatus, 1);
        
        mapper.update(null, wrapper);
    }
    
    @Override
    public void updateLastUsedTime(Long id, LocalDateTime time) {
        LambdaUpdateWrapper<AiApiKeyPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKeyPO::getId, id)
                .set(AiApiKeyPO::getLastUsedAt, time);
        
        mapper.update(null, wrapper);
    }
    
    @Override
    public void resetDailyQuota(Long id) {
        LambdaUpdateWrapper<AiApiKeyPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKeyPO::getId, id)
                .set(AiApiKeyPO::getDailyTokensUsed, 0);
        
        mapper.update(null, wrapper);
    }
    
    @Override
    public void resetMonthlyQuota(Long id) {
        LambdaUpdateWrapper<AiApiKeyPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKeyPO::getId, id)
                .set(AiApiKeyPO::getMonthlyRequests, 0)
                .set(AiApiKeyPO::getMonthlyTokensUsed, 0)
                .set(AiApiKeyPO::getMonthlyCost, BigDecimal.ZERO)
                .set(AiApiKeyPO::getMonthlyResetDate, LocalDate.now());
        
        mapper.update(null, wrapper);
    }
    
    @Override
    public void resetAllDailyQuota() {
        LambdaUpdateWrapper<AiApiKeyPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(AiApiKeyPO::getDailyTokensUsed, 0);
        mapper.update(null, wrapper);
        log.info("重置所有API密钥的每日配额");
    }
    
    @Override
    public void resetMonthlyQuotaByDate(LocalDate date) {
        LambdaUpdateWrapper<AiApiKeyPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(AiApiKeyPO::getMonthlyRequests, 0)
                .set(AiApiKeyPO::getMonthlyTokensUsed, 0)
                .set(AiApiKeyPO::getMonthlyCost, BigDecimal.ZERO)
                .set(AiApiKeyPO::getMonthlyResetDate, date);
        mapper.update(null, wrapper);
        log.info("重置月度配额，日期：{}", date);
    }
    
    @Override
    public void updateHealthCheckTime(Long id, LocalDateTime time) {
        LambdaUpdateWrapper<AiApiKeyPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKeyPO::getId, id)
                .set(AiApiKeyPO::getHealthCheckTime, time);
        
        mapper.update(null, wrapper);
    }
    
    @Override
    public List<AiApiKey> findNeedHealthCheck(int minutesSinceLastCheck) {
        LocalDateTime checkTime = LocalDateTime.now().minusMinutes(minutesSinceLastCheck);
        
        LambdaQueryWrapper<AiApiKeyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiKeyPO::getDeleted, 0)
                .and(w -> w.isNull(AiApiKeyPO::getHealthCheckTime)
                         .or()
                         .lt(AiApiKeyPO::getHealthCheckTime, checkTime))
                .orderByAsc(AiApiKeyPO::getHealthCheckTime);
        
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AiApiKey> findNeedCircuitBreakerRecovery(int minutesSinceFail) {
        LocalDateTime recoverTime = LocalDateTime.now().minusMinutes(minutesSinceFail);
        
        LambdaQueryWrapper<AiApiKeyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiKeyPO::getDeleted, 0)
                .eq(AiApiKeyPO::getStatus, 0)
                .ge(AiApiKeyPO::getFailCount, 5)
                .le(AiApiKeyPO::getLastFailTime, recoverTime);
        
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void delete(Long id) {
        LambdaUpdateWrapper<AiApiKeyPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiApiKeyPO::getId, id)
                .set(AiApiKeyPO::getDeleted, 1)
                .set(AiApiKeyPO::getUpdatedAt, LocalDateTime.now());
        
        mapper.update(null, wrapper);
        log.info("逻辑删除API密钥，ID：{}", id);
    }
    
    @Override
    public int countAvailable(String providerCode, String modelCode) {
        LambdaQueryWrapper<AiApiKeyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiKeyPO::getProviderCode, providerCode)
                .eq(AiApiKeyPO::getModelCode, modelCode)
                .eq(AiApiKeyPO::getStatus, 1)
                .eq(AiApiKeyPO::getDeleted, 0);
        
        return mapper.selectCount(wrapper).intValue();
    }
}
