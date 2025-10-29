package com.nexusvoice.application.rbac.assembler;

import com.nexusvoice.application.rbac.dto.MenuDTO;
import com.nexusvoice.application.rbac.dto.SimpleMenuDTO;
import com.nexusvoice.application.rbac.dto.SimpleMenuDTO.AuthItem;
import com.nexusvoice.application.rbac.dto.SimpleMenuDTO.MetaInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 简化菜单组装器
 * 将后端MenuDTO转换为前端SimpleMenuDTO
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
public class SimpleMenuAssembler {

    /**
     * 将MenuDTO树转换为SimpleMenuDTO树
     * 过滤按钮类型菜单，聚合为authList
     * 
     * @param menuDTOs 菜单DTO列表
     * @return 简化菜单列表
     */
    public static List<SimpleMenuDTO> toSimpleMenuTree(List<MenuDTO> menuDTOs) {
        if (menuDTOs == null || menuDTOs.isEmpty()) {
            return new ArrayList<>();
        }

        return menuDTOs.stream()
                .filter(menu -> menu.getMenuType() != 3) // 过滤按钮类型
                .map(SimpleMenuAssembler::convertToSimpleMenu)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个菜单
     * 
     * @param menuDTO 菜单DTO
     * @return 简化菜单DTO
     */
    private static SimpleMenuDTO convertToSimpleMenu(MenuDTO menuDTO) {
        SimpleMenuDTO simpleMenu = new SimpleMenuDTO();
        simpleMenu.setPath(menuDTO.getPath());
        simpleMenu.setName(menuDTO.getName());

        // 组件路径处理
        if (menuDTO.getMenuType() == 1) {
            // 目录类型，使用Layout组件
            simpleMenu.setComponent("/index/index");
        } else if (menuDTO.getMenuType() == 2) {
            // 菜单类型，使用指定组件
            simpleMenu.setComponent(menuDTO.getComponent());
        }

        // 构建元信息
        MetaInfo meta = buildMetaInfo(menuDTO);
        simpleMenu.setMeta(meta);

        // 递归处理子菜单
        if (menuDTO.getChildren() != null && !menuDTO.getChildren().isEmpty()) {
            // 分离普通菜单和按钮
            List<MenuDTO> childMenus = menuDTO.getChildren().stream()
                    .filter(child -> child.getMenuType() != 3)
                    .collect(Collectors.toList());

            List<MenuDTO> buttons = menuDTO.getChildren().stream()
                    .filter(child -> child.getMenuType() == 3)
                    .collect(Collectors.toList());

            // 递归处理子菜单
            if (!childMenus.isEmpty()) {
                List<SimpleMenuDTO> children = childMenus.stream()
                        .map(SimpleMenuAssembler::convertToSimpleMenu)
                        .collect(Collectors.toList());
                simpleMenu.setChildren(children);
            }

            // 将按钮聚合为authList
            if (!buttons.isEmpty()) {
                List<AuthItem> authList = buttons.stream()
                        .map(SimpleMenuAssembler::convertToAuthItem)
                        .collect(Collectors.toList());
                meta.setAuthList(authList);
            }
        }

        return simpleMenu;
    }

    /**
     * 构建元信息
     * 
     * @param menuDTO 菜单DTO
     * @return 元信息
     */
    private static MetaInfo buildMetaInfo(MenuDTO menuDTO) {
        MetaInfo meta = MetaInfo.builder()
                .title(menuDTO.getTitle())
                .icon(menuDTO.getIcon())
                .isHide(menuDTO.getVisible() == 0)
                .keepAlive(menuDTO.getKeepAlive() == 1)
                .permission(menuDTO.getPermission())
                .build();

        // 外链/内嵌页面处理：前端根据 isIframe + link 渲染 Iframe
        if (menuDTO.getExternalLink() != null && !menuDTO.getExternalLink().isEmpty()) {
            meta.setLink(menuDTO.getExternalLink());
            // 统一设为内嵌，具体是否允许外链同域或跨域由前端控制
            meta.setIsIframe(true);
        }

        return meta;
    }

    /**
     * 将按钮菜单转换为权限项
     * 
     * @param buttonMenu 按钮菜单
     * @return 权限项
     */
    private static AuthItem convertToAuthItem(MenuDTO buttonMenu) {
        String permission = buttonMenu.getPermission();
        String authMark = extractAuthMark(permission);

        return AuthItem.builder()
                .title(buttonMenu.getTitle())
                .authMark(authMark)
                .permission(permission)
                .build();
    }

    /**
     * 从权限标识中提取authMark（最后一段）
     * 例如：system:user:add -> add
     * 
     * @param permission 权限标识
     * @return authMark
     */
    private static String extractAuthMark(String permission) {
        if (permission == null || permission.isEmpty()) {
            return "";
        }

        int lastColonIndex = permission.lastIndexOf(':');
        if (lastColonIndex >= 0 && lastColonIndex < permission.length() - 1) {
            return permission.substring(lastColonIndex + 1);
        }

        return permission;
    }
}
