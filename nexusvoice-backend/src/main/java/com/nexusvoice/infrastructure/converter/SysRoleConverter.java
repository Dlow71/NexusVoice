package com.nexusvoice.infrastructure.converter;

import com.nexusvoice.domain.rbac.model.SysRole;
import com.nexusvoice.infrastructure.persistence.po.SysRolePO;

/**
 * 系统角色转换器
 * 负责领域实体和持久化对象之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
public class SysRoleConverter {

    /**
     * PO转领域实体
     *
     * @param po 持久化对象
     * @return 领域实体
     */
    public static SysRole toDomain(SysRolePO po) {
        if (po == null) {
            return null;
        }

        SysRole sysRole = new SysRole();
        sysRole.setId(po.getId());
        sysRole.setRoleCode(po.getRoleCode());
        sysRole.setRoleName(po.getRoleName());
        sysRole.setDescription(po.getDescription());
        sysRole.setSortOrder(po.getSortOrder());
        sysRole.setStatus(po.getStatus());
        sysRole.setIsSystem(po.getIsSystem());
        sysRole.setCreatedAt(po.getCreatedAt());
        sysRole.setUpdatedAt(po.getUpdatedAt());

        return sysRole;
    }

    /**
     * 领域实体转PO
     *
     * @param sysRole 领域实体
     * @return 持久化对象
     */
    public static SysRolePO toPO(SysRole sysRole) {
        if (sysRole == null) {
            return null;
        }

        SysRolePO po = new SysRolePO();
        po.setId(sysRole.getId());
        po.setRoleCode(sysRole.getRoleCode());
        po.setRoleName(sysRole.getRoleName());
        po.setDescription(sysRole.getDescription());
        po.setSortOrder(sysRole.getSortOrder());
        po.setStatus(sysRole.getStatus());
        po.setIsSystem(sysRole.getIsSystem());
        po.setCreatedAt(sysRole.getCreatedAt());
        po.setUpdatedAt(sysRole.getUpdatedAt());

        return po;
    }
}
