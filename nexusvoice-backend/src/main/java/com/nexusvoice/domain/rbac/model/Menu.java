package com.nexusvoice.domain.rbac.model;

import com.nexusvoice.domain.common.BaseDomainEntity;
import com.nexusvoice.domain.rbac.constant.MenuType;

import java.util.Objects;

/**
 * 菜单领域实体
 * 纯净的领域模型，不包含任何技术框架注解
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
public class Menu extends BaseDomainEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 父菜单ID，0表示根菜单
     */
    private Long parentId;

    /**
     * 菜单类型
     */
    private MenuType menuType;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 路由名称（唯一）
     */
    private String name;

    /**
     * 菜单标题（显示文本）
     */
    private String title;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 权限标识
     */
    private String permission;

    /**
     * 排序（同级菜单排序）
     */
    private Integer sortOrder;

    /**
     * 是否可见：0-隐藏 1-显示
     */
    private Integer visible;

    /**
     * 菜单状态：0-禁用 1-启用
     */
    private Integer status;

    /**
     * 是否缓存：0-否 1-是
     */
    private Integer keepAlive;

    /**
     * 外部链接地址
     */
    private String externalLink;

    // 构造函数
    public Menu() {
        this.parentId = 0L;
        this.sortOrder = 0;
        this.visible = 1;
        this.status = 1;
        this.keepAlive = 0;
    }

    public Menu(String name, String title, MenuType menuType) {
        this();
        this.name = name;
        this.title = title;
        this.menuType = menuType;
    }

    // 业务方法

    /**
     * 判断是否为根菜单
     *
     * @return true-根菜单, false-子菜单
     */
    public boolean isRootMenu() {
        return this.parentId == null || this.parentId == 0L;
    }

    /**
     * 判断是否为目录
     *
     * @return true-目录, false-其他
     */
    public boolean isDirectory() {
        return this.menuType != null && this.menuType.isDirectory();
    }

    /**
     * 判断是否为菜单
     *
     * @return true-菜单, false-其他
     */
    public boolean isMenu() {
        return this.menuType != null && this.menuType.isMenu();
    }

    /**
     * 判断是否为按钮
     *
     * @return true-按钮, false-其他
     */
    public boolean isButton() {
        return this.menuType != null && this.menuType.isButton();
    }

    /**
     * 判断是否可见
     *
     * @return true-可见, false-隐藏
     */
    public boolean isVisible() {
        return Integer.valueOf(1).equals(this.visible);
    }

    /**
     * 判断是否启用
     *
     * @return true-启用, false-禁用
     */
    public boolean isEnabled() {
        return Integer.valueOf(1).equals(this.status);
    }

    /**
     * 判断是否需要缓存
     *
     * @return true-缓存, false-不缓存
     */
    public boolean isKeepAlive() {
        return Integer.valueOf(1).equals(this.keepAlive);
    }

    /**
     * 判断是否为外部链接
     *
     * @return true-外部链接, false-内部菜单
     */
    public boolean isExternalLink() {
        return this.externalLink != null && !this.externalLink.trim().isEmpty();
    }

    /**
     * 判断是否有权限标识
     *
     * @return true-有权限标识, false-无权限标识
     */
    public boolean hasPermission() {
        return this.permission != null && !this.permission.trim().isEmpty();
    }

    /**
     * 启用菜单
     */
    public void enable() {
        this.status = 1;
    }

    /**
     * 禁用菜单
     */
    public void disable() {
        this.status = 0;
    }

    /**
     * 显示菜单
     */
    public void show() {
        this.visible = 1;
    }

    /**
     * 隐藏菜单
     */
    public void hide() {
        this.visible = 0;
    }

    /**
     * 验证菜单数据完整性
     *
     * @throws IllegalArgumentException 数据不完整时抛出
     */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("菜单名称不能为空");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("菜单标题不能为空");
        }
        if (menuType == null) {
            throw new IllegalArgumentException("菜单类型不能为空");
        }

        // 目录和菜单必须有路径
        if ((isDirectory() || isMenu()) && (path == null || path.trim().isEmpty())) {
            throw new IllegalArgumentException("目录和菜单必须设置路由路径");
        }

        // 菜单必须有组件路径
        if (isMenu() && (component == null || component.trim().isEmpty())) {
            throw new IllegalArgumentException("菜单必须设置组件路径");
        }

        // 按钮必须有权限标识
        if (isButton() && !hasPermission()) {
            throw new IllegalArgumentException("按钮必须设置权限标识");
        }
    }

    /**
     * 判断是否可以作为某菜单的父节点
     * 只有目录可以作为父节点
     *
     * @return true-可以, false-不可以
     */
    public boolean canBeParent() {
        return isDirectory();
    }

    /**
     * 判断是否可以有子节点
     * 按钮不能有子节点
     *
     * @return true-可以, false-不可以
     */
    public boolean canHaveChildren() {
        return !isButton();
    }

    // Getter and Setter methods

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public void setMenuType(MenuType menuType) {
        this.menuType = menuType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getVisible() {
        return visible;
    }

    public void setVisible(Integer visible) {
        this.visible = visible;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(Integer keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Menu menu = (Menu) o;
        return Objects.equals(id, menu.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Menu{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", menuType=" + menuType +
                ", parentId=" + parentId +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
