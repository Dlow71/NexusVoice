package com.nexusvoice.infrastructure.security;

import com.nexusvoice.application.rbac.MenuService;
import com.nexusvoice.infrastructure.cache.UserPermissionCache;
import com.nexusvoice.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限检查器
 * 负责检查用户是否拥有指定权限
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Slf4j
@Component
public class PermissionChecker {

    @Autowired
    private MenuService menuService;

    @Autowired
    private UserPermissionCache permissionCache;

    /**
     * 检查当前用户是否拥有所有指定的权限（AND关系）
     *
     * @param permissions 权限标识数组
     * @return 是否拥有所有权限
     */
    public boolean hasAllPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }

        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("无法获取当前用户ID"));

        // 获取用户的所有权限（带缓存）
        List<String> userPermissions = getUserPermissions(userId);
        Set<String> userPermissionSet = userPermissions.stream()
                .collect(Collectors.toSet());

        // 检查是否拥有所有权限
        return Arrays.stream(permissions)
                .allMatch(userPermissionSet::contains);
    }

    /**
     * 检查当前用户是否拥有任一指定的权限（OR关系）
     *
     * @param permissions 权限标识数组
     * @return 是否拥有任一权限
     */
    public boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }

        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("无法获取当前用户ID"));

        // 获取用户的所有权限（带缓存）
        List<String> userPermissions = getUserPermissions(userId);
        Set<String> userPermissionSet = userPermissions.stream()
                .collect(Collectors.toSet());

        // 检查是否拥有任一权限
        return Arrays.stream(permissions)
                .anyMatch(userPermissionSet::contains);
    }

    /**
     * 检查当前用户是否拥有指定权限
     *
     * @param permission 权限标识
     * @return 是否拥有权限
     */
    public boolean hasPermission(String permission) {
        return hasAllPermissions(permission);
    }

    /**
     * 检查当前用户是否为管理员（管理员拥有所有权限）
     *
     * @return 是否为管理员
     */
    public boolean isAdmin() {
        return SecurityUtils.isAdmin();
    }

    /**
     * 获取用户权限（带缓存）
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    private List<String> getUserPermissions(Long userId) {
        // 先从缓存获取
        List<String> cached = permissionCache.get(userId);
        if (cached != null) {
            log.debug("从缓存获取用户权限，userId：{}", userId);
            return cached;
        }

        // 缓存未命中，从数据库加载
        log.debug("缓存未命中，从数据库加载用户权限，userId：{}", userId);
        List<String> permissions = menuService.getUserPermissions(userId);
        
        // 写入缓存
        permissionCache.put(userId, permissions);
        
        return permissions;
    }

    /**
     * 清除用户权限缓存
     *
     * @param userId 用户ID
     */
    public void evictUserPermissionCache(Long userId) {
        permissionCache.evict(userId);
    }

    /**
     * 清除所有用户权限缓存
     */
    public void evictAllPermissionCache() {
        permissionCache.evictAll();
    }
}
