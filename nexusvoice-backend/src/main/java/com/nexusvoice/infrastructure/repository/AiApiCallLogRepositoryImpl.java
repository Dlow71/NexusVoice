package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusvoice.domain.ai.model.AiApiCallLog;
import com.nexusvoice.domain.ai.repository.AiApiCallLogRepository;
import com.nexusvoice.infrastructure.database.mapper.AiApiCallLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI API调用日志仓储实现
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Repository
public class AiApiCallLogRepositoryImpl implements AiApiCallLogRepository {
    
    @Autowired
    private AiApiCallLogMapper aiApiCallLogMapper;
    
    @Override
    public AiApiCallLog save(AiApiCallLog log) {
        if (log.getId() == null) {
            if (log.getCreatedAt() == null) {
                log.setCreatedAt(LocalDateTime.now());
            }
            aiApiCallLogMapper.insert(log);
        } else {
            aiApiCallLogMapper.updateById(log);
        }
        return log;
    }
    
    @Override
    public void saveAll(List<AiApiCallLog> logs) {
        for (AiApiCallLog log : logs) {
            save(log);
        }
    }
    
    @Override
    public Optional<AiApiCallLog> findById(Long id) {
        AiApiCallLog log = aiApiCallLogMapper.selectById(id);
        return Optional.ofNullable(log);
    }
    
    @Override
    public List<AiApiCallLog> findByUserId(Long userId, int limit) {
        LambdaQueryWrapper<AiApiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiCallLog::getUserId, userId)
                .orderByDesc(AiApiCallLog::getCreatedAt);
        
        Page<AiApiCallLog> page = new Page<>(1, limit);
        return aiApiCallLogMapper.selectPage(page, wrapper).getRecords();
    }
    
    @Override
    public List<AiApiCallLog> findByConversationId(Long conversationId) {
        LambdaQueryWrapper<AiApiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiCallLog::getConversationId, conversationId)
                .orderByAsc(AiApiCallLog::getCreatedAt);
        
        return aiApiCallLogMapper.selectList(wrapper);
    }
    
    @Override
    public List<AiApiCallLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AiApiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AiApiCallLog::getRequestTime, startTime, endTime)
                .orderByDesc(AiApiCallLog::getRequestTime);
        
        return aiApiCallLogMapper.selectList(wrapper);
    }
    
    @Override
    public Long countByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AiApiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AiApiCallLog::getRequestTime, startTime, endTime);
        
        return aiApiCallLogMapper.selectCount(wrapper);
    }
    
    @Override
    public BigDecimal sumCostByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        BigDecimal sum = aiApiCallLogMapper.sumCostByTimeRange(startTime, endTime);
        return sum != null ? sum : BigDecimal.ZERO;
    }
    
    @Override
    public Long sumTokensByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        Long sum = aiApiCallLogMapper.sumTokensByTimeRange(startTime, endTime);
        return sum != null ? sum : 0L;
    }
    
    @Override
    public Map<String, Long> countByModel(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> results = aiApiCallLogMapper.countByModel(startTime, endTime);
        
        Map<String, Long> countMap = new HashMap<>();
        for (Map<String, Object> result : results) {
            String modelKey = (String) result.get("model_key");
            Long count = ((Number) result.get("count")).longValue();
            countMap.put(modelKey, count);
        }
        
        return countMap;
    }
    
    @Override
    public Map<Long, Long> countByUser(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> results = aiApiCallLogMapper.countByUser(startTime, endTime);
        
        Map<Long, Long> countMap = new HashMap<>();
        for (Map<String, Object> result : results) {
            Long userId = ((Number) result.get("user_id")).longValue();
            Long count = ((Number) result.get("count")).longValue();
            countMap.put(userId, count);
        }
        
        return countMap;
    }
    
    @Override
    public List<AiApiCallLog> findByApiKeyId(Long apiKeyId, int limit) {
        LambdaQueryWrapper<AiApiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiCallLog::getApiKeyId, apiKeyId)
                .orderByDesc(AiApiCallLog::getCreatedAt);
        
        Page<AiApiCallLog> page = new Page<>(1, limit);
        return aiApiCallLogMapper.selectPage(page, wrapper).getRecords();
    }
    
    @Override
    public List<AiApiCallLog> findFailedCalls(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AiApiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(AiApiCallLog::getRequestTime, startTime, endTime)
                .eq(AiApiCallLog::getStatus, 0)
                .orderByDesc(AiApiCallLog::getRequestTime);
        
        return aiApiCallLogMapper.selectList(wrapper);
    }
    
    @Override
    public Double avgResponseTime(String providerCode, String modelCode, 
                                  LocalDateTime startTime, LocalDateTime endTime) {
        Double avg = aiApiCallLogMapper.avgResponseTime(providerCode, modelCode, startTime, endTime);
        return avg != null ? avg : 0.0;
    }
    
    @Override
    public Double calculateSuccessRate(String providerCode, String modelCode,
                                      LocalDateTime startTime, LocalDateTime endTime) {
        Double rate = aiApiCallLogMapper.calculateSuccessRate(providerCode, modelCode, startTime, endTime);
        return rate != null ? rate : 0.0;
    }
    
    @Override
    public void deleteOldLogs(int daysToKeep) {
        aiApiCallLogMapper.deleteOldLogs(daysToKeep);
        log.info("清理{}天前的API调用日志", daysToKeep);
    }
}
