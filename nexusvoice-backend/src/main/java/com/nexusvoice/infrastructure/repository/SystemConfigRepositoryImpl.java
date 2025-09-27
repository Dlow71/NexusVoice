package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.config.model.SystemConfig;
import com.nexusvoice.domain.config.repository.SystemConfigRepository;
import com.nexusvoice.infrastructure.database.mapper.SystemConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
@Repository
public class SystemConfigRepositoryImpl implements SystemConfigRepository {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigRepositoryImpl.class);

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Override
    public Optional<SystemConfig> findById(Long id) {
        logger.debug("根据ID查询配置: {}", id);
        SystemConfig config = systemConfigMapper.selectById(id);
        return Optional.ofNullable(config);
    }

    @Override
    public Optional<SystemConfig> findByKey(String configKey) {
        logger.debug("根据配置键查询配置: {}", configKey);
        SystemConfig config = systemConfigMapper.selectByKey(configKey);
        return Optional.ofNullable(config);
    }

    @Override
    public List<SystemConfig> findByGroup(String configGroup) {
        logger.debug("根据配置分组查询配置列表: {}", configGroup);
        return systemConfigMapper.selectByGroup(configGroup);
    }

    @Override
    public List<SystemConfig> findAllEnabled() {
        logger.debug("查询所有启用的配置");
        return systemConfigMapper.selectAllEnabled();
    }

    @Override
    public List<SystemConfig> findAll(int page, int size) {
        logger.debug("分页查询所有配置，页码: {}, 每页大小: {}", page, size);
        int offset = (page - 1) * size;
        
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(SystemConfig::getSortOrder, SystemConfig::getId);
        queryWrapper.last("LIMIT " + offset + ", " + size);
        
        return systemConfigMapper.selectList(queryWrapper);
    }

    @Override
    public List<SystemConfig> findByCondition(String configKey, String configGroup, Boolean enabled, int page, int size) {
        logger.debug("根据条件查询配置列表，配置键: {}, 分组: {}, 启用: {}, 页码: {}, 每页大小: {}", 
                configKey, configGroup, enabled, page, size);
        
        int offset = (page - 1) * size;
        return systemConfigMapper.selectByCondition(configKey, configGroup, enabled, offset, size);
    }

    @Override
    public long count() {
        logger.debug("统计配置总数");
        return systemConfigMapper.selectCount(null);
    }

    @Override
    public long countByCondition(String configKey, String configGroup, Boolean enabled) {
        logger.debug("根据条件统计配置数量，配置键: {}, 分组: {}, 启用: {}", configKey, configGroup, enabled);
        return systemConfigMapper.countByCondition(configKey, configGroup, enabled);
    }

    @Override
    public SystemConfig save(SystemConfig systemConfig) {
        logger.debug("保存配置: {}", systemConfig.getConfigKey());
        int result = systemConfigMapper.insert(systemConfig);
        if (result > 0) {
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
        int result = systemConfigMapper.updateById(systemConfig);
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
        int result = systemConfigMapper.deleteById(id);
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
        
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getConfigKey, configKey);
        
        int result = systemConfigMapper.delete(queryWrapper);
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
        int count = systemConfigMapper.countByKey(configKey);
        return count > 0;
    }

    @Override
    public boolean existsByKeyExcludeId(String configKey, Long excludeId) {
        logger.debug("检查配置键是否存在（排除ID）: {}, 排除ID: {}", configKey, excludeId);
        int count = systemConfigMapper.countByKeyExcludeId(configKey, excludeId);
        return count > 0;
    }

    @Override
    public int batchSave(List<SystemConfig> systemConfigs) {
        logger.debug("批量保存配置，数量: {}", systemConfigs.size());
        
        int successCount = 0;
        for (SystemConfig config : systemConfigs) {
            try {
                systemConfigMapper.insert(config);
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
        int result = systemConfigMapper.batchUpdateStatus(ids, enabled);
        logger.info("批量更新配置状态完成，更新数量: {}", result);
        return result;
    }
}
