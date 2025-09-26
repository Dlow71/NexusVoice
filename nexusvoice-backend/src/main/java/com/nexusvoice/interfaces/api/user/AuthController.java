
package com.nexusvoice.interfaces.api.user;

import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.application.user.dto.AuthResponse;
import com.nexusvoice.application.user.dto.LoginRequest;
import com.nexusvoice.application.user.dto.RegisterRequest;
import com.nexusvoice.application.user.service.AuthService;
import com.nexusvoice.common.Result;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "用户登出", description = "用户登出（客户端需要清除本地令牌）")
    @PostMapping("/logout")
    @RequireAuth
    public Result<Void> logout() {
        // JWT是无状态的，服务端不需要做什么，客户端清除令牌即可
        // 如果需要实现令牌黑名单，可以在这里添加逻辑
        
        String username = SecurityUtils.getCurrentUsername().orElse("未知用户");
        log.info("用户登出: {}", username);
        
        return Result.success("登出成功");
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
}
