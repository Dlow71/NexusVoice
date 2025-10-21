package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusvoice.application.user.dto.PageResult;
import com.nexusvoice.domain.role.model.Role;
import com.nexusvoice.domain.role.repository.RoleRepository;
import com.nexusvoice.infrastructure.persistence.mapper.RolePOMapper;
import com.nexusvoice.infrastructure.persistence.converter.RolePOConverter;
import com.nexusvoice.infrastructure.persistence.po.RolePO;
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

    private final RolePOMapper rolePOMapper;
    private final RolePOConverter rolePOConverter;
    
    /**
     * 构造器注入
     */
    public RoleRepositoryImpl(RolePOMapper rolePOMapper, RolePOConverter rolePOConverter) {
        this.rolePOMapper = rolePOMapper;
        this.rolePOConverter = rolePOConverter;
    }

    @Override
    public Optional<Role> findById(Long id) {
        RolePO po = rolePOMapper.selectById(id);
        return Optional.ofNullable(rolePOConverter.toDomain(po));
    }

    @Override
    public Role save(Role role) {
        RolePO po = rolePOConverter.toPO(role);
        if (role.getId() == null) {
            int result = rolePOMapper.insert(po);
            if (result <= 0) {
                throw new RuntimeException("保存角色失败");
            }
            role.setId(po.getId());
        } else {
            int result = rolePOMapper.updateById(po);
            if (result <= 0) {
                throw new RuntimeException("更新角色失败");
            }
        }
        return role;
    }

    @Override
    public Role update(Role role) {
        RolePO po = rolePOConverter.toPO(role);
        int result = rolePOMapper.updateById(po);
        if (result <= 0) {
            throw new RuntimeException("更新角色失败");
        }
        return role;
    }

    @Override
    public void deleteById(Long id) {
        rolePOMapper.deleteById(id);
    }

    @Override
    public PageResult<Role> pagePublicRoles(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<RolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePO::getIsPublic, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(RolePO::getName, keyword).or().like(RolePO::getDescription, keyword));
        }
        wrapper.orderByDesc(RolePO::getCreatedAt);

        Page<RolePO> pageParam = new Page<>(page, size);
        IPage<RolePO> pageResult = rolePOMapper.selectPage(pageParam, wrapper);
        
        // Convert POs to Domain entities
        java.util.List<Role> roles = pageResult.getRecords().stream()
                .map(rolePOConverter::toDomain)
                .collect(java.util.stream.Collectors.toList());
        return new PageResult<>(roles, pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    @Override
    public PageResult<Role> pageAllPrivateRoles(Integer page, Integer size, String keyword, Long userId) {
        LambdaQueryWrapper<RolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePO::getIsPublic, 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(RolePO::getName, keyword).or().like(RolePO::getDescription, keyword));
        }
        if (userId != null) {
            wrapper.eq(RolePO::getUserId, userId);
        }
        wrapper.orderByDesc(RolePO::getCreatedAt);

        Page<RolePO> pageParam = new Page<>(page, size);
        IPage<RolePO> pageResult = rolePOMapper.selectPage(pageParam, wrapper);
        
        // Convert POs to Domain entities
        java.util.List<Role> roles = pageResult.getRecords().stream()
                .map(rolePOConverter::toDomain)
                .collect(java.util.stream.Collectors.toList());
        return new PageResult<>(roles, pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    @Override
    public PageResult<Role> pageUserPrivateRoles(Integer page, Integer size, String keyword, Long ownerUserId) {
        LambdaQueryWrapper<RolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePO::getIsPublic, 0);
        wrapper.eq(RolePO::getUserId, ownerUserId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(RolePO::getName, keyword).or().like(RolePO::getDescription, keyword));
        }
        wrapper.orderByDesc(RolePO::getCreatedAt);

        Page<RolePO> pageParam = new Page<>(page, size);
        IPage<RolePO> pageResult = rolePOMapper.selectPage(pageParam, wrapper);
        
        // Convert POs to Domain entities
        java.util.List<Role> roles = pageResult.getRecords().stream()
                .map(rolePOConverter::toDomain)
                .collect(java.util.stream.Collectors.toList());
        return new PageResult<>(roles, pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }
}
