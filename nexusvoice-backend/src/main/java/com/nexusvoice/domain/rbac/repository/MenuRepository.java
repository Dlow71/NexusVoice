package com.nexusvoice.domain.rbac.repository;

import com.nexusvoice.domain.rbac.constant.MenuType;
import com.nexusvoice.domain.rbac.model.Menu;

import java.util.List;
import java.util.Optional;

/**
 * 菜单仓储接口
 * 纯接口定义，不包含任何技术实现
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
public interface MenuRepository {

    /**
     * 根据ID查询菜单
     *
     * @param id 菜单ID
     * @return 菜单实体
     */
    Optional<Menu> findById(Long id);

    /**
     * 根据名称查询菜单
     *
     * @param name 菜单名称
     * @return 菜单实体
     */
    Optional<Menu> findByName(String name);

    /**
     * 根据角色ID查询菜单列表
     *
     * @param roleId 角色ID
     * @return 菜单列表
     */
    List<Menu> findByRoleId(Long roleId);

    /**
     * 根据角色ID列表查询菜单列表（去重）
     *
     * @param roleIds 角色ID列表
     * @return 菜单列表
     */
    List<Menu> findByRoleIds(List<Long> roleIds);

    /**
     * 根据用户ID查询用户有权限的菜单列表
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<Menu> findByUserId(Long userId);

    /**
     * 根据父菜单ID查询子菜单列表
     *
     * @param parentId 父菜单ID
     * @return 菜单列表
     */
    List<Menu> findByParentId(Long parentId);

    /**
     * 查询所有菜单
     *
     * @return 菜单列表
     */
    List<Menu> findAll();

    /**
     * 查询所有启用的菜单
     *
     * @return 菜单列表
     */
    List<Menu> findAllEnabled();

    /**
     * 根据菜单类型查询菜单列表
     *
     * @param menuType 菜单类型
     * @return 菜单列表
     */
    List<Menu> findByMenuType(MenuType menuType);

    /**
     * 查询所有根菜单（parent_id = 0）
     *
     * @return 菜单列表
     */
    List<Menu> findRootMenus();

    /**
     * 保存菜单
     *
     * @param menu 菜单实体
     * @return 保存后的菜单实体
     */
    Menu save(Menu menu);

    /**
     * 更新菜单
     *
     * @param menu 菜单实体
     * @return 更新后的菜单实体
     */
    Menu update(Menu menu);

    /**
     * 根据ID删除菜单（逻辑删除）
     *
     * @param id 菜单ID
     */
    void deleteById(Long id);

    /**
     * 判断菜单名称是否存在
     *
     * @param name 菜单名称
     * @return true-存在, false-不存在
     */
    boolean existsByName(String name);

    /**
     * 判断菜单名称是否存在（排除指定ID）
     *
     * @param name 菜单名称
     * @param excludeId 排除的菜单ID
     * @return true-存在, false-不存在
     */
    boolean existsByNameExcludeId(String name, Long excludeId);

    /**
     * 判断菜单是否有子菜单
     *
     * @param menuId 菜单ID
     * @return true-有子菜单, false-无子菜单
     */
    boolean hasChildren(Long menuId);

    /**
     * 统计菜单数量
     *
     * @return 菜单总数
     */
    long count();

    /**
     * 根据权限标识查询菜单
     *
     * @param permission 权限标识
     * @return 菜单实体
     */
    Optional<Menu> findByPermission(String permission);
}
