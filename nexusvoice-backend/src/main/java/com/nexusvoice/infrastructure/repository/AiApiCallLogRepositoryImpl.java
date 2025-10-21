package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusvoice.domain.ai.model.AiApiCallLog;
import com.nexusvoice.domain.ai.repository.AiApiCallLogRepository;
import com.nexusvoice.infrastructure.persistence.converter.AiApiCallLogPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.AiApiCallLogPOMapper;
import com.nexusvoice.infrastructure.persistence.po.AiApiCallLogPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AI API调用日志仓储实现
 * 使用PO层进行持久化操作
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Repository
public class AiApiCallLogRepositoryImpl implements AiApiCallLogRepository {
    
    private final AiApiCallLogPOMapper mapper;
    private final AiApiCallLogPOConverter converter;
    
    public AiApiCallLogRepositoryImpl(AiApiCallLogPOMapper mapper, AiApiCallLogPOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }
    
    @Override
    public AiApiCallLog save(AiApiCallLog log) {
        AiApiCallLogPO po = converter.toPO(log);
        if (po.getId() == null) {
            if (po.getCreatedAt() == null) {
                po.setCreatedAt(LocalDateTime.now());
            }
            mapper.insert(po);
            log.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return converter.toDomain(po);
    }
    
    @Override
    public void saveAll(List<AiApiCallLog> logs) {
        for (AiApiCallLog log : logs) {
            save(log);
        }
    }
    
    @Override
    public Optional<AiApiCallLog> findById(Long id) {
        AiApiCallLogPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }
    
    @Override
    public List<AiApiCallLog> findByUserId(Long userId, int limit) {
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiCallLogPO::getUserId, userId)
                .orderByDesc(AiApiCallLogPO::getCreatedAt);
        
        Page<AiApiCallLogPO> page = new Page<>(1, limit);
        return mapper.selectPage(page, wrapper).getRecords().stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AiApiCallLog> findByConversationId(Long conversationId) {
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiCallLogPO::getConversationId, conversationId)
                .orderByAsc(AiApiCallLogPO::getCreatedAt);
        
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AiApiCallLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AiApiCallLogPO::getRequestTime, startTime, endTime)
                .orderByDesc(AiApiCallLogPO::getRequestTime);
        
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Long countByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AiApiCallLogPO::getRequestTime, startTime, endTime);
        
        return mapper.selectCount(wrapper);
    }
    
    @Override
    public BigDecimal sumCostByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        // 使用LambdaQueryWrapper查询后手动求和
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AiApiCallLogPO::getRequestTime, startTime, endTime);
        List<AiApiCallLogPO> logs = mapper.selectList(wrapper);
        return logs.stream()
                .map(AiApiCallLogPO::getTotalCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Override
    public Long sumTokensByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        // 使用LambdaQueryWrapper查询后手动求和
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AiApiCallLogPO::getRequestTime, startTime, endTime);
        List<AiApiCallLogPO> logs = mapper.selectList(wrapper);
        return logs.stream()
                .map(AiApiCallLogPO::getTotalTokens)
                .filter(tokens -> tokens != null)
                .mapToLong(Integer::longValue)
                .sum();
    }
    
    @Override
    public Map<String, Long> countByModel(LocalDateTime startTime, LocalDateTime endTime) {
        // 使用LambdaQueryWrapper查询后手动分组统计
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AiApiCallLogPO::getRequestTime, startTime, endTime);
        List<AiApiCallLogPO> logs = mapper.selectList(wrapper);
        return logs.stream()
                .collect(Collectors.groupingBy(
                        po -> po.getProviderCode() + ":" + po.getModelCode(),
                        Collectors.counting()
                ));
    }
    
    @Override
    public Map<Long, Long> countByUser(LocalDateTime startTime, LocalDateTime endTime) {
        // 使用LambdaQueryWrapper查询后手动分组统计
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AiApiCallLogPO::getRequestTime, startTime, endTime);
        List<AiApiCallLogPO> logs = mapper.selectList(wrapper);
        return logs.stream()
                .filter(po -> po.getUserId() != null)
                .collect(Collectors.groupingBy(
                        AiApiCallLogPO::getUserId,
                        Collectors.counting()
                ));
    }
    
    @Override
    public List<AiApiCallLog> findByApiKeyId(Long apiKeyId, int limit) {
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiCallLogPO::getApiKeyId, apiKeyId)
                .orderByDesc(AiApiCallLogPO::getCreatedAt);
        
        Page<AiApiCallLogPO> page = new Page<>(1, limit);
        return mapper.selectPage(page, wrapper).getRecords().stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AiApiCallLog> findFailedCalls(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AiApiCallLogPO::getRequestTime, startTime, endTime)
                .eq(AiApiCallLogPO::getStatus, 0)
                .orderByDesc(AiApiCallLogPO::getRequestTime);
        
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Double avgResponseTime(String providerCode, String modelCode, 
                                  LocalDateTime startTime, LocalDateTime endTime) {
        // 使用LambdaQueryWrapper查询后手动计算平均值
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiCallLogPO::getProviderCode, providerCode)
                .eq(AiApiCallLogPO::getModelCode, modelCode)
                .between(AiApiCallLogPO::getRequestTime, startTime, endTime);
        List<AiApiCallLogPO> logs = mapper.selectList(wrapper);
        return logs.stream()
                .map(AiApiCallLogPO::getResponseTimeMs)
                .filter(time -> time != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }
    
    @Override
    public Double calculateSuccessRate(String providerCode, String modelCode,
                                      LocalDateTime startTime, LocalDateTime endTime) {
        // 使用LambdaQueryWrapper查询后手动计算成功率
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiCallLogPO::getProviderCode, providerCode)
                .eq(AiApiCallLogPO::getModelCode, modelCode)
                .between(AiApiCallLogPO::getRequestTime, startTime, endTime);
        List<AiApiCallLogPO> logs = mapper.selectList(wrapper);
        if (logs.isEmpty()) {
            return 0.0;
        }
        long successCount = logs.stream()
                .filter(po -> po.getStatus() != null && po.getStatus() == 1)
                .count();
        return (double) successCount / logs.size() * 100;
    }
    
    @Override
    public void deleteOldLogs(int daysToKeep) {
        // 使用LambdaQueryWrapper删除旧日志
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
        LambdaQueryWrapper<AiApiCallLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(AiApiCallLogPO::getCreatedAt, cutoffTime);
        mapper.delete(wrapper);
        log.info("清理{}天前的API调用日志", daysToKeep);
    }
}
