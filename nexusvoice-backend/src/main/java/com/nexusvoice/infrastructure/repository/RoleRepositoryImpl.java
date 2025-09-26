package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusvoice.application.user.dto.PageResult;
import com.nexusvoice.domain.role.model.Role;
import com.nexusvoice.domain.role.repository.RoleRepository;
import com.nexusvoice.infrastructure.database.mapper.RoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 角色仓储实现类
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Repository
public class RoleRepositoryImpl implements RoleRepository {

    @Autowired
    private RoleMapper roleMapper;

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(roleMapper.selectById(id));
    }

    @Override
    public Role save(Role role) {
        if (role.getId() == null) {
            int result = roleMapper.insert(role);
            if (result <= 0) {
                throw new RuntimeException("保存角色失败");
            }
        } else {
            int result = roleMapper.updateById(role);
            if (result <= 0) {
                throw new RuntimeException("更新角色失败");
            }
        }
        return role;
    }

    @Override
    public Role update(Role role) {
        int result = roleMapper.updateById(role);
        if (result <= 0) {
            throw new RuntimeException("更新角色失败");
        }
        return role;
    }

    @Override
    public void deleteById(String id) {
        roleMapper.deleteById(id);
    }

    @Override
    public PageResult<Role> pagePublicRoles(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getIsPublic, true);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Role::getName, keyword).or().like(Role::getDescription, keyword));
        }
        wrapper.orderByDesc(Role::getCreatedAt);

        Page<Role> pageParam = new Page<>(page, size);
        IPage<Role> pageResult = roleMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    @Override
    public PageResult<Role> pageAllPrivateRoles(Integer page, Integer size, String keyword, String userId) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getIsPublic, false);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Role::getName, keyword).or().like(Role::getDescription, keyword));
        }
        if (StringUtils.hasText(userId)) {
            wrapper.eq(Role::getUserId, userId);
        }
        wrapper.orderByDesc(Role::getCreatedAt);

        Page<Role> pageParam = new Page<>(page, size);
        IPage<Role> pageResult = roleMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    @Override
    public PageResult<Role> pageUserPrivateRoles(Integer page, Integer size, String keyword, String ownerUserId) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getIsPublic, false);
        wrapper.eq(Role::getUserId, ownerUserId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Role::getName, keyword).or().like(Role::getDescription, keyword));
        }
        wrapper.orderByDesc(Role::getCreatedAt);

        Page<Role> pageParam = new Page<>(page, size);
        IPage<Role> pageResult = roleMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }
}
