package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.config.model.SystemConfig;
import com.nexusvoice.domain.config.repository.SystemConfigRepository;
import com.nexusvoice.infrastructure.cache.SystemConfigCacheInfraService;
import com.nexusvoice.infrastructure.persistence.converter.SystemConfigPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.SystemConfigPOMapper;
import com.nexusvoice.infrastructure.persistence.po.SystemConfigPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置仓储实现类
 * 包含缓存管理功能
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
@Repository
public class SystemConfigRepositoryImpl implements SystemConfigRepository {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigRepositoryImpl.class);

    private final SystemConfigPOMapper mapper;
    private final SystemConfigPOConverter converter;
    
    @Autowired(required = false)
    private SystemConfigCacheInfraService cacheService;

    public SystemConfigRepositoryImpl(SystemConfigPOMapper mapper, SystemConfigPOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public Optional<SystemConfig> findById(Long id) {
        logger.debug("根据ID查询配置: {}", id);
        SystemConfigPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public Optional<SystemConfig> findByKey(String configKey) {
        logger.debug("根据配置键查询配置: {}", configKey);
        
        // 1. 先尝试从缓存获取
        if (cacheService != null) {
            String cachedValue = cacheService.getConfigValue(configKey);
            if (cachedValue != null) {
                // 缓存命中，构造配置对象（只包含key和value）
                SystemConfig config = new SystemConfig();
                config.setConfigKey(configKey);
                config.setConfigValue(cachedValue);
                config.setEnabled(true);
                logger.debug("从缓存获取配置: {}", configKey);
                return Optional.of(config);
            }
        }
        
        // 2. 缓存未命中，从数据库查询
        LambdaQueryWrapper<SystemConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfigPO::getConfigKey, configKey)
                    .eq(SystemConfigPO::getDeleted, 0);
        SystemConfigPO po = mapper.selectOne(queryWrapper);
        SystemConfig config = converter.toDomain(po);
        
        // 3. 写入缓存
        if (cacheService != null && config != null && config.isActive()) {
            cacheService.setConfigValue(configKey, config.getConfigValue());
            logger.debug("配置已写入缓存: {}", configKey);
        } else if (cacheService != null) {
            // 配置不存在或未启用，缓存null值防止穿透
            cacheService.setConfigValue(configKey, null);
            logger.debug("配置不存在，已缓存空值: {}", configKey);
        }
        
        return Optional.ofNullable(config);
    }

    @Override
    public List<SystemConfig> findByGroup(String configGroup) {
        logger.debug("根据配置分组查询配置列表: {}", configGroup);
        LambdaQueryWrapper<SystemConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfigPO::getConfigGroup, configGroup)
                    .eq(SystemConfigPO::getDeleted, 0)
                    .orderByAsc(SystemConfigPO::getSortOrder, SystemConfigPO::getId);
        return mapper.selectList(queryWrapper).stream()
                .map(converter::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<SystemConfig> findAllEnabled() {
        logger.debug("查询所有启用的配置");
        LambdaQueryWrapper<SystemConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfigPO::getEnabled, 1)
                    .eq(SystemConfigPO::getDeleted, 0)
                    .orderByAsc(SystemConfigPO::getSortOrder, SystemConfigPO::getId);
        return mapper.selectList(queryWrapper).stream()
                .map(converter::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<SystemConfig> findAll(int page, int size) {
        logger.debug("分页查询所有配置，页码: {}, 每页大小: {}", page, size);
        int offset = (page - 1) * size;
        
        LambdaQueryWrapper<SystemConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfigPO::getDeleted, 0)
                    .orderByAsc(SystemConfigPO::getSortOrder, SystemConfigPO::getId)
                    .last("LIMIT " + size + " OFFSET " + offset);
        
        return mapper.selectList(queryWrapper).stream()
                .map(converter::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<SystemConfig> findByCondition(String configKey, String configGroup, Boolean enabled, int page, int size) {
        logger.debug("根据条件查询配置列表，配置键: {}, 分组: {}, 启用: {}, 页码: {}, 每页大小: {}", 
                configKey, configGroup, enabled, page, size);
        
        int offset = (page - 1) * size;
        LambdaQueryWrapper<SystemConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfigPO::getDeleted, 0);
        
        if (configKey != null && !configKey.trim().isEmpty()) {
            queryWrapper.like(SystemConfigPO::getConfigKey, configKey);
        }
        if (configGroup != null && !configGroup.trim().isEmpty()) {
            queryWrapper.eq(SystemConfigPO::getConfigGroup, configGroup);
        }
        if (enabled != null) {
            queryWrapper.eq(SystemConfigPO::getEnabled, enabled ? 1 : 0);
        }
        
        queryWrapper.orderByAsc(SystemConfigPO::getSortOrder, SystemConfigPO::getId)
                    .last("LIMIT " + size + " OFFSET " + offset);
        
        return mapper.selectList(queryWrapper).stream()
                .map(converter::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public long count() {
        logger.debug("统计配置总数");
        LambdaQueryWrapper<SystemConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfigPO::getDeleted, 0);
        return mapper.selectCount(queryWrapper);
    }

    @Override
    public long countByCondition(String configKey, String configGroup, Boolean enabled) {
        logger.debug("根据条件统计配置数量，配置键: {}, 分组: {}, 启用: {}", configKey, configGroup, enabled);
        LambdaQueryWrapper<SystemConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfigPO::getDeleted, 0);
        
        if (configKey != null && !configKey.trim().isEmpty()) {
            queryWrapper.like(SystemConfigPO::getConfigKey, configKey);
        }
        if (configGroup != null && !configGroup.trim().isEmpty()) {
            queryWrapper.eq(SystemConfigPO::getConfigGroup, configGroup);
        }
        if (enabled != null) {
            queryWrapper.eq(SystemConfigPO::getEnabled, enabled ? 1 : 0);
        }
        
        return mapper.selectCount(queryWrapper);
    }

    @Override
    public SystemConfig save(SystemConfig systemConfig) {
        logger.debug("保存配置: {}", systemConfig.getConfigKey());
        SystemConfigPO po = converter.toPO(systemConfig);
        int result = mapper.insert(po);
        if (result > 0) {
            systemConfig.setId(po.getId());
            logger.info("配置保存成功，ID: {}", systemConfig.getId());
            return systemConfig;
        } else {
            logger.error("配置保存失败: {}", systemConfig.getConfigKey());
            throw new RuntimeException("配置保存失败");
        }
    }

    @Override
    public SystemConfig update(SystemConfig systemConfig) {
        logger.debug("更新配置: {}", systemConfig.getId());
        SystemConfigPO po = converter.toPO(systemConfig);
        int result = mapper.updateById(po);
        if (result > 0) {
            logger.info("配置更新成功，ID: {}", systemConfig.getId());
            return systemConfig;
        } else {
            logger.error("配置更新失败，ID: {}", systemConfig.getId());
            throw new RuntimeException("配置更新失败");
        }
    }

    @Override
    public boolean deleteById(Long id) {
        logger.debug("删除配置: {}", id);
        int result = mapper.deleteById(id);
        boolean success = result > 0;
        if (success) {
            logger.info("配置删除成功，ID: {}", id);
        } else {
            logger.warn("配置删除失败，ID: {}", id);
        }
        return success;
    }

    @Override
    public boolean deleteByKey(String configKey) {
        logger.debug("根据配置键删除配置: {}", configKey);
        
        LambdaQueryWrapper<SystemConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfigPO::getConfigKey, configKey);
        
        int result = mapper.delete(queryWrapper);
        boolean success = result > 0;
        if (success) {
            logger.info("配置删除成功，配置键: {}", configKey);
        } else {
            logger.warn("配置删除失败，配置键: {}", configKey);
        }
        return success;
    }

    @Override
    public boolean existsByKey(String configKey) {
        logger.debug("检查配置键是否存在: {}", configKey);
        LambdaQueryWrapper<SystemConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfigPO::getConfigKey, configKey)
                    .eq(SystemConfigPO::getDeleted, 0);
        return mapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean existsByKeyExcludeId(String configKey, Long excludeId) {
        logger.debug("检查配置键是否存在（排除ID）: {}, 排除ID: {}", configKey, excludeId);
        LambdaQueryWrapper<SystemConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfigPO::getConfigKey, configKey)
                    .ne(SystemConfigPO::getId, excludeId)
                    .eq(SystemConfigPO::getDeleted, 0);
        return mapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public int batchSave(List<SystemConfig> systemConfigs) {
        logger.debug("批量保存配置，数量: {}", systemConfigs.size());
        
        int successCount = 0;
        for (SystemConfig config : systemConfigs) {
            try {
                SystemConfigPO po = converter.toPO(config);
                mapper.insert(po);
                config.setId(po.getId());
                successCount++;
            } catch (Exception e) {
                logger.error("批量保存配置失败，配置键: {}", config.getConfigKey(), e);
            }
        }
        
        logger.info("批量保存配置完成，成功数量: {}", successCount);
        return successCount;
    }

    @Override
    public int batchUpdateStatus(List<Long> ids, Boolean enabled) {
        logger.debug("批量更新配置状态，配置数量: {}, 状态: {}", ids.size(), enabled);
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SystemConfigPO> updateWrapper = 
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.in(SystemConfigPO::getId, ids)
                     .set(SystemConfigPO::getEnabled, enabled ? 1 : 0);
        int result = mapper.update(null, updateWrapper);
        logger.info("批量更新配置状态完成，更新数量: {}", result);
        return result;
    }

    @Override
    public void refreshCache(String configKey) {
        if (cacheService != null) {
            logger.debug("刷新配置缓存，配置键: {}", configKey);
            cacheService.evictConfig(configKey);
        } else {
            logger.debug("缓存服务未启用，跳过缓存刷新");
        }
    }

    @Override
    public void refreshAllCache() {
        if (cacheService != null) {
            logger.info("刷新所有配置缓存");
            cacheService.evictAllConfigs();
        } else {
            logger.debug("缓存服务未启用，跳过缓存刷新");
        }
    }
}
