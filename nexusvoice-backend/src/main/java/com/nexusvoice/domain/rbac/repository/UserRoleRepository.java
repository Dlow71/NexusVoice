package com.nexusvoice.domain.rbac.repository;

import java.util.List;

/**
 * 用户角色关联仓储接口
 * 纯接口定义，不包含任何技术实现
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
public interface UserRoleRepository {

    /**
     * 根据用户ID查询角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> findRoleIdsByUserId(Long userId);

    /**
     * 根据角色ID查询用户ID列表
     *
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    List<Long> findUserIdsByRoleId(Long roleId);

    /**
     * 保存用户角色关联
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     */
    void save(Long userId, Long roleId);

    /**
     * 批量保存用户角色关联
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     */
    void saveBatch(Long userId, List<Long> roleIds);

    /**
     * 删除用户角色关联
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     */
    void delete(Long userId, Long roleId);

    /**
     * 删除用户的所有角色关联
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);

    /**
     * 删除角色的所有用户关联
     *
     * @param roleId 角色ID
     */
    void deleteByRoleId(Long roleId);

    /**
     * 判断用户是否拥有指定角色
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return true-拥有, false-不拥有
     */
    boolean exists(Long userId, Long roleId);

    /**
     * 统计拥有指定角色的用户数量
     *
     * @param roleId 角色ID
     * @return 用户数量
     */
    long countUsersByRoleId(Long roleId);
}
