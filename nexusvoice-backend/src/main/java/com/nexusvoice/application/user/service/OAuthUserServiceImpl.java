package com.nexusvoice.application.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.user.constant.OAuthProvider;
import com.nexusvoice.domain.user.constant.UserStatus;
import com.nexusvoice.domain.user.constant.UserType;
import com.nexusvoice.domain.user.model.User;
import com.nexusvoice.domain.user.repository.UserRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth用户服务实现
 * 
 * @author NexusVoice
 * @since 2025-01-20
 */
@Service
public class OAuthUserServiceImpl implements OAuthUserService {
    
    private static final Logger log = LoggerFactory.getLogger(OAuthUserServiceImpl.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public User findOrCreateOAuthUser(OAuthProvider provider, Map<String, Object> oauthUserInfo) {
        log.info("处理OAuth用户登录，提供商：{}，用户信息：{}", provider, oauthUserInfo);
        
        // 1. 验证OAuth提供商是否支持
        if (!provider.isSupported()) {
            log.error("不支持的OAuth提供商：{}", provider);
            throw BizException.of(ErrorCodeEnum.OAUTH_PROVIDER_NOT_SUPPORTED, 
                    "暂不支持" + provider.getName() + "登录");
        }
        
        // 2. 提取OAuth用户信息
        String oauthId = extractOAuthId(provider, oauthUserInfo);
        String email = extractEmail(oauthUserInfo);
        String username = extractUsername(oauthUserInfo);
        String avatarUrl = extractAvatarUrl(oauthUserInfo);
        
        log.info("提取OAuth用户信息 - ID：{}，邮箱：{}，用户名：{}", oauthId, email, username);
        
        // 3. 查找是否已存在OAuth绑定的用户
        Optional<User> existingOAuthUser = userRepository.findByOAuthProviderAndId(
                provider.getCode(), oauthId);
        
        if (existingOAuthUser.isPresent()) {
            User user = existingOAuthUser.get();
            log.info("找到已绑定的OAuth用户，用户ID：{}，邮箱：{}", user.getId(), user.getEmail());
            
            // 更新最后登录时间和头像（如果有变化）
            user.updateLastLoginTime();
            if (StringUtils.hasText(avatarUrl) && !avatarUrl.equals(user.getOauthAvatarUrl())) {
                user.setOauthAvatarUrl(avatarUrl);
                // 如果用户没有设置过头像，也更新主头像
                if (!StringUtils.hasText(user.getAvatarUrl())) {
                    user.setAvatarUrl(avatarUrl);
                }
            }
            
            // 保存原始OAuth数据
            try {
                user.setOauthRawData(objectMapper.writeValueAsString(oauthUserInfo));
            } catch (Exception e) {
                log.warn("序列化OAuth原始数据失败：{}", e.getMessage());
            }
            
            return userRepository.update(user);
        }
        
        // 4. 如果邮箱已存在，检查是否可以自动绑定
        if (StringUtils.hasText(email)) {
            Optional<User> emailUser = userRepository.findByEmail(email);
            if (emailUser.isPresent()) {
                User user = emailUser.get();
                log.info("邮箱{}已存在，用户ID：{}", email, user.getId());
                
                // 检查该用户是否已绑定其他OAuth账号
                if (user.isOAuthUser()) {
                    log.error("用户{}已绑定其他OAuth账号：{}", user.getId(), user.getOauthProvider());
                    throw BizException.of(ErrorCodeEnum.OAUTH_EMAIL_ALREADY_BOUND,
                            "该邮箱已绑定其他" + user.getOauthProvider().getName() + "账号");
                }
                
                // 自动绑定到现有账号（可配置为需要用户确认）
                log.info("自动绑定OAuth账号到现有用户：{}", user.getId());
                user.bindOAuth(provider, oauthId, username);
                user.setOauthAvatarUrl(avatarUrl);
                
                // 如果用户没有头像，使用OAuth头像
                if (!StringUtils.hasText(user.getAvatarUrl()) && StringUtils.hasText(avatarUrl)) {
                    user.setAvatarUrl(avatarUrl);
                }
                
                // 保存原始OAuth数据
                try {
                    user.setOauthRawData(objectMapper.writeValueAsString(oauthUserInfo));
                } catch (Exception e) {
                    log.warn("序列化OAuth原始数据失败：{}", e.getMessage());
                }
                
                user.updateLastLoginTime();
                return userRepository.update(user);
            }
        }
        
        // 5. 创建新用户
        log.info("创建新的OAuth用户");
        User newUser = new User();
        
        // 设置OAuth信息
        newUser.setOauthProvider(provider);
        newUser.setOauthId(oauthId);
        newUser.setOauthUsername(username);
        newUser.setOauthAvatarUrl(avatarUrl);
        newUser.setOauthBindTime(LocalDateTime.now());
        
        // 设置基本信息
        newUser.setEmail(email != null ? email : generateOAuthEmail(provider, oauthId));
        newUser.setNickname(username != null ? username : provider.getName() + "用户");
        newUser.setAvatarUrl(avatarUrl);
        newUser.setEmailVerified(email != null ? 1 : 0); // 如果OAuth提供了邮箱，认为已验证
        newUser.setUserType(UserType.USER);
        newUser.setStatus(UserStatus.NORMAL);
        newUser.setLastLoginAt(LocalDateTime.now());
        
        // 保存原始OAuth数据
        try {
            newUser.setOauthRawData(objectMapper.writeValueAsString(oauthUserInfo));
        } catch (Exception e) {
            log.warn("序列化OAuth原始数据失败：{}", e.getMessage());
        }
        
        User savedUser = userRepository.save(newUser);
        log.info("OAuth用户创建成功，用户ID：{}，邮箱：{}", savedUser.getId(), savedUser.getEmail());
        
        return savedUser;
    }
    
    @Override
    @Transactional
    public User bindOAuthAccount(Long userId, OAuthProvider provider, Map<String, Object> oauthUserInfo) {
        log.info("绑定OAuth账号，用户ID：{}，提供商：{}", userId, provider);
        
        // 1. 查找用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在"));
        
        // 2. 检查是否已绑定OAuth
        if (user.isOAuthUser()) {
            if (user.getOauthProvider() == provider) {
                log.warn("用户{}已绑定{}账号", userId, provider);
                throw BizException.of(ErrorCodeEnum.OAUTH_BIND_FAILED, 
                        "您已绑定" + provider.getName() + "账号");
            } else {
                log.warn("用户{}已绑定其他OAuth账号：{}", userId, user.getOauthProvider());
                throw BizException.of(ErrorCodeEnum.OAUTH_BIND_FAILED,
                        "您已绑定" + user.getOauthProvider().getName() + "账号，请先解绑");
            }
        }
        
        // 3. 提取OAuth信息
        String oauthId = extractOAuthId(provider, oauthUserInfo);
        String username = extractUsername(oauthUserInfo);
        String avatarUrl = extractAvatarUrl(oauthUserInfo);
        
        // 4. 检查OAuth账号是否已被其他用户绑定
        if (userRepository.existsByOAuthProviderAndId(provider.getCode(), oauthId)) {
            log.error("OAuth账号已被其他用户绑定，提供商：{}，OAuth ID：{}", provider, oauthId);
            throw BizException.of(ErrorCodeEnum.OAUTH_ACCOUNT_ALREADY_BOUND,
                    "该" + provider.getName() + "账号已被其他用户绑定");
        }
        
        // 5. 绑定OAuth账号
        user.bindOAuth(provider, oauthId, username);
        user.setOauthAvatarUrl(avatarUrl);
        
        // 保存原始OAuth数据
        try {
            user.setOauthRawData(objectMapper.writeValueAsString(oauthUserInfo));
        } catch (Exception e) {
            log.warn("序列化OAuth原始数据失败：{}", e.getMessage());
        }
        
        User updatedUser = userRepository.update(user);
        log.info("OAuth账号绑定成功，用户ID：{}，提供商：{}", userId, provider);
        
        return updatedUser;
    }
    
    @Override
    @Transactional
    public User unbindOAuthAccount(Long userId, OAuthProvider provider) {
        log.info("解绑OAuth账号，用户ID：{}，提供商：{}", userId, provider);
        
        // 1. 查找用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在"));
        
        // 2. 检查是否绑定了指定的OAuth账号
        if (!user.isOAuthUser() || user.getOauthProvider() != provider) {
            log.warn("用户{}未绑定{}账号", userId, provider);
            throw BizException.of(ErrorCodeEnum.OAUTH_UNBIND_FAILED,
                    "您未绑定" + provider.getName() + "账号");
        }
        
        // 3. 检查是否可以解绑（必须有密码）
        if (!user.isPasswordUser()) {
            log.error("用户{}没有设置密码，无法解绑OAuth", userId);
            throw BizException.of(ErrorCodeEnum.OAUTH_UNBIND_NO_PASSWORD,
                    "解绑失败：请先设置登录密码");
        }
        
        // 4. 解绑OAuth账号
        user.unbindOAuth();
        User updatedUser = userRepository.update(user);
        log.info("OAuth账号解绑成功，用户ID：{}，提供商：{}", userId, provider);
        
        return updatedUser;
    }
    
    @Override
    @Transactional
    public void updateOAuthTokens(Long userId, String accessToken, String refreshToken, Long expiresIn) {
        log.debug("更新OAuth令牌，用户ID：{}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在"));
        
        if (!user.isOAuthUser()) {
            log.warn("用户{}不是OAuth用户，无法更新令牌", userId);
            return;
        }
        
        // 计算过期时间
        LocalDateTime expiresAt = null;
        if (expiresIn != null && expiresIn > 0) {
            expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
        }
        
        user.updateOAuthTokens(accessToken, refreshToken, expiresAt);
        userRepository.update(user);
        
        log.debug("OAuth令牌更新成功，用户ID：{}", userId);
    }
    
