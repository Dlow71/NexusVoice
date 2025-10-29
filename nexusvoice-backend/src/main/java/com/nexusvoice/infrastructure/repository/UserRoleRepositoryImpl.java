package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rbac.repository.UserRoleRepository;
import com.nexusvoice.infrastructure.persistence.mapper.UserRoleMapper;
import com.nexusvoice.infrastructure.persistence.po.UserRolePO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户角色关联仓储实现
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Repository
public class UserRoleRepositoryImpl implements UserRoleRepository {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Override
    public List<Long> findRoleIdsByUserId(Long userId) {
        LambdaQueryWrapper<UserRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRolePO::getUserId, userId);
        
        List<UserRolePO> poList = userRoleMapper.selectList(wrapper);
        return poList.stream()
                .map(UserRolePO::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findUserIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<UserRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRolePO::getRoleId, roleId);
        
        List<UserRolePO> poList = userRoleMapper.selectList(wrapper);
        return poList.stream()
                .map(UserRolePO::getUserId)
                .collect(Collectors.toList());
    }

    @Override
    public void save(Long userId, Long roleId) {
        // 检查是否已存在
        if (exists(userId, roleId)) {
            return;
        }
        
        UserRolePO po = new UserRolePO();
        po.setUserId(userId);
        po.setRoleId(roleId);
        po.setCreatedAt(LocalDateTime.now());
        userRoleMapper.insert(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        
        // 先删除用户的所有角色关联
        deleteByUserId(userId);
        
        // 批量插入新的关联
        for (Long roleId : roleIds) {
            save(userId, roleId);
        }
    }

    @Override
    public void delete(Long userId, Long roleId) {
        LambdaQueryWrapper<UserRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRolePO::getUserId, userId);
        wrapper.eq(UserRolePO::getRoleId, roleId);
        userRoleMapper.delete(wrapper);
    }

    @Override
    public void deleteByUserId(Long userId) {
        LambdaQueryWrapper<UserRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRolePO::getUserId, userId);
        userRoleMapper.delete(wrapper);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        LambdaQueryWrapper<UserRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRolePO::getRoleId, roleId);
        userRoleMapper.delete(wrapper);
    }

    @Override
    public boolean exists(Long userId, Long roleId) {
        LambdaQueryWrapper<UserRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRolePO::getUserId, userId);
        wrapper.eq(UserRolePO::getRoleId, roleId);
        return userRoleMapper.selectCount(wrapper) > 0;
    }

    @Override
    public long countUsersByRoleId(Long roleId) {
        LambdaQueryWrapper<UserRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRolePO::getRoleId, roleId);
        return userRoleMapper.selectCount(wrapper);
    }
}
