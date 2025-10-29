package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rbac.repository.RoleMenuRepository;
import com.nexusvoice.infrastructure.persistence.mapper.RoleMenuMapper;
import com.nexusvoice.infrastructure.persistence.po.RoleMenuPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色菜单关联仓储实现
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Repository
public class RoleMenuRepositoryImpl implements RoleMenuRepository {

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Override
    public List<Long> findMenuIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<RoleMenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenuPO::getRoleId, roleId);
        
        List<RoleMenuPO> poList = roleMenuMapper.selectList(wrapper);
        return poList.stream()
                .map(RoleMenuPO::getMenuId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findRoleIdsByMenuId(Long menuId) {
        LambdaQueryWrapper<RoleMenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenuPO::getMenuId, menuId);
        
        List<RoleMenuPO> poList = roleMenuMapper.selectList(wrapper);
        return poList.stream()
                .map(RoleMenuPO::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    public void save(Long roleId, Long menuId) {
        // 检查是否已存在
        if (exists(roleId, menuId)) {
            return;
        }
        
        RoleMenuPO po = new RoleMenuPO();
        po.setRoleId(roleId);
        po.setMenuId(menuId);
        po.setCreatedAt(LocalDateTime.now());
        roleMenuMapper.insert(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(Long roleId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        
        // 先删除角色的所有菜单关联
        deleteByRoleId(roleId);
        
        // 批量插入新的关联
        for (Long menuId : menuIds) {
            save(roleId, menuId);
        }
    }

    @Override
    public void delete(Long roleId, Long menuId) {
        LambdaQueryWrapper<RoleMenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenuPO::getRoleId, roleId);
        wrapper.eq(RoleMenuPO::getMenuId, menuId);
        roleMenuMapper.delete(wrapper);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        LambdaQueryWrapper<RoleMenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenuPO::getRoleId, roleId);
        roleMenuMapper.delete(wrapper);
    }

    @Override
    public void deleteByMenuId(Long menuId) {
        LambdaQueryWrapper<RoleMenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenuPO::getMenuId, menuId);
        roleMenuMapper.delete(wrapper);
    }

    @Override
    public boolean exists(Long roleId, Long menuId) {
        LambdaQueryWrapper<RoleMenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenuPO::getRoleId, roleId);
        wrapper.eq(RoleMenuPO::getMenuId, menuId);
        return roleMenuMapper.selectCount(wrapper) > 0;
    }

    @Override
    public long countRolesByMenuId(Long menuId) {
        LambdaQueryWrapper<RoleMenuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenuPO::getMenuId, menuId);
        return roleMenuMapper.selectCount(wrapper);
    }
}
