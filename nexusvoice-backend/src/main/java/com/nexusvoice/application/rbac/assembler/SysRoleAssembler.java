package com.nexusvoice.application.rbac.assembler;

import com.nexusvoice.application.rbac.dto.SysRoleDTO;
import com.nexusvoice.application.rbac.dto.request.RoleCreateRequest;
import com.nexusvoice.application.rbac.dto.request.RoleUpdateRequest;
import com.nexusvoice.domain.rbac.model.SysRole;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统角色装配器
 * 负责DTO和领域实体之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
public class SysRoleAssembler {

    /**
     * 领域实体转DTO
     *
     * @param sysRole 领域实体
     * @return DTO
     */
    public static SysRoleDTO toDTO(SysRole sysRole) {
        if (sysRole == null) {
            return null;
        }

        SysRoleDTO dto = new SysRoleDTO();
        dto.setId(sysRole.getId());
        dto.setRoleCode(sysRole.getRoleCode());
        dto.setRoleName(sysRole.getRoleName());
        dto.setDescription(sysRole.getDescription());
        dto.setSortOrder(sysRole.getSortOrder());
        dto.setStatus(sysRole.getStatus());
        dto.setIsSystem(sysRole.getIsSystem());
        dto.setCreatedAt(sysRole.getCreatedAt());
        dto.setUpdatedAt(sysRole.getUpdatedAt());

        return dto;
    }

    /**
     * 领域实体列表转DTO列表
     *
     * @param roles 领域实体列表
     * @return DTO列表
     */
    public static List<SysRoleDTO> toDTOList(List<SysRole> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(SysRoleAssembler::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建请求转领域实体
     *
     * @param request 创建请求
     * @return 领域实体
     */
    public static SysRole toDomain(RoleCreateRequest request) {
        if (request == null) {
            return null;
        }

        SysRole sysRole = new SysRole();
        sysRole.setRoleCode(request.getRoleCode());
        sysRole.setRoleName(request.getRoleName());
        sysRole.setDescription(request.getDescription());
        sysRole.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        sysRole.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        sysRole.setIsSystem(false);  // 新创建的角色默认不是系统角色

        return sysRole;
    }

    /**
     * 更新请求更新领域实体
     *
     * @param sysRole 领域实体
     * @param request 更新请求
     */
    public static void updateDomain(SysRole sysRole, RoleUpdateRequest request) {
        if (sysRole == null || request == null) {
            return;
        }

        if (request.getRoleName() != null) {
            sysRole.setRoleName(request.getRoleName());
        }
        if (request.getDescription() != null) {
            sysRole.setDescription(request.getDescription());
        }
        if (request.getSortOrder() != null) {
            sysRole.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            sysRole.setStatus(request.getStatus());
        }
    }
}
