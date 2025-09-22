package com.nexusvoice.api;

import com.nexusvoice.common.Result;
import com.nexusvoice.dto.LoginRequest;
import com.nexusvoice.dto.LoginResponse;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.JwtUtils;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 
 * @author NexusVoice
 * @since 2025-09-22
 */
@Tag(name = "认证管理", description = "用户认证相关接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;
    
    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户名密码登录，返回JWT令牌")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求: {}", request);
        
        try {
            // TODO: 这里应该从数据库验证用户信息，暂时使用硬编码演示
            if (!validateUser(request.getUsername(), request.getPassword())) {
                throw BizException.of(ErrorCodeEnum.USER_PASSWORD_ERROR, "用户名或密码错误");
            }
            
            // 模拟用户信息
            Long userId = 1L;
            String username = request.getUsername();
            String roles = "ROLE_USER,ROLE_ADMIN";
            
            // 生成令牌
            String accessToken = jwtUtils.generateAccessToken(userId, username, roles);
            String refreshToken = jwtUtils.generateRefreshToken(userId, username);
            
            // 构建响应
            LoginResponse response = new LoginResponse(
                accessToken, 
                refreshToken, 
                jwtExpiration / 1000, // 转换为秒
                userId, 
                username, 
                roles
            );
            
            log.info("用户登录成功: userId={}, username={}", userId, username);
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
    public Result<Map<String, Object>> refreshToken(
            @Parameter(description = "刷新令牌") @RequestParam String refreshToken) {
        
        log.info("刷新令牌请求");
        
        try {
            // 验证刷新令牌
            if (!jwtUtils.validateToken(refreshToken) || !jwtUtils.isRefreshToken(refreshToken)) {
                throw BizException.of(ErrorCodeEnum.TOKEN_INVALID, "无效的刷新令牌");
            }
            
            // 生成新的访问令牌
            String newAccessToken = jwtUtils.refreshAccessToken(refreshToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtExpiration / 1000);
            
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
    public Result<Map<String, Object>> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
        
        String username = SecurityUtils.getCurrentUsername().orElse("");
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("username", username);
        userInfo.put("authorities", SecurityUtils.getCurrentUserAuthorities());
        
        return Result.success("获取用户信息成功", userInfo);
    }
    
    /**
     * 验证用户凭据（临时实现）
     * TODO: 后续集成用户服务后替换
     */
    private boolean validateUser(String username, String password) {
        // 临时硬编码用户验证
        Map<String, String> users = Map.of(
            "admin", passwordEncoder.encode("123456"),
            "user", passwordEncoder.encode("123456"),
            "test", passwordEncoder.encode("test123")
        );
        
        String encodedPassword = users.get(username);
        return encodedPassword != null && passwordEncoder.matches(password, encodedPassword);
    }
}
