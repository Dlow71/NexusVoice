package com.nexusvoice.domain.config.repository;

import com.nexusvoice.domain.config.model.SystemConfig;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置仓储接口
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
public interface SystemConfigRepository {

    /**
     * 根据ID查询配置
     * 
     * @param id 配置ID
     * @return 配置信息
     */
    Optional<SystemConfig> findById(Long id);

    /**
     * 根据配置键查询配置
     * 
     * @param configKey 配置键
     * @return 配置信息
     */
    Optional<SystemConfig> findByKey(String configKey);

    /**
     * 根据配置分组查询配置列表
     * 
     * @param configGroup 配置分组
     * @return 配置列表
     */
    List<SystemConfig> findByGroup(String configGroup);

    /**
     * 查询所有启用的配置
     * 
     * @return 启用的配置列表
     */
    List<SystemConfig> findAllEnabled();

    /**
     * 查询所有配置（分页）
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 配置列表
     */
    List<SystemConfig> findAll(int page, int size);

    /**
     * 根据条件查询配置列表
     * 
     * @param configKey 配置键（模糊查询）
     * @param configGroup 配置分组
     * @param enabled 是否启用
     * @param page 页码
     * @param size 每页大小
     * @return 配置列表
     */
    List<SystemConfig> findByCondition(String configKey, String configGroup, Boolean enabled, int page, int size);

    /**
     * 统计配置总数
     * 
     * @return 配置总数
     */
    long count();

    /**
     * 根据条件统计配置数量
     * 
     * @param configKey 配置键（模糊查询）
     * @param configGroup 配置分组
     * @param enabled 是否启用
     * @return 配置数量
     */
    long countByCondition(String configKey, String configGroup, Boolean enabled);

    /**
     * 保存配置
     * 
     * @param systemConfig 配置信息
     * @return 保存后的配置信息
     */
    SystemConfig save(SystemConfig systemConfig);

    /**
     * 更新配置
     * 
     * @param systemConfig 配置信息
     * @return 更新后的配置信息
     */
    SystemConfig update(SystemConfig systemConfig);

    /**
     * 根据ID删除配置
     * 
     * @param id 配置ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据配置键删除配置
     * 
     * @param configKey 配置键
     * @return 是否删除成功
     */
    boolean deleteByKey(String configKey);

    /**
     * 检查配置键是否存在
     * 
     * @param configKey 配置键
     * @return 是否存在
     */
    boolean existsByKey(String configKey);

    /**
     * 检查配置键是否存在（排除指定ID）
     * 
     * @param configKey 配置键
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean existsByKeyExcludeId(String configKey, Long excludeId);

    /**
     * 批量保存配置
     * 
     * @param systemConfigs 配置列表
     * @return 保存成功的数量
     */
    int batchSave(List<SystemConfig> systemConfigs);

    /**
     * 批量更新配置状态
     * 
     * @param ids 配置ID列表
     * @param enabled 是否启用
     * @return 更新成功的数量
     */
    int batchUpdateStatus(List<Long> ids, Boolean enabled);
}
