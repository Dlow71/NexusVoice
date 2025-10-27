
package com.nexusvoice.interfaces.api.user;

import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.application.auth.service.TokenManagementService;
import com.nexusvoice.application.user.dto.AuthResponse;
import com.nexusvoice.application.user.dto.LoginRequest;
import com.nexusvoice.application.user.dto.RegisterRequest;
import com.nexusvoice.application.user.service.AuthService;
import com.nexusvoice.common.Result;
import com.nexusvoice.domain.auth.model.UserSession;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Tag(name = "认证管理", description = "用户认证相关接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private TokenManagementService tokenManagementService;
    
    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "用户注册，返回JWT令牌")
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("用户注册请求: {}", request);
        
        try {
            AuthResponse response = authService.register(request);
            log.info("用户注册成功: {}", response.getUserInfo().getEmail());
            return Result.success("注册成功", response);
            
        } catch (BizException e) {
            log.warn("用户注册失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("用户注册异常: {}", e.getMessage(), e);
            throw BizException.of(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "注册失败，请稍后重试");
        }
    }

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户名密码登录，返回JWT令牌")
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求: {}", request);
        
        try {
            AuthResponse response = authService.login(request);
            log.info("用户登录成功: {}", response.getUserInfo().getEmail());
            return Result.success("登录成功", response);
            
        } catch (BizException e) {
            log.warn("用户登录失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("用户登录异常: {}", e.getMessage(), e);
            throw BizException.of(ErrorCodeEnum.LOGIN_FAILED, "登录失败，请稍后重试");
        }
    }
    
    /**
     * 刷新令牌
     */
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    @PostMapping("/refresh")
    public Result<AuthResponse> refreshToken(
            @Parameter(description = "刷新令牌") @RequestBody Map<String, String> request) {
        
        log.info("刷新令牌请求");
        
        try {
            String refreshToken = request.get("refreshToken");
            if (!StringUtils.hasText(refreshToken)) {
                throw BizException.of(ErrorCodeEnum.TOKEN_INVALID, "刷新令牌不能为空");
            }
            
            AuthResponse response = authService.refreshToken(refreshToken);
            log.info("令牌刷新成功");
            return Result.success("令牌刷新成功", response);
            
        } catch (BizException e) {
            log.warn("令牌刷新失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("令牌刷新异常: {}", e.getMessage(), e);
            throw BizException.of(ErrorCodeEnum.TOKEN_INVALID, "令牌刷新失败");
        }
    }
    
    /**
     * 用户登出
     */
    @Operation(summary = "用户登出", description = "用户登出，将Token加入黑名单并删除会话")
    @PostMapping("/logout")
    @RequireAuth
    public Result<Void> logout(HttpServletRequest request) {
        try {
            String username = SecurityUtils.getCurrentUsername().orElse("未知用户");
            
            // 从请求中提取Token
            String token = extractToken(request);
            if (token != null) {
                // 将Token加入黑名单并删除会话
                tokenManagementService.logout(token);
                log.info("用户登出成功: {}", username);
                return Result.success("登出成功");
            } else {
                log.warn("未找到Token，登出失败: {}", username);
                return Result.success("登出成功"); // 客户端仍然需要清除本地Token
            }
        } catch (Exception e) {
            log.error("用户登出异常: {}", e.getMessage(), e);
            return Result.success("登出成功"); // 即使服务端失败，客户端也应该清除Token
        }
    }
    
    /**
     * 登出所有设备
     */
    @Operation(summary = "登出所有设备", description = "登出用户的所有设备会话")
    @PostMapping("/logout-all")
    @RequireAuth
    public Result<Void> logoutAll() {
        try {
            Long userId = SecurityUtils.getCurrentUserId()
                    .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
            
            tokenManagementService.logoutAll(userId);
            
            log.info("用户登出所有设备成功: userId={}", userId);
            return Result.success("已登出所有设备");
            
        } catch (BizException e) {
            log.warn("登出所有设备失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("登出所有设备异常: {}", e.getMessage(), e);
            throw BizException.of(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "登出失败");
        }
    }
    
    /**
     * 获取活跃会话列表
     */
    @Operation(summary = "获取活跃会话", description = "获取当前用户的所有活跃会话")
    @GetMapping("/sessions")
    @RequireAuth
    public Result<List<SessionInfo>> getActiveSessions() {
        try {
            Long userId = SecurityUtils.getCurrentUserId()
                    .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
            
            List<UserSession> sessions = tokenManagementService.getActiveSessions(userId);
            
            List<SessionInfo> sessionInfos = sessions.stream()
                    .map(this::convertToSessionInfo)
                    .toList();
            
            return Result.success("获取成功", sessionInfos);
            
        } catch (BizException e) {
            log.warn("获取活跃会话失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取活跃会话异常: {}", e.getMessage(), e);
            throw BizException.of(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "获取失败");
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的基本信息")
    @GetMapping("/me")
    @RequireAuth
    public Result<AuthResponse.UserInfo> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
        
        String username = SecurityUtils.getCurrentUsername().orElse("");
        
        // 这里应该从数据库获取完整用户信息，暂时返回基本信息
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
        userInfo.setId(userId);
        userInfo.setEmail(username);
        userInfo.setUserType(SecurityUtils.getCurrentUserType().orElse(null));
        
        return Result.success("获取用户信息成功", userInfo);
    }
    
    /**
     * 邮箱验证
     */
    @Operation(summary = "邮箱验证", description = "验证用户邮箱")
    @PostMapping("/verify-email")
    public Result<Void> verifyEmail(
            @Parameter(description = "验证码") @RequestParam String code,
            @Parameter(description = "邮箱") @RequestParam String email) {
        
        log.info("邮箱验证请求: email={}", email);
        
        // TODO: 实现邮箱验证逻辑
        // 1. 验证验证码是否正确
        // 2. 更新用户邮箱验证状态
        
        return Result.success("邮箱验证成功");
    }
    
    /**
     * 从请求中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return request.getParameter("token");
    }
    
    /**
     * 转换为SessionInfo DTO
     */
    private SessionInfo convertToSessionInfo(UserSession session) {
        SessionInfo info = new SessionInfo();
        info.setDeviceType(session.getDeviceType());
        info.setDeviceId(session.getDeviceId());
        info.setIpAddress(session.getIpAddress());
        info.setLastActiveAt(session.getLastActiveAt());
        info.setCreatedAt(session.getCreatedAt());
        info.setCurrent(false); // 需要判断是否当前会话
        return info;
    }
    
    /**
     * 会话信息DTO
     */
    public static class SessionInfo {
        private String deviceType;
        private String deviceId;
        private String ipAddress;
        private java.time.LocalDateTime lastActiveAt;
        private java.time.LocalDateTime createdAt;
        private boolean current;
        
        public String getDeviceType() {
            return deviceType;
        }
        
        public void setDeviceType(String deviceType) {
            this.deviceType = deviceType;
        }
        
        public String getDeviceId() {
            return deviceId;
        }
        
        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }
        
        public String getIpAddress() {
            return ipAddress;
        }
        
        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }
        
        public java.time.LocalDateTime getLastActiveAt() {
            return lastActiveAt;
        }
        
        public void setLastActiveAt(java.time.LocalDateTime lastActiveAt) {
            this.lastActiveAt = lastActiveAt;
        }
        
        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
        
        public boolean isCurrent() {
            return current;
        }
        
        public void setCurrent(boolean current) {
            this.current = current;
        }
    }
}
