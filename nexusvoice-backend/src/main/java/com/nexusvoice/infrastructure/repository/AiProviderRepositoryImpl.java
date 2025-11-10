package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nexusvoice.domain.ai.model.AiProvider;
import com.nexusvoice.domain.ai.repository.AiProviderRepository;
import com.nexusvoice.infrastructure.persistence.converter.AiProviderPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.AiProviderMapper;
import com.nexusvoice.infrastructure.persistence.po.AiProviderPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI服务提供商仓储实现
 * 使用MyBatis-Plus进行数据访问
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Repository
public class AiProviderRepositoryImpl implements AiProviderRepository {
    
    @Autowired
    private AiProviderMapper mapper;
    
    @Autowired
    private AiProviderPOConverter converter;
    
    @Override
    public Optional<AiProvider> findById(Long id) {
        AiProviderPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(converter::toDomain);
    }
    
    @Override
    public Optional<AiProvider> findByCode(String providerCode) {
        LambdaQueryWrapper<AiProviderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderPO::getProviderCode, providerCode)
               .eq(AiProviderPO::getDeleted, 0);
        
        AiProviderPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(converter::toDomain);
    }
    
    @Override
    public List<AiProvider> findAllEnabled() {
        LambdaQueryWrapper<AiProviderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderPO::getStatus, 1)
               .eq(AiProviderPO::getDeleted, 0)
               .orderByAsc(AiProviderPO::getPriority)
               .orderByAsc(AiProviderPO::getId);
        
        List<AiProviderPO> poList = mapper.selectList(wrapper);
        return converter.toDomainList(poList);
    }
    
    @Override
    public List<AiProvider> findAllOfficial() {
        LambdaQueryWrapper<AiProviderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderPO::getIsOfficial, true)
               .eq(AiProviderPO::getDeleted, 0)
               .orderByAsc(AiProviderPO::getPriority)
               .orderByAsc(AiProviderPO::getId);
        
        List<AiProviderPO> poList = mapper.selectList(wrapper);
        return converter.toDomainList(poList);
    }
    
    @Override
    public List<AiProvider> findByUserId(Long userId) {
        LambdaQueryWrapper<AiProviderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderPO::getUserId, userId)
               .eq(AiProviderPO::getIsOfficial, false)
               .eq(AiProviderPO::getDeleted, 0)
               .orderByDesc(AiProviderPO::getCreatedAt);
        
        List<AiProviderPO> poList = mapper.selectList(wrapper);
        return converter.toDomainList(poList);
    }
    
    @Override
    public List<AiProvider> findAll() {
        LambdaQueryWrapper<AiProviderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderPO::getDeleted, 0)
               .orderByAsc(AiProviderPO::getPriority)
               .orderByAsc(AiProviderPO::getId);
        
        List<AiProviderPO> poList = mapper.selectList(wrapper);
        return converter.toDomainList(poList);
    }
    
    @Override
    public AiProvider save(AiProvider provider) {
        AiProviderPO po = converter.toPO(provider);
        
        if (provider.isNew()) {
            // 新建
            provider.onCreate();
            po.setCreatedAt(provider.getCreatedAt());
            po.setUpdatedAt(provider.getUpdatedAt());
            mapper.insert(po);
            provider.setId(po.getId());
            log.info("创建AI服务商成功，ID：{}，代码：{}", po.getId(), provider.getProviderCode());
        } else {
            // 更新
            provider.onUpdate();
            po.setUpdatedAt(provider.getUpdatedAt());
            mapper.updateById(po);
            log.info("更新AI服务商成功，ID：{}，代码：{}", provider.getId(), provider.getProviderCode());
        }
        
        return provider;
    }
    
    @Override
    public int saveBatch(List<AiProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (AiProvider provider : providers) {
            save(provider);
            count++;
        }
        
        log.info("批量保存AI服务商成功，数量：{}", count);
        return count;
    }
    
    @Override
    public boolean delete(Long id) {
        LambdaUpdateWrapper<AiProviderPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiProviderPO::getId, id)
               .set(AiProviderPO::getDeleted, 1)
               .set(AiProviderPO::getUpdatedAt, LocalDateTime.now());
        
        int rows = mapper.update(null, wrapper);
        boolean success = rows > 0;
        
        if (success) {
            log.info("删除AI服务商成功，ID：{}", id);
        } else {
            log.warn("删除AI服务商失败，ID：{}", id);
        }
        
        return success;
    }
    
    @Override
    public boolean existsByCode(String providerCode) {
        LambdaQueryWrapper<AiProviderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderPO::getProviderCode, providerCode)
               .eq(AiProviderPO::getDeleted, 0);
        
        return mapper.selectCount(wrapper) > 0;
    }
    
    @Override
    public boolean existsByCodeExcludeId(String providerCode, Long excludeId) {
        LambdaQueryWrapper<AiProviderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderPO::getProviderCode, providerCode)
               .ne(AiProviderPO::getId, excludeId)
               .eq(AiProviderPO::getDeleted, 0);
        
        return mapper.selectCount(wrapper) > 0;
    }
    
    @Override
    public long count(Boolean isOfficial, Integer status) {
        LambdaQueryWrapper<AiProviderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderPO::getDeleted, 0);
        
        if (isOfficial != null) {
            wrapper.eq(AiProviderPO::getIsOfficial, isOfficial);
        }
        
        if (status != null) {
            wrapper.eq(AiProviderPO::getStatus, status);
        }
        
        return mapper.selectCount(wrapper);
    }
    
    @Override
    public boolean updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<AiProviderPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiProviderPO::getId, id)
               .set(AiProviderPO::getStatus, status)
               .set(AiProviderPO::getUpdatedAt, LocalDateTime.now());
        
        int rows = mapper.update(null, wrapper);
        boolean success = rows > 0;
        
        if (success) {
            log.info("更新AI服务商状态成功，ID：{}，状态：{}", id, status);
        } else {
            log.warn("更新AI服务商状态失败，ID：{}", id);
        }
        
        return success;
    }
}
