package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.user.constant.OAuthProvider;
import com.nexusvoice.domain.user.constant.UserStatus;
import com.nexusvoice.domain.user.constant.UserType;
import com.nexusvoice.domain.user.model.User;
import com.nexusvoice.infrastructure.persistence.po.UserPO;
import org.springframework.stereotype.Component;

/**
 * 用户PO与领域对象转换器
 * 负责在持久化对象(PO)和领域对象(Domain)之间进行转换
 * 
 * @author NexusVoice
 * @since 2025-10-21
 */
@Component
public class UserPOConverter {

    /**
     * 将领域对象转换为持久化对象
     * 
     * @param user 领域用户对象
     * @return 持久化用户对象
     */
    public UserPO toPO(User user) {
        if (user == null) {
            return null;
        }

        UserPO po = new UserPO();
        
        // 基础字段
        po.setId(user.getId());
        po.setEmail(user.getEmail());
        po.setPasswordHash(user.getPasswordHash());
        po.setNickname(user.getNickname());
        po.setAvatarUrl(user.getAvatarUrl());
        po.setPhone(user.getPhone());
        po.setEmailVerified(user.getEmailVerified());
        po.setLastLoginAt(user.getLastLoginAt());
        po.setProfileBio(user.getProfileBio());
        
        // 枚举类型转换
        if (user.getUserType() != null) {
            po.setUserType(user.getUserType().name());
        }
        if (user.getStatus() != null) {
            po.setStatus(user.getStatus().getCode());
        }
        
        // OAuth相关字段
        if (user.getOauthProvider() != null) {
            po.setOauthProvider(user.getOauthProvider().getCode());
        }
        po.setOauthId(user.getOauthId());
        po.setOauthUsername(user.getOauthUsername());
        po.setOauthAvatarUrl(user.getOauthAvatarUrl());
        po.setOauthAccessToken(user.getOauthAccessToken());
        po.setOauthRefreshToken(user.getOauthRefreshToken());
        po.setOauthTokenExpiresAt(user.getOauthTokenExpiresAt());
        po.setOauthBindTime(user.getOauthBindTime());
        po.setOauthRawData(user.getOauthRawData());
        
        // 基础审计字段
        po.setCreatedAt(user.getCreatedAt());
        po.setUpdatedAt(user.getUpdatedAt());
        po.setDeleted(user.getDeleted());
        
        return po;
    }

    /**
     * 将持久化对象转换为领域对象
     * 
     * @param po 持久化用户对象
     * @return 领域用户对象
     */
    public User toDomain(UserPO po) {
        if (po == null) {
            return null;
        }

        User user = new User();
        
        // 基础字段
        user.setId(po.getId());
        user.setEmail(po.getEmail());
        user.setPasswordHash(po.getPasswordHash());
        user.setNickname(po.getNickname());
        user.setAvatarUrl(po.getAvatarUrl());
        user.setPhone(po.getPhone());
        user.setEmailVerified(po.getEmailVerified());
        user.setLastLoginAt(po.getLastLoginAt());
        user.setProfileBio(po.getProfileBio());
        
        // 转换为枚举类型
        if (po.getUserType() != null) {
            user.setUserType(UserType.fromCode(po.getUserType()));
        }
        if (po.getStatus() != null) {
            user.setStatus(UserStatus.fromCode(po.getStatus()));
        }
        
        // OAuth相关字段
        if (po.getOauthProvider() != null) {
            user.setOauthProvider(OAuthProvider.fromCode(po.getOauthProvider()));
        }
        user.setOauthId(po.getOauthId());
        user.setOauthUsername(po.getOauthUsername());
        user.setOauthAvatarUrl(po.getOauthAvatarUrl());
        user.setOauthAccessToken(po.getOauthAccessToken());
        user.setOauthRefreshToken(po.getOauthRefreshToken());
        user.setOauthTokenExpiresAt(po.getOauthTokenExpiresAt());
        user.setOauthBindTime(po.getOauthBindTime());
        user.setOauthRawData(po.getOauthRawData());
        
        // 基础审计字段
        user.setCreatedAt(po.getCreatedAt());
        user.setUpdatedAt(po.getUpdatedAt());
        user.setDeleted(po.getDeleted());
        
        return user;
    }

    /**
     * 更新持久化对象的字段（不包含审计字段）
     * 
     * @param po 目标持久化对象
     * @param user 源领域对象
     * @return 更新后的持久化对象
     */
    public UserPO updatePO(UserPO po, User user) {
        if (po == null || user == null) {
            return po;
        }

        // 更新业务字段（不更新ID和审计字段）
        po.setEmail(user.getEmail());
        po.setPasswordHash(user.getPasswordHash());
        po.setNickname(user.getNickname());
        po.setAvatarUrl(user.getAvatarUrl());
        po.setPhone(user.getPhone());
        po.setEmailVerified(user.getEmailVerified());
        po.setLastLoginAt(user.getLastLoginAt());
        po.setProfileBio(user.getProfileBio());
        
        // 枚举类型转换
        if (user.getUserType() != null) {
            po.setUserType(user.getUserType().name());
        }
        if (user.getStatus() != null) {
            po.setStatus(user.getStatus().getCode());
        }
        
        // OAuth相关字段
        if (user.getOauthProvider() != null) {
            po.setOauthProvider(user.getOauthProvider().getCode());
        }
        po.setOauthId(user.getOauthId());
        po.setOauthUsername(user.getOauthUsername());
        po.setOauthAvatarUrl(user.getOauthAvatarUrl());
        po.setOauthAccessToken(user.getOauthAccessToken());
        po.setOauthRefreshToken(user.getOauthRefreshToken());
        po.setOauthTokenExpiresAt(user.getOauthTokenExpiresAt());
        po.setOauthBindTime(user.getOauthBindTime());
        po.setOauthRawData(user.getOauthRawData());
        
        return po;
    }
}
