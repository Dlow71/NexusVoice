package com.nexusvoice.domain.rbac.model;

import com.nexusvoice.domain.common.BaseDomainEntity;

import java.util.Objects;

/**
 * 系统角色领域实体
 * 纯净的领域模型，不包含任何技术框架注解
 * 注意：区别于AI聊天角色（roles表），这是系统权限角色
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
public class SysRole extends BaseDomainEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 角色编码（全局唯一，用于代码中判断）
     */
    private String roleCode;

    /**
     * 角色名称（用于界面显示）
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 排序（数字越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    /**
     * 是否系统内置角色：0-否 1-是
     * 系统内置角色不允许删除
     */
    private Boolean isSystem;

    // 构造函数
    public SysRole() {
        this.status = 1;
        this.sortOrder = 0;
        this.isSystem = false;
    }

    public SysRole(String roleCode, String roleName) {
        this();
        this.roleCode = roleCode;
        this.roleName = roleName;
    }

    // 业务方法

    /**
     * 判断角色是否启用
     *
     * @return true-启用, false-禁用
     */
    public boolean isEnabled() {
        return Integer.valueOf(1).equals(this.status);
    }

    /**
     * 判断角色是否禁用
     *
     * @return true-禁用, false-启用
     */
    public boolean isDisabled() {
        return !isEnabled();
    }

    /**
     * 启用角色
     */
    public void enable() {
        this.status = 1;
    }

    /**
     * 禁用角色
     */
    public void disable() {
        this.status = 0;
    }

    /**
     * 判断是否可以删除
     * 系统内置角色不允许删除
     *
     * @return true-可删除, false-不可删除
     */
    public boolean canDelete() {
        return !Boolean.TRUE.equals(this.isSystem);
    }

    /**
     * 判断是否为系统内置角色
     *
     * @return true-系统内置, false-非系统内置
     */
    public boolean isSystemRole() {
        return Boolean.TRUE.equals(this.isSystem);
    }

    /**
     * 判断是否为超级管理员角色
     *
     * @return true-超级管理员, false-其他
     */
    public boolean isSuperAdmin() {
        return "super_admin".equals(this.roleCode);
    }

    /**
     * 判断是否为管理员角色
     *
     * @return true-管理员, false-其他
     */
    public boolean isAdmin() {
        return "admin".equals(this.roleCode) || isSuperAdmin();
    }

    /**
     * 验证角色编码格式
     * 格式要求：小写字母、数字、下划线，3-50个字符
     *
     * @throws IllegalArgumentException 格式不正确时抛出
     */
    public void validateRoleCode() {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            throw new IllegalArgumentException("角色编码不能为空");
        }
        if (!roleCode.matches("^[a-z0-9_]{3,50}$")) {
            throw new IllegalArgumentException("角色编码格式不正确：只能包含小写字母、数字、下划线，长度3-50");
        }
    }

    // Getter and Setter methods

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysRole sysRole = (SysRole) o;
        return Objects.equals(id, sysRole.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SysRole{" +
                "id=" + id +
                ", roleCode='" + roleCode + '\'' +
                ", roleName='" + roleName + '\'' +
                ", status=" + status +
                ", isSystem=" + isSystem +
                ", createdAt=" + createdAt +
                '}';
    }
}
