package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexusvoice.application.user.dto.PageResult;
import com.nexusvoice.domain.user.constant.UserStatus;
import com.nexusvoice.domain.user.constant.UserType;
import com.nexusvoice.domain.user.model.User;
import com.nexusvoice.domain.user.repository.UserRepository;
import com.nexusvoice.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 用户仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Repository
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {

    @Override
    public Optional<User> findById(String id) {
        User user = getById(id);
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        User user = getOne(wrapper);
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        if (phone == null) {
            return Optional.empty();
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        User user = getOne(wrapper);
        return Optional.ofNullable(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return count(wrapper) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        if (phone == null) {
            return false;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return count(wrapper) > 0;
    }

    @Override
    public User save(User user) {
        super.save(user);
        return user;
    }

    @Override
    public User update(User user) {
        super.updateById(user);
        return user;
    }

    @Override
    public void deleteById(String id) {
        super.removeById(id);
    }

    @Override
    public long count() {
        return super.count();
    }

    @Override
    public PageResult<User> findUserPage(Integer page, Integer size, String keyword, 
                                        UserType userType, UserStatus status) {
        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getEmail, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        
        if (userType != null) {
            wrapper.eq(User::getUserType, userType);
        }
        
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        
        wrapper.orderByDesc(User::getCreatedAt);
        
        // 分页查询
        Page<User> pageParam = new Page<>(page, size);
        IPage<User> pageResult = super.page(pageParam, wrapper);
        
        // 转换为自定义分页结果
        return new PageResult<>(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
    }
}
