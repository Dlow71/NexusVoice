package com.nexusvoice.application.user.service;

import com.nexusvoice.application.user.dto.AuthResponse;
import com.nexusvoice.application.user.dto.LoginRequest;
import com.nexusvoice.application.user.dto.RegisterRequest;
import com.nexusvoice.domain.user.constant.UserStatus;
import com.nexusvoice.domain.user.constant.UserType;
import com.nexusvoice.domain.user.model.User;
import com.nexusvoice.domain.user.repository.UserRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.application.auth.service.TokenManagementService;
import com.nexusvoice.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

/**
 * 认证服务
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private TokenManagementService tokenManagementService;

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 认证响应
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return register(request, getHttpServletRequest());
    }
    
    /**
     * 用户注册（带HttpServletRequest）
     *
     * @param request 注册请求
     * @param httpRequest HTTP请求
     * @return 认证响应
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("用户注册请求: {}", request);

        // 验证请求参数
        validateRegisterRequest(request);

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BizException.of(ErrorCodeEnum.USER_EMAIL_EXISTS, "该邮箱已被注册");
        }

        // 检查手机号是否已存在
        if (StringUtils.hasText(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw BizException.of(ErrorCodeEnum.USER_PHONE_EXISTS, "该手机号已被注册");
        }

        // 创建用户
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setUserType(UserType.USER);
        user.setStatus(UserStatus.NORMAL);
        user.setEmailVerified(0);

        // 保存用户
        User savedUser = userRepository.save(user);
        log.info("用户注册成功: {}", savedUser.getId());

        // 生成令牌并返回（创建会话）
        return generateAuthResponse(savedUser, httpRequest);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 认证响应
     */
    public AuthResponse login(LoginRequest request) {
        return login(request, getHttpServletRequest());
    }
    
    /**
     * 用户登录（带HttpServletRequest）
     *
     * @param request 登录请求
     * @param httpRequest HTTP请求
     * @return 认证响应
     */
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("用户登录请求: {}", request);

        // 验证请求参数
        validateLoginRequest(request);

        // 查找用户
        User user = findUserByUsername(request.getUsername());

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("用户 {} 登录密码错误", request.getUsername());
            throw BizException.of(ErrorCodeEnum.USER_PASSWORD_ERROR, "用户名或密码错误");
        }

        // 检查用户状态
        if (!user.isAccountNormal()) {
            if (user.isBanned()) {
                throw BizException.of(ErrorCodeEnum.USER_BANNED, "账户已被封禁，请联系管理员");
            } else {
                throw BizException.of(ErrorCodeEnum.USER_STATUS_ABNORMAL, "账户状态异常，无法登录");
            }
        }

        // 更新最后登录时间
        user.updateLastLoginTime();
        userRepository.update(user);

        log.info("用户 {} 登录成功", user.getEmail());

        // 生成令牌并返回（创建会话）
        return generateAuthResponse(user, httpRequest);
    }

    /**
     * 管理员登录
     *
     * @param request 登录请求
     * @return 认证响应
     */
    public AuthResponse adminLogin(LoginRequest request) {
        return adminLogin(request, getHttpServletRequest());
    }
    
    /**
     * 管理员登录（带HttpServletRequest）
     *
     * @param request 登录请求
     * @param httpRequest HTTP请求
     * @return 认证响应
     */
    public AuthResponse adminLogin(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("管理员登录请求: {}", request);

        // 验证请求参数
        validateLoginRequest(request);

        // 查找用户
        User user = findUserByUsername(request.getUsername());

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("管理员 {} 登录密码错误", request.getUsername());
            throw BizException.of(ErrorCodeEnum.USER_PASSWORD_ERROR, "用户名或密码错误");
        }

        // ✅ 验证用户类型必须是管理员
        if (!UserType.ADMIN.equals(user.getUserType())) {
            log.warn("用户 {} 尝试使用管理员接口登录，但不是管理员", request.getUsername());
            throw BizException.of(ErrorCodeEnum.ADMIN_ACCESS_DENIED, "需要管理员权限才能登录后台管理系统");
        }

        // 检查用户状态
        if (!user.isAccountNormal()) {
            if (user.isBanned()) {
                throw BizException.of(ErrorCodeEnum.USER_BANNED, "账户已被封禁，请联系管理员");
            } else {
                throw BizException.of(ErrorCodeEnum.USER_STATUS_ABNORMAL, "账户状态异常，无法登录");
            }
        }

        // 更新最后登录时间
        user.updateLastLoginTime();
        userRepository.update(user);

        log.info("管理员 {} 登录成功", user.getEmail());

        // 生成令牌并返回（创建会话）
        return generateAuthResponse(user, httpRequest);
    }

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 认证响应
     */
    public AuthResponse refreshToken(String refreshToken) {
        return refreshToken(refreshToken, getHttpServletRequest());
    }
    
    /**
     * 刷新令牌（带HttpServletRequest）
     *
     * @param refreshToken 刷新令牌
     * @param httpRequest HTTP请求
     * @return 认证响应
     */
    public AuthResponse refreshToken(String refreshToken, HttpServletRequest httpRequest) {
        log.info("刷新令牌请求");

        if (!StringUtils.hasText(refreshToken)) {
            throw BizException.of(ErrorCodeEnum.TOKEN_INVALID, "刷新令牌不能为空");
        }

        try {
            // 验证刷新令牌
            if (!jwtUtils.validateToken(refreshToken) || !jwtUtils.isRefreshToken(refreshToken)) {
                throw BizException.of(ErrorCodeEnum.TOKEN_INVALID, "无效的刷新令牌");
            }

            // 从令牌中获取用户信息
            Long userId = jwtUtils.getUserIdFromToken(refreshToken);
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isEmpty()) {
                throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
            }

            User user = userOpt.get();

            // 检查用户状态
            if (!user.isAccountNormal()) {
                throw BizException.of(ErrorCodeEnum.USER_STATUS_ABNORMAL, "账户状态异常");
            }

            log.info("令牌刷新成功，用户: {}", user.getEmail());

            // 生成新的令牌（刷新会话）
            AuthResponse response = generateAuthResponse(user, httpRequest);
            
            // 处理旧会话：将旧Token加入黑名单并删除会话
            // 从refreshToken提取旧的accessToken（需要从会话中查找）
            tokenManagementService.refreshSession(
                extractOldAccessToken(refreshToken), 
                response.getAccessToken(), 
                response.getRefreshToken(), 
                httpRequest
            );
            
            return response;

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("刷新令牌失败: {}", e.getMessage(), e);
            throw BizException.of(ErrorCodeEnum.TOKEN_INVALID, "令牌刷新失败");
        }
    }

    /**
     * 验证注册请求
     */
    private void validateRegisterRequest(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "两次输入的密码不一致");
        }
    }

    /**
     * 验证登录请求
     */
    private void validateLoginRequest(LoginRequest request) {
        // 可以在这里添加更多验证逻辑
    }

    /**
     * 根据用户名查找用户
     */
    private User findUserByUsername(String username) {
        Optional<User> userOpt;

        // 判断是邮箱还是手机号
        if (username.contains("@")) {
            userOpt = userRepository.findByEmail(username);
        } else {
            userOpt = userRepository.findByPhone(username);
        }

        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }

        return userOpt.get();
    }

    /**
     * 提取旧的AccessToken（从RefreshToken对应的会话）
     */
    private String extractOldAccessToken(String refreshToken) {
        // 这里需要从会话仓储中查找
        // 简化实现：直接返回refreshToken，TokenManagementService会处理
        return refreshToken;
    }
    
    /**
     * 生成认证响应
     */
    private AuthResponse generateAuthResponse(User user) {
        return generateAuthResponse(user, null);
    }
    
    /**
     * 生成认证响应（带HttpServletRequest）
     */
    private AuthResponse generateAuthResponse(User user, HttpServletRequest httpRequest) {
        // 生成访问令牌
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), user.getUserType());
        
        // 生成刷新令牌
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getEmail());

        // 获取令牌过期时间
        Date expirationDate = jwtUtils.getExpirationFromToken(accessToken);
        LocalDateTime expiresAt = expirationDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        // 构建用户信息
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getUserType(),
                user.getEmailVerified()
        );
        
        // 临时方案：根据用户类型返回固定角色
        // TODO: 未来实现完整RBAC权限系统后，从数据库查询用户实际角色
        java.util.List<String> roles = new java.util.ArrayList<>();
        if (user.getUserType() != null && user.getUserType().isAdmin()) {
            roles.add("admin"); // 管理员角色
        } else {
            roles.add("user"); // 普通用户角色
        }
        userInfo.setRoles(roles);
        
        // 创建会话失败不应阻塞登录/注册主流程，避免Redis或序列化异常导致认证整体不可用
        if (httpRequest != null) {
            try {
                tokenManagementService.createSession(user.getId(), accessToken, refreshToken, httpRequest);
            } catch (Exception e) {
                log.error("创建用户会话失败，降级为仅返回令牌: userId={}", user.getId(), e);
            }
        }

        return new AuthResponse(accessToken, refreshToken, expiresAt, userInfo);
    }
    
    /**
     * 获取当前HTTP请求
     */
    private HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
