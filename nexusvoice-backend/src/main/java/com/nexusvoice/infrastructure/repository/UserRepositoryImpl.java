package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusvoice.application.user.dto.PageResult;
import com.nexusvoice.domain.user.constant.UserStatus;
import com.nexusvoice.domain.user.constant.UserType;
import com.nexusvoice.domain.user.model.User;
import com.nexusvoice.domain.user.repository.UserRepository;
import com.nexusvoice.infrastructure.persistence.mapper.UserPOMapper;
import com.nexusvoice.infrastructure.persistence.converter.UserPOConverter;
import com.nexusvoice.infrastructure.persistence.po.UserPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户仓储实现类
 * 使用PO对象与数据库交互，通过转换器转换为领域对象
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserPOMapper userPOMapper;

    /**
     * 构造器注入（替代@Autowired字段注入）
     */
    public UserRepositoryImpl(UserPOMapper userPOMapper) {
        this.userPOMapper = userPOMapper;
    }

    @Override
    public Optional<User> findById(Long id) {
        UserPO po = userPOMapper.selectById(id);
        return Optional.ofNullable(UserPOConverter.toDomain(po));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getEmail, email);
        UserPO po = userPOMapper.selectOne(wrapper);
        return Optional.ofNullable(UserPOConverter.toDomain(po));
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getPhone, phone);
        UserPO po = userPOMapper.selectOne(wrapper);
        return Optional.ofNullable(UserPOConverter.toDomain(po));
    }

    @Override
    public boolean existsByEmail(String email) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getEmail, email);
        return userPOMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getPhone, phone)
                .eq(UserPO::getDeleted, 0);
        return userPOMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Optional<User> findByOAuthProviderAndId(String provider, String oauthId) {
        if (!StringUtils.hasText(provider) || !StringUtils.hasText(oauthId)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getOauthProvider, provider)
                .eq(UserPO::getOauthId, oauthId)
                .eq(UserPO::getDeleted, 0);
        UserPO po = userPOMapper.selectOne(wrapper);
        return Optional.ofNullable(UserPOConverter.toDomain(po));
    }

    @Override
    public boolean existsByOAuthProviderAndId(String provider, String oauthId) {
        if (!StringUtils.hasText(provider) || !StringUtils.hasText(oauthId)) {
            return false;
        }
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getOauthProvider, provider)
                .eq(UserPO::getOauthId, oauthId)
                .eq(UserPO::getDeleted, 0);
        return userPOMapper.selectCount(wrapper) > 0;
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            // 新增
            UserPO po = UserPOConverter.toPO(user);
            int result = userPOMapper.insert(po);
            if (result <= 0) {
                throw new RuntimeException("保存用户失败");
            }
            // 将生成的ID和审计字段回写到领域对象
            user.setId(po.getId());
            user.setCreatedAt(po.getCreatedAt());
            user.setUpdatedAt(po.getUpdatedAt());
        } else {
            // 更新
            UserPO po = UserPOConverter.toPO(user);
            int result = userPOMapper.updateById(po);
            if (result <= 0) {
                throw new RuntimeException("更新用户失败");
            }
            user.setUpdatedAt(po.getUpdatedAt());
        }
        return user;
    }

    @Override
    public User update(User user) {
        UserPO po = UserPOConverter.toPO(user);
        int result = userPOMapper.updateById(po);
        if (result <= 0) {
            throw new RuntimeException("更新用户失败");
        }
        user.setUpdatedAt(po.getUpdatedAt());
        return user;
    }

    @Override
    public void deleteById(Long id) {
        userPOMapper.deleteById(id);
    }

    @Override
    public long count() {
        return userPOMapper.selectCount(null);
    }

    @Override
    public PageResult<User> findUserPage(Integer page, Integer size, String keyword, 
                                        UserType userType, UserStatus status) {
        // 构建查询条件
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(UserPO::getEmail, keyword)
                    .or().like(UserPO::getNickname, keyword)
                    .or().like(UserPO::getPhone, keyword));
        }
        
        if (userType != null) {
            wrapper.eq(UserPO::getUserType, userType.name());
        }
        
        if (status != null) {
            wrapper.eq(UserPO::getStatus, status.name());
        }
        
        wrapper.orderByDesc(UserPO::getCreatedAt);
        
        // 分页查询
        Page<UserPO> pageParam = new Page<>(page, size);
        IPage<UserPO> pageResult = userPOMapper.selectPage(pageParam, wrapper);
        
        // 转换为领域对象列表
        var users = pageResult.getRecords().stream()
                .map(UserPOConverter::toDomain)
                .collect(Collectors.toList());
        
        // 转换为自定义分页结果
        return new PageResult<>(
                users,
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
    }
}