    @Override
    @Transactional
    public User syncOAuthUserInfo(Long userId, Map<String, Object> oauthUserInfo) {
        log.info("同步OAuth用户信息，用户ID：{}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在"));
        
        if (!user.isOAuthUser()) {
            log.warn("用户{}不是OAuth用户，无法同步信息", userId);
            throw BizException.of(ErrorCodeEnum.OAUTH_USER_NOT_FOUND, "用户未绑定OAuth账号");
        }
        
        // 提取并更新信息
        String username = extractUsername(oauthUserInfo);
        String avatarUrl = extractAvatarUrl(oauthUserInfo);
        
        if (StringUtils.hasText(username)) {
            user.setOauthUsername(username);
        }
        
        if (StringUtils.hasText(avatarUrl)) {
            user.setOauthAvatarUrl(avatarUrl);
            // 如果用户没有设置过头像，也更新主头像
            if (!StringUtils.hasText(user.getAvatarUrl())) {
                user.setAvatarUrl(avatarUrl);
            }
        }
        
        // 保存原始OAuth数据
        try {
            user.setOauthRawData(objectMapper.writeValueAsString(oauthUserInfo));
        } catch (Exception e) {
            log.warn("序列化OAuth原始数据失败：{}", e.getMessage());
        }
        
        User updatedUser = userRepository.update(user);
        log.info("OAuth用户信息同步成功，用户ID：{}", userId);
        
