package com.nexusvoice.infrastructure.converter;

import com.nexusvoice.domain.rbac.constant.MenuType;
import com.nexusvoice.domain.rbac.model.Menu;
import com.nexusvoice.infrastructure.persistence.po.MenuPO;

/**
 * 菜单转换器
 * 负责领域实体和持久化对象之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
public class MenuConverter {

    /**
     * PO转领域实体
     *
     * @param po 持久化对象
     * @return 领域实体
     */
    public static Menu toDomain(MenuPO po) {
        if (po == null) {
            return null;
        }

        Menu menu = new Menu();
        menu.setId(po.getId());
        menu.setParentId(po.getParentId());
        // 转换枚举类型
        menu.setMenuType(po.getMenuType() != null ? MenuType.fromCode(po.getMenuType()) : null);
        menu.setPath(po.getPath());
        menu.setComponent(po.getComponent());
        menu.setName(po.getName());
        menu.setTitle(po.getTitle());
        menu.setIcon(po.getIcon());
        menu.setPermission(po.getPermission());
        menu.setSortOrder(po.getSortOrder());
        menu.setVisible(po.getVisible());
        menu.setStatus(po.getStatus());
        menu.setKeepAlive(po.getKeepAlive());
        menu.setExternalLink(po.getExternalLink());
        menu.setCreatedAt(po.getCreatedAt());
        menu.setUpdatedAt(po.getUpdatedAt());

        return menu;
    }

    /**
     * 领域实体转PO
     *
     * @param menu 领域实体
     * @return 持久化对象
     */
    public static MenuPO toPO(Menu menu) {
        if (menu == null) {
            return null;
        }

        MenuPO po = new MenuPO();
        po.setId(menu.getId());
        po.setParentId(menu.getParentId());
        // 转换枚举类型
        po.setMenuType(menu.getMenuType() != null ? menu.getMenuType().getCode() : null);
        po.setPath(menu.getPath());
        po.setComponent(menu.getComponent());
        po.setName(menu.getName());
        po.setTitle(menu.getTitle());
        po.setIcon(menu.getIcon());
        po.setPermission(menu.getPermission());
        po.setSortOrder(menu.getSortOrder());
        po.setVisible(menu.getVisible());
        po.setStatus(menu.getStatus());
        po.setKeepAlive(menu.getKeepAlive());
        po.setExternalLink(menu.getExternalLink());
        po.setCreatedAt(menu.getCreatedAt());
        po.setUpdatedAt(menu.getUpdatedAt());

        return po;
    }
}
