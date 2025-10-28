package com.nexusvoice.interfaces.api.admin;

import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.application.auth.service.TokenManagementService;
import com.nexusvoice.application.user.dto.AuthResponse;
import com.nexusvoice.application.user.dto.LoginRequest;
import com.nexusvoice.application.user.service.AuthService;
import com.nexusvoice.common.Result;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员认证控制器
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Tag(name = "管理员认证", description = "管理员登录、登出等认证接口")
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminAuthController.class);
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private TokenManagementService tokenManagementService;
    
    /**
     * 管理员登录
     */
    @Operation(summary = "管理员登录", description = "管理员用户名密码登录，返回JWT令牌")
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("管理员登录请求: {}", request);
        
        try {
            AuthResponse response = authService.adminLogin(request);
            log.info("管理员登录成功: {}", response.getUserInfo().getEmail());
            return Result.success("登录成功", response);
            
        } catch (BizException e) {
            log.warn("管理员登录失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("管理员登录异常: {}", e.getMessage(), e);
            throw BizException.of(ErrorCodeEnum.LOGIN_FAILED, "登录失败，请稍后重试");
        }
    }
    
    /**
     * 管理员登出
     */
    @Operation(summary = "管理员登出", description = "管理员登出，将Token加入黑名单并删除会话")
    @PostMapping("/logout")
    @RequireAuth
    public Result<Void> logout(HttpServletRequest request) {
        try {
            String username = SecurityUtils.getCurrentUsername().orElse("未知管理员");
            
            // 从请求中提取Token
            String token = extractToken(request);
            if (token != null) {
                // 将Token加入黑名单并删除会话
                tokenManagementService.logout(token);
                log.info("管理员登出成功: {}", username);
                return Result.success("登出成功");
            } else {
                log.warn("未找到Token，登出失败: {}", username);
                return Result.success("登出成功"); // 客户端仍然需要清除本地Token
            }
        } catch (Exception e) {
            log.error("管理员登出异常: {}", e.getMessage(), e);
            return Result.success("登出成功"); // 即使服务端失败，客户端也应该清除Token
        }
    }
    
    /**
     * 获取当前管理员信息
     */
    @Operation(summary = "获取当前管理员信息", description = "获取当前登录管理员的基本信息")
    @GetMapping("/me")
    @RequireAuth
    public Result<AuthResponse.UserInfo> getCurrentAdmin() {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
        
        String username = SecurityUtils.getCurrentUsername().orElse("");
        
        // 这里应该从数据库获取完整用户信息，暂时返回基本信息
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
        userInfo.setId(userId);
        userInfo.setEmail(username);
        userInfo.setUserType(SecurityUtils.getCurrentUserType().orElse(null));
        
        return Result.success("获取管理员信息成功", userInfo);
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
}
