package com.nexusvoice.application.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单DTO
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Data
@Schema(description = "菜单DTO")
public class MenuDTO {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "父菜单ID，0表示根菜单", example = "0")
    private Long parentId;

    @Schema(description = "菜单类型：1-目录 2-菜单 3-按钮", example = "2")
    private Integer menuType;

    @Schema(description = "路由路径", example = "/system/user")
    private String path;

    @Schema(description = "组件路径", example = "system/user")
    private String component;

    @Schema(description = "路由名称", example = "User")
    private String name;

    @Schema(description = "菜单标题", example = "用户管理")
    private String title;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "权限标识", example = "system:user:view")
    private String permission;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "是否可见：0-隐藏 1-显示", example = "1")
    private Integer visible;

    @Schema(description = "状态：0-禁用 1-启用", example = "1")
    private Integer status;

    @Schema(description = "是否缓存：0-否 1-是", example = "0")
    private Integer keepAlive;

    @Schema(description = "外部链接地址")
    private String externalLink;

    @Schema(description = "子菜单列表")
    private List<MenuDTO> children;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
