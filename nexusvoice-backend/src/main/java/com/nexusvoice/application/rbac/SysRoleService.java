package com.nexusvoice.application.rbac;

import com.nexusvoice.application.rbac.assembler.SysRoleAssembler;
import com.nexusvoice.application.rbac.dto.SysRoleDTO;
import com.nexusvoice.application.rbac.dto.request.RoleCreateRequest;
import com.nexusvoice.application.rbac.dto.request.RoleUpdateRequest;
import com.nexusvoice.domain.rbac.model.SysRole;
import com.nexusvoice.domain.rbac.repository.RoleMenuRepository;
import com.nexusvoice.domain.rbac.repository.SysRoleRepository;
import com.nexusvoice.domain.rbac.repository.UserRoleRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.security.PermissionChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统角色应用服务
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Slf4j
@Service
public class SysRoleService {

    @Autowired
    private SysRoleRepository sysRoleRepository;

    @Autowired
    private RoleMenuRepository roleMenuRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PermissionChecker permissionChecker;

    /**
     * 创建角色
     *
     * @param request 创建请求
     * @return 角色DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public SysRoleDTO createRole(RoleCreateRequest request) {
        log.info("创建角色，roleCode：{}, roleName：{}", request.getRoleCode(), request.getRoleName());

        // 1. 验证角色编码是否已存在
        if (sysRoleRepository.existsByRoleCode(request.getRoleCode())) {
            throw new BizException(ErrorCodeEnum.ROLE_CODE_ALREADY_EXISTS);
        }

        // 2. 转换为领域实体并验证
        SysRole sysRole = SysRoleAssembler.toDomain(request);
        sysRole.validateRoleCode();

        // 3. 保存角色
        sysRole = sysRoleRepository.save(sysRole);

        // 4. 分配菜单权限
        if (request.getMenuIds() != null && !request.getMenuIds().isEmpty()) {
            roleMenuRepository.saveBatch(sysRole.getId(), request.getMenuIds());
        }

        log.info("角色创建成功，roleId：{}", sysRole.getId());
        
        SysRoleDTO dto = SysRoleAssembler.toDTO(sysRole);
        dto.setMenuIds(request.getMenuIds());
        return dto;
    }

    /**
     * 更新角色
     *
     * @param request 更新请求
     * @return 角色DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public SysRoleDTO updateRole(RoleUpdateRequest request) {
        log.info("更新角色，roleId：{}", request.getId());

        // 1. 查询角色
        SysRole sysRole = sysRoleRepository.findById(request.getId())
                .orElseThrow(() -> new BizException(ErrorCodeEnum.ROLE_NOT_FOUND));

        // 2. 检查是否为系统角色
        if (sysRole.isSystemRole()) {
            throw new BizException(ErrorCodeEnum.ROLE_SYSTEM_CANNOT_EDIT);
        }

        // 3. 更新角色信息
        SysRoleAssembler.updateDomain(sysRole, request);
        sysRole = sysRoleRepository.update(sysRole);

        // 4. 更新菜单权限
        if (request.getMenuIds() != null) {
            roleMenuRepository.saveBatch(sysRole.getId(), request.getMenuIds());
        }

        // 5. 清除所有拥有该角色的用户权限缓存
        List<Long> userIds = userRoleRepository.findUserIdsByRoleId(sysRole.getId());
        if (!userIds.isEmpty()) {
            userIds.forEach(permissionChecker::evictUserPermissionCache);
            log.info("已清除{}个用户的权限缓存，roleId：{}", userIds.size(), sysRole.getId());
        }

        log.info("角色更新成功，roleId：{}", sysRole.getId());
        
        SysRoleDTO dto = SysRoleAssembler.toDTO(sysRole);
        dto.setMenuIds(request.getMenuIds());
        return dto;
    }

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        log.info("删除角色，roleId：{}", roleId);

        // 1. 查询角色
        SysRole sysRole = sysRoleRepository.findById(roleId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.ROLE_NOT_FOUND));

        // 2. 检查是否可以删除
        if (!sysRole.canDelete()) {
            throw new BizException(ErrorCodeEnum.ROLE_SYSTEM_CANNOT_DELETE);
        }

        // 3. 检查是否有用户使用该角色
        long userCount = userRoleRepository.countUsersByRoleId(roleId);
        if (userCount > 0) {
            throw new BizException(ErrorCodeEnum.ROLE_HAS_USERS);
        }

        // 4. 获取拥有该角色的用户（删除前）
        List<Long> userIds = userRoleRepository.findUserIdsByRoleId(roleId);

        // 5. 删除角色菜单关联
        roleMenuRepository.deleteByRoleId(roleId);

        // 6. 删除角色
        sysRoleRepository.deleteById(roleId);

        // 7. 清除用户权限缓存
        if (!userIds.isEmpty()) {
            userIds.forEach(permissionChecker::evictUserPermissionCache);
            log.info("已清除{}个用户的权限缓存，roleId：{}", userIds.size(), roleId);
        }

        log.info("角色删除成功，roleId：{}", roleId);
    }

    /**
     * 查询角色详情
     *
     * @param roleId 角色ID
     * @return 角色DTO
     */
    public SysRoleDTO getRoleById(Long roleId) {
        SysRole sysRole = sysRoleRepository.findById(roleId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.ROLE_NOT_FOUND));

        SysRoleDTO dto = SysRoleAssembler.toDTO(sysRole);
        
        // 查询角色的菜单权限
        List<Long> menuIds = roleMenuRepository.findMenuIdsByRoleId(roleId);
        dto.setMenuIds(menuIds);

        return dto;
    }

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    public List<SysRoleDTO> listRoles() {
        List<SysRole> roles = sysRoleRepository.findAll();
        return SysRoleAssembler.toDTOList(roles);
    }

    /**
     * 查询用户的角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    public List<SysRoleDTO> listUserRoles(Long userId) {
        List<SysRole> roles = sysRoleRepository.findByUserId(userId);
        return SysRoleAssembler.toDTOList(roles);
    }

    /**
     * 为用户分配角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        log.info("为用户分配角色，userId：{}, roleIds：{}", userId, roleIds);
        
        // 验证角色是否存在
        for (Long roleId : roleIds) {
            sysRoleRepository.findById(roleId)
                    .orElseThrow(() -> new BizException(ErrorCodeEnum.ROLE_NOT_FOUND));
        }

        // 批量保存用户角色关联
        userRoleRepository.saveBatch(userId, roleIds);
        
        // 清除用户权限缓存
        permissionChecker.evictUserPermissionCache(userId);
        log.info("已清除用户权限缓存，userId：{}", userId);
        
        log.info("用户角色分配成功，userId：{}", userId);
    }
}
