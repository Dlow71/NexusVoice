package com.nexusvoice.domain.rbac.repository;

import com.nexusvoice.domain.rbac.model.SysRole;

import java.util.List;
import java.util.Optional;

/**
 * 系统角色仓储接口
 * 纯接口定义，不包含任何技术实现
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
public interface SysRoleRepository {

    /**
     * 根据ID查询角色
     *
     * @param id 角色ID
     * @return 角色实体
     */
    Optional<SysRole> findById(Long id);

    /**
     * 根据角色编码查询角色
     *
     * @param roleCode 角色编码
     * @return 角色实体
     */
    Optional<SysRole> findByRoleCode(String roleCode);

    /**
     * 根据用户ID查询用户的所有角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> findByUserId(Long userId);

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    List<SysRole> findAll();

    /**
     * 查询所有启用的角色
     *
     * @return 角色列表
     */
    List<SysRole> findAllEnabled();

    /**
     * 保存角色
     *
     * @param role 角色实体
     * @return 保存后的角色实体
     */
    SysRole save(SysRole role);

    /**
     * 更新角色
     *
     * @param role 角色实体
     * @return 更新后的角色实体
     */
    SysRole update(SysRole role);

    /**
     * 根据ID删除角色（逻辑删除）
     *
     * @param id 角色ID
     */
    void deleteById(Long id);

    /**
     * 判断角色编码是否存在
     *
     * @param roleCode 角色编码
     * @return true-存在, false-不存在
     */
    boolean existsByRoleCode(String roleCode);

    /**
     * 判断角色编码是否存在（排除指定ID）
     *
     * @param roleCode 角色编码
     * @param excludeId 排除的角色ID
     * @return true-存在, false-不存在
     */
    boolean existsByRoleCodeExcludeId(String roleCode, Long excludeId);

    /**
     * 统计角色数量
     *
     * @return 角色总数
     */
    long count();
}