        return updatedUser;
    }
    
    /**
     * 提取OAuth用户ID
     */
    private String extractOAuthId(OAuthProvider provider, Map<String, Object> userInfo) {
        String oauthId = null;
        
        switch (provider) {
            case GITHUB:
                // GitHub用户ID是数字，但我们转换为字符串存储
                Object idObj = userInfo.get("id");
                if (idObj != null) {
                    oauthId = String.valueOf(idObj);
                }
                break;
            case GOOGLE:
                oauthId = (String) userInfo.get("sub");
                break;
            default:
                oauthId = (String) userInfo.get("id");
        }
        
        if (!StringUtils.hasText(oauthId)) {
            log.error("无法提取OAuth用户ID，提供商：{}，用户信息：{}", provider, userInfo);
            throw BizException.of(ErrorCodeEnum.OAUTH_USER_INFO_FETCH_FAILED,
                    "无法获取" + provider.getName() + "用户ID");
        }
        
        return oauthId;
    }
    
    /**
     * 提取邮箱
     */
    private String extractEmail(Map<String, Object> userInfo) {
        return (String) userInfo.get("email");
    }
    
    /**
     * 提取用户名
     */
    private String extractUsername(Map<String, Object> userInfo) {
        // 优先使用name，其次是login（GitHub），最后是email的前缀
        String username = (String) userInfo.get("name");
        if (!StringUtils.hasText(username)) {
            username = (String) userInfo.get("login");
        }
        if (!StringUtils.hasText(username)) {
            String email = extractEmail(userInfo);
            if (StringUtils.hasText(email) && email.contains("@")) {
                username = email.substring(0, email.indexOf("@"));
            }
        }
        return username;
    }
    
    /**
     * 提取头像URL
     */
    private String extractAvatarUrl(Map<String, Object> userInfo) {
        String avatarUrl = (String) userInfo.get("avatar_url");
        if (!StringUtils.hasText(avatarUrl)) {
            avatarUrl = (String) userInfo.get("picture");
        }
        return avatarUrl;
    }
    
    /**
     * 生成OAuth用户的邮箱（当OAuth没有提供邮箱时）
     */
    private String generateOAuthEmail(OAuthProvider provider, String oauthId) {
        // 生成一个唯一的邮箱地址，格式：oauth_github_123456@nexusvoice.local
        return String.format("oauth_%s_%s@nexusvoice.local", 
                provider.getCode().toLowerCase(), oauthId);
    }
}
