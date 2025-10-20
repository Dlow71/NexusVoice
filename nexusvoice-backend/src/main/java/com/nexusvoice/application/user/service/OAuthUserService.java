package com.nexusvoice.application.user.service;

import com.nexusvoice.domain.user.constant.OAuthProvider;
import com.nexusvoice.domain.user.model.User;
import java.util.Map;

/**
 * OAuth用户服务接口
 * 负责处理OAuth认证相关的用户业务逻辑
 * 
 * @author NexusVoice
 * @since 2025-01-20
 */
public interface OAuthUserService {
    
    /**
     * 查找或创建OAuth用户
     * 
     * @param provider OAuth提供商
     * @param oauthUserInfo OAuth用户信息（从第三方平台获取的原始信息）
     * @return 系统用户
     */
    User findOrCreateOAuthUser(OAuthProvider provider, Map<String, Object> oauthUserInfo);
    
    /**
     * 绑定OAuth账号到现有用户
     * 
     * @param userId 用户ID
     * @param provider OAuth提供商
     * @param oauthUserInfo OAuth用户信息
     * @return 更新后的用户
     */
    User bindOAuthAccount(Long userId, OAuthProvider provider, Map<String, Object> oauthUserInfo);
    
    /**
     * 解绑OAuth账号
     * 
     * @param userId 用户ID
     * @param provider OAuth提供商
     * @return 更新后的用户
     */
    User unbindOAuthAccount(Long userId, OAuthProvider provider);
    
    /**
     * 更新OAuth令牌
     * 
     * @param userId 用户ID
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresIn 过期时间（秒）
     */
    void updateOAuthTokens(Long userId, String accessToken, String refreshToken, Long expiresIn);
    
    /**
     * 同步OAuth用户信息
     * 
     * @param userId 用户ID
     * @param oauthUserInfo OAuth用户信息
     * @return 更新后的用户
     */
    User syncOAuthUserInfo(Long userId, Map<String, Object> oauthUserInfo);
}
