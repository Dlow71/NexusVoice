package com.nexusvoice.infrastructure.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.domain.config.model.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置Mapper接口
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键查询配置
     * 
     * @param configKey 配置键
     * @return 配置信息
     */
    SystemConfig selectByKey(@Param("configKey") String configKey);

    /**
     * 根据配置分组查询配置列表
     * 
     * @param configGroup 配置分组
     * @return 配置列表
     */
    List<SystemConfig> selectByGroup(@Param("configGroup") String configGroup);

    /**
     * 查询所有启用的配置
     * 
     * @return 启用的配置列表
     */
    List<SystemConfig> selectAllEnabled();

    /**
     * 根据条件查询配置列表（分页）
     * 
     * @param configKey 配置键（模糊查询）
     * @param configGroup 配置分组
     * @param enabled 是否启用
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 配置列表
     */
    List<SystemConfig> selectByCondition(
            @Param("configKey") String configKey,
            @Param("configGroup") String configGroup,
            @Param("enabled") Boolean enabled,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 根据条件统计配置数量
     * 
     * @param configKey 配置键（模糊查询）
     * @param configGroup 配置分组
     * @param enabled 是否启用
     * @return 配置数量
     */
    long countByCondition(
            @Param("configKey") String configKey,
            @Param("configGroup") String configGroup,
            @Param("enabled") Boolean enabled
    );

    /**
     * 检查配置键是否存在
     * 
     * @param configKey 配置键
     * @return 存在的数量
     */
    int countByKey(@Param("configKey") String configKey);

    /**
     * 检查配置键是否存在（排除指定ID）
     * 
     * @param configKey 配置键
     * @param excludeId 排除的ID
     * @return 存在的数量
     */
    int countByKeyExcludeId(@Param("configKey") String configKey, @Param("excludeId") Long excludeId);

    /**
     * 批量更新配置状态
     * 
     * @param ids 配置ID列表
     * @param enabled 是否启用
     * @return 更新成功的数量
     */
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled);
}
