package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rbac.constant.MenuType;
import com.nexusvoice.domain.rbac.model.Menu;
import com.nexusvoice.domain.rbac.repository.MenuRepository;
import com.nexusvoice.infrastructure.converter.MenuConverter;
import com.nexusvoice.infrastructure.persistence.mapper.MenuMapper;
import com.nexusvoice.infrastructure.persistence.po.MenuPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 菜单仓储实现
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Repository
public class MenuRepositoryImpl implements MenuRepository {

    @Autowired
    private MenuMapper menuMapper;

    @Override
    public Optional<Menu> findById(Long id) {
        MenuPO po = menuMapper.selectById(id);
        return Optional.ofNullable(MenuConverter.toDomain(po));
    }

    @Override
    public Optional<Menu> findByName(String name) {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuPO::getName, name);
        MenuPO po = menuMapper.selectOne(wrapper);
        return Optional.ofNullable(MenuConverter.toDomain(po));
    }

    @Override
    public List<Menu> findByRoleId(Long roleId) {
        List<MenuPO> poList = menuMapper.selectByRoleId(roleId);
        return poList.stream()
                .map(MenuConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> findByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<MenuPO> poList = menuMapper.selectByRoleIds(roleIds);
        return poList.stream()
                .map(MenuConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> findByUserId(Long userId) {
        List<MenuPO> poList = menuMapper.selectByUserId(userId);
        return poList.stream()
                .map(MenuConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> findByParentId(Long parentId) {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuPO::getParentId, parentId);
        wrapper.orderByAsc(MenuPO::getSortOrder);
        
        List<MenuPO> poList = menuMapper.selectList(wrapper);
        return poList.stream()
                .map(MenuConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> findAll() {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(MenuPO::getSortOrder);
        
        List<MenuPO> poList = menuMapper.selectList(wrapper);
        return poList.stream()
                .map(MenuConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> findAllEnabled() {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuPO::getStatus, 1);
        wrapper.orderByAsc(MenuPO::getSortOrder);
        
        List<MenuPO> poList = menuMapper.selectList(wrapper);
        return poList.stream()
                .map(MenuConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> findByMenuType(MenuType menuType) {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuPO::getMenuType, menuType.getCode());
        wrapper.orderByAsc(MenuPO::getSortOrder);
        
        List<MenuPO> poList = menuMapper.selectList(wrapper);
        return poList.stream()
                .map(MenuConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> findRootMenus() {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuPO::getParentId, 0L);
        wrapper.orderByAsc(MenuPO::getSortOrder);
        
        List<MenuPO> poList = menuMapper.selectList(wrapper);
        return poList.stream()
                .map(MenuConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Menu save(Menu menu) {
        MenuPO po = MenuConverter.toPO(menu);
        if (po.getCreatedAt() == null) {
            po.setCreatedAt(LocalDateTime.now());
        }
        if (po.getUpdatedAt() == null) {
            po.setUpdatedAt(LocalDateTime.now());
        }
        menuMapper.insert(po);
        menu.setId(po.getId());
        return menu;
    }

    @Override
    public Menu update(Menu menu) {
        MenuPO po = MenuConverter.toPO(menu);
        po.setUpdatedAt(LocalDateTime.now());
        menuMapper.updateById(po);
        return menu;
    }

    @Override
    public void deleteById(Long id) {
        menuMapper.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuPO::getName, name);
        wrapper.eq(MenuPO::getDeleted, 0); // 只检查未删除的菜单
        return menuMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByNameExcludeId(String name, Long excludeId) {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuPO::getName, name);
        wrapper.ne(MenuPO::getId, excludeId);
        wrapper.eq(MenuPO::getDeleted, 0); // 只检查未删除的菜单
        return menuMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean hasChildren(Long menuId) {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuPO::getParentId, menuId);
        return menuMapper.selectCount(wrapper) > 0;
    }

    @Override
    public long count() {
        return menuMapper.selectCount(null);
    }

    @Override
    public Optional<Menu> findByPermission(String permission) {
        LambdaQueryWrapper<MenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuPO::getPermission, permission);
        MenuPO po = menuMapper.selectOne(wrapper);
        return Optional.ofNullable(MenuConverter.toDomain(po));
    }
}
