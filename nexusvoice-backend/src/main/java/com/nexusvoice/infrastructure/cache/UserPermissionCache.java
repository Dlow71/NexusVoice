package com.nexusvoice.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户权限缓存
 * 使用Caffeine本地缓存提升权限检查性能
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Slf4j
@Component
public class UserPermissionCache {

    /**
     * 用户权限缓存
     * Key: userId
     * Value: 权限标识列表
     * 过期时间：5分钟
     */
    private final Cache<Long, List<String>> permissionCache;

    public UserPermissionCache() {
        this.permissionCache = Caffeine.newBuilder()
                .maximumSize(10000)  // 最多缓存10000个用户的权限
                .expireAfterWrite(5, TimeUnit.MINUTES)  // 5分钟后过期
                .recordStats()  // 记录统计信息
                .build();
        
        log.info("用户权限缓存初始化完成，最大容量：10000，过期时间：5分钟");
    }

    /**
     * 获取用户权限（从缓存）
     *
     * @param userId 用户ID
     * @return 权限列表，如果缓存中不存在则返回null
     */
    public List<String> get(Long userId) {
        return permissionCache.getIfPresent(userId);
    }

    /**
     * 缓存用户权限
     *
     * @param userId 用户ID
     * @param permissions 权限列表
     */
    public void put(Long userId, List<String> permissions) {
        permissionCache.put(userId, permissions);
        log.debug("缓存用户权限，userId：{}，权限数量：{}", userId, permissions.size());
    }

    /**
     * 使失效指定用户的权限缓存
     *
     * @param userId 用户ID
     */
    public void evict(Long userId) {
        permissionCache.invalidate(userId);
        log.info("清除用户权限缓存，userId：{}", userId);
    }

    /**
     * 清空所有权限缓存
     */
    public void evictAll() {
        permissionCache.invalidateAll();
        log.info("清空所有用户权限缓存");
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息字符串
     */
    public String getStats() {
        var stats = permissionCache.stats();
        return String.format("命中率: %.2f%%, 缓存大小: %d, 命中次数: %d, 未命中次数: %d",
                stats.hitRate() * 100,
                permissionCache.estimatedSize(),
                stats.hitCount(),
                stats.missCount());
    }
}
