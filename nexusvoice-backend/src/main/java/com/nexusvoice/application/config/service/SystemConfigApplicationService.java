package com.nexusvoice.application.config.service;

import com.nexusvoice.application.config.assembler.SystemConfigAssembler;
import com.nexusvoice.application.config.dto.SystemConfigCreateRequest;
import com.nexusvoice.application.config.dto.SystemConfigDto;
import com.nexusvoice.application.config.dto.SystemConfigQueryRequest;
import com.nexusvoice.application.config.dto.SystemConfigUpdateRequest;
import com.nexusvoice.common.Result;
import com.nexusvoice.domain.config.model.SystemConfig;
import com.nexusvoice.domain.config.repository.SystemConfigRepository;
import com.nexusvoice.domain.config.service.SystemConfigCacheService;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 系统配置应用服务
 * 
 * @author NexusVoice
 * @since 2025-09-27
 * @updated 2025-10-18 添加缓存一致性管理
 */
@Service
public class SystemConfigApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigApplicationService.class);

    @Autowired
    private SystemConfigRepository systemConfigRepository;
    
    @Autowired
    private SystemConfigCacheService cacheService;

    /**
     * 创建系统配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<SystemConfigDto> createConfig(SystemConfigCreateRequest request) {
        logger.info("创建系统配置，配置键: {}", request.getConfigKey());

        try {
            // 检查配置键是否已存在
            if (systemConfigRepository.existsByKey(request.getConfigKey())) {
                logger.warn("配置键已存在: {}", request.getConfigKey());
                throw BizException.of(ErrorCodeEnum.CONFIG_ALREADY_EXISTS, "配置键已存在");
            }

            // 转换为领域对象并保存
            SystemConfig systemConfig = SystemConfigAssembler.toDomain(request);
            SystemConfig savedConfig = systemConfigRepository.save(systemConfig);
            
            // 注册事务同步器，确保事务提交后才刷新缓存
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 第一次删除缓存（立即删除）
                        cacheService.evictConfig(savedConfig.getConfigKey());
                        logger.info("第一次删除缓存: {}", savedConfig.getConfigKey());
                        
                        // 延迟双删：500ms后再次删除，确保清除延迟读取的老数据
                        CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                                .execute(() -> {
                                    try {
                                        cacheService.evictConfig(savedConfig.getConfigKey());
                                        logger.info("延迟双删：第二次删除缓存: {}", savedConfig.getConfigKey());
                                    } catch (Exception e) {
                                        logger.error("延迟双删失败: {}", savedConfig.getConfigKey(), e);
                                    }
                                });
                    }
                });
            } else {
                // 非事务环境也使用双删策略
                cacheService.evictConfig(savedConfig.getConfigKey());
                CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                        .execute(() -> {
                            try {
                                cacheService.evictConfig(savedConfig.getConfigKey());
                                logger.info("延迟双删：第二次删除缓存: {}", savedConfig.getConfigKey());
                            } catch (Exception e) {
                                logger.error("延迟双删失败: {}", savedConfig.getConfigKey(), e);
                            }
                        });
            }
            
            logger.info("系统配置创建成功，ID: {}, 配置键: {}", 
                    savedConfig.getId(), savedConfig.getConfigKey());
            
            return Result.success(SystemConfigAssembler.toDto(savedConfig));

        } catch (BizException e) {
            logger.error("创建系统配置失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("创建系统配置异常", e);
            throw BizException.of(ErrorCodeEnum.CONFIG_CREATE_FAILED, "创建系统配置失败");
        }
    }

    /**
     * 更新系统配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<SystemConfigDto> updateConfig(SystemConfigUpdateRequest request) {
        logger.info("更新系统配置，ID: {}", request.getId());

        try {
            // 查询现有配置
            Optional<SystemConfig> existingConfigOpt = systemConfigRepository.findById(request.getId());
            if (!existingConfigOpt.isPresent()) {
                logger.warn("系统配置不存在，ID: {}", request.getId());
                throw BizException.of(ErrorCodeEnum.CONFIG_NOT_FOUND, "系统配置不存在");
            }

            SystemConfig existingConfig = existingConfigOpt.get();

            // 检查是否为只读配置
            if (!existingConfig.isModifiable()) {
                logger.warn("配置为只读，不允许修改，ID: {}", request.getId());
                throw BizException.of(ErrorCodeEnum.CONFIG_READONLY, "配置为只读，不允许修改");
            }

            // 检查配置键是否与其他配置冲突
            if (!existingConfig.getConfigKey().equals(request.getConfigKey()) &&
                systemConfigRepository.existsByKeyExcludeId(request.getConfigKey(), request.getId())) {
                logger.warn("配置键已存在: {}", request.getConfigKey());
                throw BizException.of(ErrorCodeEnum.CONFIG_ALREADY_EXISTS, "配置键已存在");
            }

            // 更新配置
            String oldConfigKey = existingConfig.getConfigKey();
            SystemConfigAssembler.updateDomain(existingConfig, request);
            SystemConfig updatedConfig = systemConfigRepository.update(existingConfig);
            
            // 注册事务同步器，使用延迟双删策略
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 第一次删除
                        cacheService.evictConfig(updatedConfig.getConfigKey());
                        if (!oldConfigKey.equals(updatedConfig.getConfigKey())) {
                            cacheService.evictConfig(oldConfigKey);
                        }
                        logger.info("第一次删除缓存: {}", updatedConfig.getConfigKey());
                        
                        // 延迟双删
                        CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                                .execute(() -> {
                                    try {
                                        cacheService.evictConfig(updatedConfig.getConfigKey());
                                        if (!oldConfigKey.equals(updatedConfig.getConfigKey())) {
                                            cacheService.evictConfig(oldConfigKey);
                                        }
                                        logger.info("延迟双删：第二次删除缓存: {}", updatedConfig.getConfigKey());
                                    } catch (Exception e) {
                                        logger.error("延迟双删失败: {}", updatedConfig.getConfigKey(), e);
                                    }
                                });
                    }
                });
            } else {
                // 非事务环境也使用双删
                cacheService.evictConfig(updatedConfig.getConfigKey());
                if (!oldConfigKey.equals(updatedConfig.getConfigKey())) {
                    cacheService.evictConfig(oldConfigKey);
                }
                CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                        .execute(() -> {
                            try {
                                cacheService.evictConfig(updatedConfig.getConfigKey());
                                if (!oldConfigKey.equals(updatedConfig.getConfigKey())) {
                                    cacheService.evictConfig(oldConfigKey);
                                }
                                logger.info("延迟双删：第二次删除缓存: {}", updatedConfig.getConfigKey());
                            } catch (Exception e) {
                                logger.error("延迟双删失败: {}", updatedConfig.getConfigKey(), e);
                            }
                        });
            }
            
            logger.info("系统配置更新成功，ID: {}, 配置键: {}", 
                    updatedConfig.getId(), updatedConfig.getConfigKey());
            
            return Result.success(SystemConfigAssembler.toDto(updatedConfig));

        } catch (BizException e) {
            logger.error("更新系统配置失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("更新系统配置异常", e);
            throw BizException.of(ErrorCodeEnum.CONFIG_UPDATE_FAILED, "更新系统配置失败");
        }
    }

    /**
     * 删除系统配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteConfig(Long id) {
        logger.info("删除系统配置，ID: {}", id);

        try {
            // 查询配置是否存在
            Optional<SystemConfig> configOpt = systemConfigRepository.findById(id);
            if (!configOpt.isPresent()) {
                logger.warn("系统配置不存在，ID: {}", id);
                throw BizException.of(ErrorCodeEnum.CONFIG_NOT_FOUND, "系统配置不存在");
            }

            SystemConfig config = configOpt.get();

            // 检查是否为只读配置
            if (!config.isModifiable()) {
                logger.warn("配置为只读，不允许删除，ID: {}", id);
                throw BizException.of(ErrorCodeEnum.CONFIG_READONLY, "配置为只读，不允许删除");
            }

            // 删除配置
            String configKey = config.getConfigKey();
            boolean deleted = systemConfigRepository.deleteById(id);
            if (!deleted) {
                logger.error("删除系统配置失败，ID: {}", id);
                throw BizException.of(ErrorCodeEnum.CONFIG_DELETE_FAILED, "删除系统配置失败");
            }
            
            // 注册事务同步器，使用延迟双删策略
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 第一次删除
                        cacheService.evictConfig(configKey);
                        logger.info("第一次删除缓存: {}", configKey);
                        
                        // 延迟双删
                        CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                                .execute(() -> {
                                    try {
                                        cacheService.evictConfig(configKey);
                                        logger.info("延迟双删：第二次删除缓存: {}", configKey);
                                    } catch (Exception e) {
                                        logger.error("延迟双删失败: {}", configKey, e);
                                    }
                                });
                    }
                });
            } else {
                // 非事务环境也使用双删
                cacheService.evictConfig(configKey);
                CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                        .execute(() -> {
                            try {
                                cacheService.evictConfig(configKey);
                                logger.info("延迟双删：第二次删除缓存: {}", configKey);
                            } catch (Exception e) {
                                logger.error("延迟双删失败: {}", configKey, e);
                            }
                        });
            }
            
            logger.info("系统配置删除成功，ID: {}, 配置键: {}", id, configKey);
            
            return Result.success();

        } catch (BizException e) {
            logger.error("删除系统配置失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("删除系统配置异常", e);
            throw BizException.of(ErrorCodeEnum.CONFIG_DELETE_FAILED, "删除系统配置失败");
        }
    }

    /**
     * 根据ID查询系统配置
     */
    public Result<SystemConfigDto> getConfigById(Long id) {
        logger.info("查询系统配置，ID: {}", id);

        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findById(id);
            if (!configOpt.isPresent()) {
                logger.warn("系统配置不存在，ID: {}", id);
                throw BizException.of(ErrorCodeEnum.CONFIG_NOT_FOUND, "系统配置不存在");
            }

            return Result.success(SystemConfigAssembler.toDto(configOpt.get()));

        } catch (BizException e) {
            logger.error("查询系统配置失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("查询系统配置异常", e);
            throw BizException.of(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "查询系统配置失败");
        }
    }

    /**
     * 根据配置键查询系统配置
     */
    public Result<SystemConfigDto> getConfigByKey(String configKey) {
        logger.info("根据配置键查询系统配置: {}", configKey);

        try {
            Optional<SystemConfig> configOpt = systemConfigRepository.findByKey(configKey);
            if (!configOpt.isPresent()) {
                logger.warn("系统配置不存在，配置键: {}", configKey);
                throw BizException.of(ErrorCodeEnum.CONFIG_NOT_FOUND, "系统配置不存在");
            }

            return Result.success(SystemConfigAssembler.toDto(configOpt.get()));

        } catch (BizException e) {
            logger.error("查询系统配置失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("查询系统配置异常", e);
            throw BizException.of(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "查询系统配置失败");
        }
    }

    /**
     * 分页查询系统配置
     */
    public Result<Map<String, Object>> queryConfigs(SystemConfigQueryRequest request) {
        logger.info("分页查询系统配置，页码: {}, 每页大小: {}", request.getPage(), request.getSize());

        try {
            // 查询配置列表
            List<SystemConfig> configs = systemConfigRepository.findByCondition(
                    request.getConfigKey(),
                    request.getConfigGroup(),
                    request.getEnabled(),
                    request.getPage(),
                    request.getSize()
            );

            // 查询总数
            long total = systemConfigRepository.countByCondition(
                    request.getConfigKey(),
                    request.getConfigGroup(),
                    request.getEnabled()
            );

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("list", SystemConfigAssembler.toDtoList(configs));
            result.put("total", total);
            result.put("page", request.getPage());
            result.put("size", request.getSize());
            result.put("pages", (total + request.getSize() - 1) / request.getSize());

            return Result.success(result);

        } catch (Exception e) {
            logger.error("查询系统配置异常", e);
            throw BizException.of(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "查询系统配置失败");
        }
    }

    /**
     * 根据分组查询配置列表
     */
    public Result<List<SystemConfigDto>> getConfigsByGroup(String configGroup) {
        logger.info("根据分组查询配置列表: {}", configGroup);

        try {
            List<SystemConfig> configs = systemConfigRepository.findByGroup(configGroup);
            return Result.success(SystemConfigAssembler.toDtoList(configs));

        } catch (Exception e) {
            logger.error("查询配置列表异常", e);
            throw BizException.of(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "查询配置列表失败");
        }
    }

    /**
     * 查询所有启用的配置
     */
    public Result<List<SystemConfigDto>> getAllEnabledConfigs() {
        logger.info("查询所有启用的配置");

        try {
            List<SystemConfig> configs = systemConfigRepository.findAllEnabled();
            return Result.success(SystemConfigAssembler.toDtoList(configs));

        } catch (Exception e) {
            logger.error("查询启用配置异常", e);
            throw BizException.of(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "查询启用配置失败");
        }
    }

    /**
     * 批量更新配置状态
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchUpdateStatus(List<Long> ids, Boolean enabled) {
        logger.info("批量更新配置状态，配置数量: {}, 状态: {}", ids.size(), enabled);

        try {
            // 获取所有受影响的配置键
            Set<String> affectedKeys = new HashSet<>();
            for (Long id : ids) {
                Optional<SystemConfig> configOpt = systemConfigRepository.findById(id);
                configOpt.ifPresent(config -> affectedKeys.add(config.getConfigKey()));
            }
            
            int updatedCount = systemConfigRepository.batchUpdateStatus(ids, enabled);
            
            // 注册事务同步器，使用延迟双删策略
            if (!affectedKeys.isEmpty()) {
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            // 第一次删除
                            cacheService.evictConfigs(affectedKeys);
                            logger.info("第一次批量删除缓存: {}", affectedKeys);
                            
                            // 延迟双删
                            CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                                    .execute(() -> {
                                        try {
                                            cacheService.evictConfigs(affectedKeys);
                                            logger.info("延迟双删：第二次批量删除缓存: {}", affectedKeys);
                                        } catch (Exception e) {
                                            logger.error("延迟双删失败", e);
                                        }
                                    });
                        }
                    });
                } else {
                    // 非事务环境也使用双删
                    cacheService.evictConfigs(affectedKeys);
                    CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                            .execute(() -> {
                                try {
                                    cacheService.evictConfigs(affectedKeys);
                                    logger.info("延迟双删：第二次批量删除缓存: {}", affectedKeys);
                                } catch (Exception e) {
                                    logger.error("延迟双删失败", e);
                                }
                            });
                }
                logger.info("批量更新配置状态成功，更新数量: {}", updatedCount);
            }
            
            return Result.success();

        } catch (Exception e) {
            logger.error("批量更新配置状态异常", e);
            throw BizException.of(ErrorCodeEnum.CONFIG_UPDATE_FAILED, "批量更新配置状态失败");
        }
    }
    
    /**
     * 刷新配置缓存
     * 
     * @param configKeys 配置键列表，为空则刷新全部
     */
    public Result<Void> refreshCache(List<String> configKeys) {
        logger.info("刷新配置缓存，配置键数量: {}", 
                configKeys == null ? "全部" : configKeys.size());
        
        try {
            if (configKeys == null || configKeys.isEmpty()) {
                // 刷新全部
                cacheService.evictAllConfigs();
                cacheService.warmUpCache();
                logger.info("已刷新全部配置缓存");
            } else {
                // 刷新指定配置
                cacheService.evictConfigs(new HashSet<>(configKeys));
                logger.info("已刷新指定配置缓存: {}", configKeys);
            }
            
            return Result.success();
            
        } catch (Exception e) {
            logger.error("刷新配置缓存失败", e);
            throw BizException.of(ErrorCodeEnum.CONFIG_UPDATE_FAILED, "刷新配置缓存失败");
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public Result<Map<String, Object>> getCacheStats() {
        try {
            Map<String, Object> stats = cacheService.getCacheStats();
            return Result.success(stats);
        } catch (Exception e) {
            logger.error("获取缓存统计信息失败", e);
            throw BizException.of(ErrorCodeEnum.SYSTEM_ERROR, "获取缓存统计信息失败");
        }
    }
}
