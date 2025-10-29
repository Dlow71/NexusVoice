package com.nexusvoice.domain.rbac.repository;

import java.util.List;

/**
 * 角色菜单关联仓储接口
 * 纯接口定义，不包含任何技术实现
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
public interface RoleMenuRepository {

    /**
     * 根据角色ID查询菜单ID列表
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    List<Long> findMenuIdsByRoleId(Long roleId);

    /**
     * 根据菜单ID查询角色ID列表
     *
     * @param menuId 菜单ID
     * @return 角色ID列表
     */
    List<Long> findRoleIdsByMenuId(Long menuId);

    /**
     * 保存角色菜单关联
     *
     * @param roleId 角色ID
     * @param menuId 菜单ID
     */
    void save(Long roleId, Long menuId);

    /**
     * 批量保存角色菜单关联
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     */
    void saveBatch(Long roleId, List<Long> menuIds);

    /**
     * 删除角色菜单关联
     *
     * @param roleId 角色ID
     * @param menuId 菜单ID
     */
    void delete(Long roleId, Long menuId);

    /**
     * 删除角色的所有菜单关联
     *
     * @param roleId 角色ID
     */
    void deleteByRoleId(Long roleId);

    /**
     * 删除菜单的所有角色关联
     *
     * @param menuId 菜单ID
     */
    void deleteByMenuId(Long menuId);

    /**
     * 判断角色是否拥有指定菜单
     *
     * @param roleId 角色ID
     * @param menuId 菜单ID
     * @return true-拥有, false-不拥有
     */
    boolean exists(Long roleId, Long menuId);

    /**
     * 统计拥有指定菜单的角色数量
     *
     * @param menuId 菜单ID
     * @return 角色数量
     */
    long countRolesByMenuId(Long menuId);
}
