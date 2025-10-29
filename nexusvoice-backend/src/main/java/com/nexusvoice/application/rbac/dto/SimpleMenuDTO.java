package com.nexusvoice.application.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简化菜单DTO（用于前端路由）
 * 适配前端AppRouteRecord结构
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简化菜单DTO")
public class SimpleMenuDTO {

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "路由名称")
    private String name;

    @Schema(description = "路由元信息")
    private MetaInfo meta;

    @Schema(description = "子路由列表")
    private List<SimpleMenuDTO> children;

    /**
     * 路由元信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "路由元信息")
    public static class MetaInfo {

        @Schema(description = "菜单标题")
        private String title;

        @Schema(description = "菜单图标")
        private String icon;

        @Schema(description = "是否隐藏")
        private Boolean isHide;

        @Schema(description = "是否缓存")
        private Boolean keepAlive;

        @Schema(description = "按钮权限列表")
        private List<AuthItem> authList;

        @Schema(description = "外部链接地址")
        private String link;

        @Schema(description = "是否iframe")
        private Boolean isIframe;

        @Schema(description = "权限标识")
        private String permission;
    }

    /**
     * 按钮权限项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "按钮权限项")
    public static class AuthItem {

        @Schema(description = "按钮标题")
        private String title;

        @Schema(description = "权限标识（最后一段）")
        private String authMark;

        @Schema(description = "完整权限标识")
        private String permission;
    }
}
