package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusvoice.domain.developer.model.DeveloperApiKey;
import com.nexusvoice.domain.developer.repository.DeveloperApiKeyRepository;
import com.nexusvoice.infrastructure.persistence.converter.DeveloperApiKeyPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.DeveloperApiKeyPOMapper;
import com.nexusvoice.infrastructure.persistence.po.DeveloperApiKeyPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 开发者API密钥仓储实现
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Slf4j
@Repository
public class DeveloperApiKeyRepositoryImpl implements DeveloperApiKeyRepository {
    
    @Autowired
    private DeveloperApiKeyPOMapper mapper;
    
    @Autowired
    private DeveloperApiKeyPOConverter converter;
    
    @Override
    public void save(DeveloperApiKey apiKey) {
        DeveloperApiKeyPO po = converter.toPO(apiKey);
        mapper.insert(po);
        // 回写ID
        apiKey.setId(po.getId());
        log.info("保存开发者API密钥成功，ID：{}，名称：{}", po.getId(), po.getKeyName());
    }
    
    @Override
    public void update(DeveloperApiKey apiKey) {
        DeveloperApiKeyPO po = converter.toPO(apiKey);
        mapper.updateById(po);
        log.debug("更新开发者API密钥成功，ID：{}", po.getId());
    }
    
    @Override
    public Optional<DeveloperApiKey> findById(Long id) {
        DeveloperApiKeyPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }
    
    @Override
    public Optional<DeveloperApiKey> findByKeyValueHash(String keyValueHash) {
        LambdaQueryWrapper<DeveloperApiKeyPO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DeveloperApiKeyPO::getKeyValueHash, keyValueHash)
               .eq(DeveloperApiKeyPO::getDeleted, 0);
        
        DeveloperApiKeyPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }
    
    @Override
    public List<DeveloperApiKey> findByUserId(Long userId) {
        LambdaQueryWrapper<DeveloperApiKeyPO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DeveloperApiKeyPO::getUserId, userId)
               .eq(DeveloperApiKeyPO::getDeleted, 0)
               .orderByDesc(DeveloperApiKeyPO::getCreatedAt);
        
        List<DeveloperApiKeyPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<DeveloperApiKey> findByUserIdAndKeyName(Long userId, String keyName) {
        LambdaQueryWrapper<DeveloperApiKeyPO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DeveloperApiKeyPO::getUserId, userId)
               .eq(DeveloperApiKeyPO::getKeyName, keyName)
               .eq(DeveloperApiKeyPO::getDeleted, 0);
        
        DeveloperApiKeyPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }
    
    @Override
    public long countByUserId(Long userId) {
        LambdaQueryWrapper<DeveloperApiKeyPO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DeveloperApiKeyPO::getUserId, userId)
               .eq(DeveloperApiKeyPO::getDeleted, 0);
        
        return mapper.selectCount(wrapper);
    }
    
    @Override
    public void delete(Long id) {
        // 逻辑删除：设置deleted=1
        DeveloperApiKeyPO po = new DeveloperApiKeyPO();
        po.setId(id);
        po.setDeleted(1);
        po.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(po);
        log.info("逻辑删除开发者API密钥成功，ID：{}", id);
    }
    
    @Override
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        for (Long id : ids) {
            delete(id);
        }
        log.info("批量逻辑删除开发者API密钥成功，数量：{}", ids.size());
    }
    
    @Override
    public List<DeveloperApiKey> findExpiredKeys() {
        LambdaQueryWrapper<DeveloperApiKeyPO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DeveloperApiKeyPO::getStatus, "ACTIVE")
               .isNotNull(DeveloperApiKeyPO::getExpireAt)
               .lt(DeveloperApiKeyPO::getExpireAt, LocalDateTime.now())
               .eq(DeveloperApiKeyPO::getDeleted, 0);
        
        List<DeveloperApiKeyPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DeveloperApiKey> findKeysNeedDailyReset() {
        // 查询所有未删除的API密钥（用于定时任务重置今日统计）
        LambdaQueryWrapper<DeveloperApiKeyPO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DeveloperApiKeyPO::getDeleted, 0);
        
        List<DeveloperApiKeyPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
}
