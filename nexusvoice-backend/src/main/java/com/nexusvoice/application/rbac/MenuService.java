package com.nexusvoice.application.rbac;

import com.nexusvoice.application.rbac.assembler.MenuAssembler;
import com.nexusvoice.application.rbac.dto.MenuDTO;
import com.nexusvoice.application.rbac.dto.request.MenuSaveRequest;
import com.nexusvoice.domain.rbac.model.Menu;
import com.nexusvoice.domain.rbac.repository.MenuRepository;
import com.nexusvoice.domain.rbac.repository.RoleMenuRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.security.PermissionChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 菜单应用服务
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Slf4j
@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private RoleMenuRepository roleMenuRepository;

    @Lazy
    @Autowired
    private PermissionChecker permissionChecker;

    /**
     * 保存菜单（新增或更新）
     *
     * @param request 保存请求
     * @return 菜单DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public MenuDTO saveMenu(MenuSaveRequest request) {
        Menu menu = MenuAssembler.toDomain(request);

        // 验证菜单数据
        menu.validate();

        // 检查父菜单
        if (!menu.isRootMenu()) {
            Menu parentMenu = menuRepository.findById(menu.getParentId())
                    .orElseThrow(() -> new BizException(ErrorCodeEnum.MENU_PARENT_NOT_FOUND));
            
            if (!parentMenu.canBeParent()) {
                throw new BizException(ErrorCodeEnum.MENU_PARENT_INVALID);
            }
        }

        // 检查菜单名称唯一性
        if (menu.getId() != null) {
            // 更新
            if (menuRepository.existsByNameExcludeId(menu.getName(), menu.getId())) {
                throw new BizException(ErrorCodeEnum.MENU_NAME_ALREADY_EXISTS);
            }
            log.info("更新菜单，menuId：{}, name：{}", menu.getId(), menu.getName());
            menu = menuRepository.update(menu);
        } else {
            // 新增
            if (menuRepository.existsByName(menu.getName())) {
                throw new BizException(ErrorCodeEnum.MENU_NAME_ALREADY_EXISTS);
            }
            log.info("创建菜单，name：{}", menu.getName());
            menu = menuRepository.save(menu);
        }

        log.info("菜单保存成功，menuId：{}", menu.getId());
        
        // 清除所有用户权限缓存（菜单变更影响权限）
        permissionChecker.evictAllPermissionCache();
        log.info("已清除所有用户权限缓存");
        
        return MenuAssembler.toDTO(menu);
    }

    /**
     * 删除菜单
     *
     * @param menuId 菜单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long menuId) {
        log.info("删除菜单，menuId：{}", menuId);

        // 检查菜单是否存在
        menuRepository.findById(menuId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.MENU_NOT_FOUND));

        // 检查是否有子菜单
        if (menuRepository.hasChildren(menuId)) {
            throw new BizException(ErrorCodeEnum.MENU_HAS_CHILDREN);
        }

        // 删除角色菜单关联
        roleMenuRepository.deleteByMenuId(menuId);

        // 删除菜单
        menuRepository.deleteById(menuId);

        // 清除所有用户权限缓存
        permissionChecker.evictAllPermissionCache();
        log.info("已清除所有用户权限缓存");

        log.info("菜单删除成功，menuId：{}", menuId);
    }

    /**
     * 查询菜单详情
     *
     * @param menuId 菜单ID
     * @return 菜单DTO
     */
    public MenuDTO getMenuById(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.MENU_NOT_FOUND));
        return MenuAssembler.toDTO(menu);
    }

    /**
     * 查询所有菜单（扁平列表）
     *
     * @return 菜单列表
     */
    public List<MenuDTO> listMenus() {
        List<Menu> menus = menuRepository.findAll();
        return MenuAssembler.toDTOList(menus);
    }

    /**
     * 查询菜单树
     *
     * @return 菜单树
     */
    public List<MenuDTO> getMenuTree() {
        List<Menu> menus = menuRepository.findAllEnabled();
        List<MenuDTO> menuDTOs = MenuAssembler.toDTOList(menus);
        return MenuAssembler.buildTree(menuDTOs, 0L);
    }

    /**
     * 查询用户的菜单树
     *
     * @param userId 用户ID
     * @return 菜单树
     */
    public List<MenuDTO> getUserMenuTree(Long userId) {
        List<Menu> menus = menuRepository.findByUserId(userId);
        List<MenuDTO> menuDTOs = MenuAssembler.toDTOList(menus);
        
        // 只返回启用且可见的菜单
        menuDTOs = menuDTOs.stream()
                .filter(dto -> Integer.valueOf(1).equals(dto.getStatus()))  // 启用
                .filter(dto -> Integer.valueOf(1).equals(dto.getVisible())) // 可见
                .toList();
        
        return MenuAssembler.buildTree(menuDTOs, 0L);
    }

    /**
     * 查询角色的菜单列表
     *
     * @param roleId 角色ID
     * @return 菜单列表
     */
    public List<MenuDTO> getRoleMenus(Long roleId) {
        List<Menu> menus = menuRepository.findByRoleId(roleId);
        return MenuAssembler.toDTOList(menus);
    }

    /**
     * 查询用户的权限标识列表
     *
     * @param userId 用户ID
     * @return 权限标识列表
     */
    public List<String> getUserPermissions(Long userId) {
        List<Menu> menus = menuRepository.findByUserId(userId);
        return menus.stream()
                .filter(Menu::hasPermission)
                .filter(Menu::isEnabled)
                .map(Menu::getPermission)
                .distinct()
                .toList();
    }
}
