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
import com.nexusvoice.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 认证响应
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
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
        user.setEmailVerified(false);

        // 保存用户
        User savedUser = userRepository.save(user);
        log.info("用户注册成功: {}", savedUser.getId());

        // 生成令牌并返回
        return generateAuthResponse(savedUser);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 认证响应
     */
    public AuthResponse login(LoginRequest request) {
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

        // 生成令牌并返回
        return generateAuthResponse(user);
    }

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 认证响应
     */
    public AuthResponse refreshToken(String refreshToken) {
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
            String userId = jwtUtils.getUserIdFromToken(refreshToken);
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

            // 生成新的令牌
            return generateAuthResponse(user);

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
     * 生成认证响应
     */
    private AuthResponse generateAuthResponse(User user) {
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

        return new AuthResponse(accessToken, refreshToken, expiresAt, userInfo);
    }
}
