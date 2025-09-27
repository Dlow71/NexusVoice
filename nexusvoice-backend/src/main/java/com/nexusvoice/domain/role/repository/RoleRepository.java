package com.nexusvoice.domain.role.repository;

import com.nexusvoice.domain.role.model.Role;
import com.nexusvoice.application.user.dto.PageResult;

import java.util.Optional;

/**
 * 角色仓储接口
 * 定义AI角色的数据访问领域接口
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
public interface RoleRepository {

    /**
     * 根据ID查找角色
     */
    Optional<Role> findById(Long id);

    /**
     * 保存角色（新增）
     */
    Role save(Role role);

    /**
     * 更新角色
     */
    Role update(Role role);

    /**
     * 根据ID删除（逻辑删除）
     */
    void deleteById(Long id);

    /**
     * 分页查询公共角色
     */
    PageResult<Role> pagePublicRoles(Integer page, Integer size, String keyword);

    /**
     * 分页查询所有用户的私人角色（管理端）
     * @param userId 可选的用户ID过滤
     */
    PageResult<Role> pageAllPrivateRoles(Integer page, Integer size, String keyword, Long userId);

    /**
     * 分页查询当前用户的私人角色（用户端）
     */
    PageResult<Role> pageUserPrivateRoles(Integer page, Integer size, String keyword, Long ownerUserId);
}
