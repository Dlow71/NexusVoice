package com.nexusvoice.infrastructure.config;

import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.security.JwtAuthenticationFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 用户上下文工具类
 * 用于获取当前登录用户信息
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
public class UserContext {

    /**
     * 获取当前用户ID
     * 
     * @return 用户ID
     * @throws BizException 如果用户未登录
     */
    public static String getCurrentUserId() {
        JwtAuthenticationFilter.UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (userPrincipal == null) {
            throw BizException.of(ErrorCodeEnum.UNAUTHORIZED, "用户未登录");
        }
        return userPrincipal.getUserId();
    }

    /**
     * 获取当前用户名
     * 
     * @return 用户名
     * @throws BizException 如果用户未登录
     */
    public static String getCurrentUsername() {
        JwtAuthenticationFilter.UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (userPrincipal == null) {
            throw BizException.of(ErrorCodeEnum.UNAUTHORIZED, "用户未登录");
        }
        return userPrincipal.getUsername();
    }

    /**
     * 获取当前用户主体对象
     * 
     * @return 用户主体对象，如果未登录返回null
     */
    public static JwtAuthenticationFilter.UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtAuthenticationFilter.UserPrincipal) {
            return (JwtAuthenticationFilter.UserPrincipal) principal;
        }

        return null;
    }

    /**
     * 检查用户是否已登录
     * 
     * @return true-已登录，false-未登录
     */
    public static boolean isAuthenticated() {
        return getCurrentUserPrincipal() != null;
    }

    /**
     * 获取当前用户ID（可选）
     * 
     * @return 用户ID，如果未登录返回null
     */
    public static String getCurrentUserIdOptional() {
        JwtAuthenticationFilter.UserPrincipal userPrincipal = getCurrentUserPrincipal();
        return userPrincipal != null ? userPrincipal.getUserId() : null;
    }

    /**
     * 获取当前用户名（可选）
     * 
     * @return 用户名，如果未登录返回null
     */
    public static String getCurrentUsernameOptional() {
        JwtAuthenticationFilter.UserPrincipal userPrincipal = getCurrentUserPrincipal();
        return userPrincipal != null ? userPrincipal.getUsername() : null;
    }
}
