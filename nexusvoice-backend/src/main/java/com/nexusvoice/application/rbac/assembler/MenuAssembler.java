package com.nexusvoice.application.rbac.assembler;

import com.nexusvoice.application.rbac.dto.MenuDTO;
import com.nexusvoice.application.rbac.dto.request.MenuSaveRequest;
import com.nexusvoice.domain.rbac.constant.MenuType;
import com.nexusvoice.domain.rbac.model.Menu;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单装配器
 * 负责DTO和领域实体之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
public class MenuAssembler {

    /**
     * 领域实体转DTO
     *
     * @param menu 领域实体
     * @return DTO
     */
    public static MenuDTO toDTO(Menu menu) {
        if (menu == null) {
            return null;
        }

        MenuDTO dto = new MenuDTO();
        dto.setId(menu.getId());
        dto.setParentId(menu.getParentId());
        dto.setMenuType(menu.getMenuType() != null ? menu.getMenuType().getCode() : null);
        dto.setPath(menu.getPath());
        dto.setComponent(menu.getComponent());
        dto.setName(menu.getName());
        dto.setTitle(menu.getTitle());
        dto.setIcon(menu.getIcon());
        dto.setPermission(menu.getPermission());
        dto.setSortOrder(menu.getSortOrder());
        dto.setVisible(menu.getVisible());
        dto.setStatus(menu.getStatus());
        dto.setKeepAlive(menu.getKeepAlive());
        dto.setExternalLink(menu.getExternalLink());
        dto.setCreatedAt(menu.getCreatedAt());
        dto.setUpdatedAt(menu.getUpdatedAt());

        return dto;
    }

    /**
     * 领域实体列表转DTO列表
     *
     * @param menus 领域实体列表
     * @return DTO列表
     */
    public static List<MenuDTO> toDTOList(List<Menu> menus) {
        if (menus == null) {
            return null;
        }
        return menus.stream()
                .map(MenuAssembler::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 保存请求转领域实体
     *
     * @param request 保存请求
     * @return 领域实体
     */
    public static Menu toDomain(MenuSaveRequest request) {
        if (request == null) {
            return null;
        }

        Menu menu = new Menu();
        menu.setId(request.getId());
        menu.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        menu.setMenuType(request.getMenuType() != null ? MenuType.fromCode(request.getMenuType()) : null);
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setName(request.getName());
        menu.setTitle(request.getTitle());
        menu.setIcon(request.getIcon());
        menu.setPermission(request.getPermission());
        menu.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        menu.setVisible(request.getVisible() != null ? request.getVisible() : 1);
        menu.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        menu.setKeepAlive(request.getKeepAlive() != null ? request.getKeepAlive() : 0);
        menu.setExternalLink(request.getExternalLink());

        return menu;
    }

    /**
     * 构建菜单树
     *
     * @param menus 菜单列表
     * @param parentId 父菜单ID
     * @return 菜单树
     */
    public static List<MenuDTO> buildTree(List<MenuDTO> menus, Long parentId) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        return menus.stream()
                .filter(menu -> {
                    Long pid = menu.getParentId();
                    return (parentId == null && (pid == null || pid == 0L)) ||
                           (parentId != null && parentId.equals(pid));
                })
                .peek(menu -> {
                    // 递归构建子菜单
                    List<MenuDTO> children = buildTree(menus, menu.getId());
                    menu.setChildren(children.isEmpty() ? null : children);
                })
                .collect(Collectors.toList());
    }
}
