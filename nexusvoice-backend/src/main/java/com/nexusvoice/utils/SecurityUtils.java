package com.nexusvoice.utils;

import com.nexusvoice.domain.user.constant.UserType;
import com.nexusvoice.security.JwtAuthenticationFilter.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Optional;

/**
 * 安全工具类
 * 
 * @author NexusVoice
 * @since 2025-09-22
 */
public class SecurityUtils {
    
    /**
     * 获取当前认证信息
     * 
     * @return 认证信息
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }
    
    /**
     * 获取当前用户主体
     * 
     * @return 用户主体
     */
    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        return getCurrentAuthentication()
                .map(Authentication::getPrincipal)
                .filter(UserPrincipal.class::isInstance)
                .map(UserPrincipal.class::cast);
    }
    
    /**
     * 获取当前用户ID
     * 
     * @return 用户ID
     */
    public static Optional<String> getCurrentUserId() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getUserId);
    }
    
    /**
     * 获取当前用户名
     * 
     * @return 用户名
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getUsername);
    }
    
    /**
     * 获取当前用户权限
     * 
     * @return 权限集合
     */
    public static Collection<? extends GrantedAuthority> getCurrentUserAuthorities() {
        return getCurrentAuthentication()
                .map(Authentication::getAuthorities)
                .orElse(null);
    }
    
    /**
     * 检查当前用户是否已认证
     * 
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        return getCurrentAuthentication()
                .map(Authentication::isAuthenticated)
                .orElse(false);
    }
    
    /**
     * 检查当前用户是否具有指定角色
     * 
     * @param role 角色名称
     * @return 是否具有角色
     */
    public static boolean hasRole(String role) {
        Collection<? extends GrantedAuthority> authorities = getCurrentUserAuthorities();
        if (authorities == null) {
            return false;
        }
        
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));
    }
    
    /**
     * 检查当前用户是否具有指定权限
     * 
     * @param permission 权限名称
     * @return 是否具有权限
     */
    public static boolean hasPermission(String permission) {
        Collection<? extends GrantedAuthority> authorities = getCurrentUserAuthorities();
        if (authorities == null) {
            return false;
        }
        
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(permission));
    }
    
    /**
     * 检查当前用户是否具有任意一个指定角色
     * 
     * @param roles 角色数组
     * @return 是否具有任意角色
     */
    public static boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查当前用户是否具有任意一个指定权限
     * 
     * @param permissions 权限数组
     * @return 是否具有任意权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查当前用户是否为管理员
     * 
     * @return 是否为管理员
     */
    public static boolean isAdmin() {
        return hasRole("ROLE_" + UserType.ADMIN.getCode());
    }
    
    /**
     * 检查当前用户是否为普通用户
     * 
     * @return 是否为普通用户
     */
    public static boolean isUser() {
        return hasRole("ROLE_" + UserType.USER.getCode());
    }
    
    /**
     * 获取当前用户类型
     * 
     * @return 用户类型
     */
    public static Optional<UserType> getCurrentUserType() {
        Collection<? extends GrantedAuthority> authorities = getCurrentUserAuthorities();
        if (authorities == null) {
            return Optional.empty();
        }
        
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5)) // 移除 "ROLE_" 前缀
                .map(code -> {
                    try {
                        return UserType.fromCode(code);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(userType -> userType != null)
                .findFirst();
    }
}
