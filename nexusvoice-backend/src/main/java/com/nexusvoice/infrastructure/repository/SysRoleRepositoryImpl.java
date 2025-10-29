package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rbac.model.SysRole;
import com.nexusvoice.domain.rbac.repository.SysRoleRepository;
import com.nexusvoice.infrastructure.converter.SysRoleConverter;
import com.nexusvoice.infrastructure.persistence.mapper.SysRoleMapper;
import com.nexusvoice.infrastructure.persistence.po.SysRolePO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 系统角色仓储实现
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Repository
public class SysRoleRepositoryImpl implements SysRoleRepository {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public Optional<SysRole> findById(Long id) {
        SysRolePO po = sysRoleMapper.selectById(id);
        return Optional.ofNullable(SysRoleConverter.toDomain(po));
    }

    @Override
    public Optional<SysRole> findByRoleCode(String roleCode) {
        LambdaQueryWrapper<SysRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePO::getRoleCode, roleCode);
        SysRolePO po = sysRoleMapper.selectOne(wrapper);
        return Optional.ofNullable(SysRoleConverter.toDomain(po));
    }

    @Override
    public List<SysRole> findByUserId(Long userId) {
        List<SysRolePO> poList = sysRoleMapper.selectByUserId(userId);
        return poList.stream()
                .map(SysRoleConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SysRole> findAll() {
        List<SysRolePO> poList = sysRoleMapper.selectList(null);
        return poList.stream()
                .map(SysRoleConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SysRole> findAllEnabled() {
        LambdaQueryWrapper<SysRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePO::getStatus, 1);
        wrapper.orderByAsc(SysRolePO::getSortOrder);
        
        List<SysRolePO> poList = sysRoleMapper.selectList(wrapper);
        return poList.stream()
                .map(SysRoleConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public SysRole save(SysRole role) {
        SysRolePO po = SysRoleConverter.toPO(role);
        if (po.getCreatedAt() == null) {
            po.setCreatedAt(LocalDateTime.now());
        }
        if (po.getUpdatedAt() == null) {
            po.setUpdatedAt(LocalDateTime.now());
        }
        sysRoleMapper.insert(po);
        role.setId(po.getId());
        return role;
    }

    @Override
    public SysRole update(SysRole role) {
        SysRolePO po = SysRoleConverter.toPO(role);
        po.setUpdatedAt(LocalDateTime.now());
        sysRoleMapper.updateById(po);
        return role;
    }

    @Override
    public void deleteById(Long id) {
        sysRoleMapper.deleteById(id);
    }

    @Override
    public boolean existsByRoleCode(String roleCode) {
        LambdaQueryWrapper<SysRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePO::getRoleCode, roleCode);
        return sysRoleMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByRoleCodeExcludeId(String roleCode, Long excludeId) {
        LambdaQueryWrapper<SysRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePO::getRoleCode, roleCode);
        wrapper.ne(SysRolePO::getId, excludeId);
        return sysRoleMapper.selectCount(wrapper) > 0;
    }

    @Override
    public long count() {
        return sysRoleMapper.selectCount(null);
    }
}
