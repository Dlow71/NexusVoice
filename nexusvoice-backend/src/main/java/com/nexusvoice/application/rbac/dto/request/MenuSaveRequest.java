package com.nexusvoice.application.rbac.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 菜单保存请求（新增/更新）
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Data
@Schema(description = "菜单保存请求")
public class MenuSaveRequest {

    @Schema(description = "菜单ID（更新时必填）")
    private Long id;

    @Schema(description = "父菜单ID，0表示根菜单", example = "0")
    private Long parentId;

    @NotNull(message = "菜单类型不能为空")
    @Schema(description = "菜单类型：1-目录 2-菜单 3-按钮", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer menuType;

    @Size(max = 200, message = "路由路径长度不能超过200")
    @Schema(description = "路由路径", example = "/system/user")
    private String path;

    @Size(max = 200, message = "组件路径长度不能超过200")
    @Schema(description = "组件路径", example = "system/user")
    private String component;

    @NotBlank(message = "路由名称不能为空")
    @Size(max = 50, message = "路由名称长度不能超过50")
    @Schema(description = "路由名称", example = "User", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "菜单标题不能为空")
    @Size(max = 50, message = "菜单标题长度不能超过50")
    @Schema(description = "菜单标题", example = "用户管理", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = 50, message = "菜单图标长度不能超过50")
    @Schema(description = "菜单图标")
    private String icon;

    @Size(max = 100, message = "权限标识长度不能超过100")
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

    @Size(max = 500, message = "外部链接地址长度不能超过500")
    @Schema(description = "外部链接地址")
    private String externalLink;
}
