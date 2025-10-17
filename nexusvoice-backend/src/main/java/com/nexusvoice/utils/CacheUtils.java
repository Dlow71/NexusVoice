package com.nexusvoice.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存工具类
 * 整合Caffeine本地缓存和Redis分布式缓存，实现二级缓存机制
 * 提供缓存穿透、缓存击穿、缓存雪崩的防护
 *
 * @author NexusVoice
 * @since 2025-10-17
 */
@Component
public class CacheUtils {

    private static final Logger log = LoggerFactory.getLogger(CacheUtils.class);

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedissonUtils redissonUtils;

    /**
     * 空值标记，用于防止缓存穿透
     */
    private static final String NULL_VALUE = "NULL_VALUE_PLACEHOLDER";

    /**
     * 本地缓存默认最大容量
     */
    private static final long DEFAULT_MAXIMUM_SIZE = 10000;

    /**
     * 本地缓存默认过期时间（秒）
     */
    private static final long DEFAULT_EXPIRE_SECONDS = 300;

    /**
     * 本地缓存容器
     */
    private final ConcurrentHashMap<String, Cache<String, Object>> localCaches = new ConcurrentHashMap<>();

    /**
     * 默认本地缓存
     */
    private final Cache<String, Object> defaultLocalCache = Caffeine.newBuilder()
            .maximumSize(DEFAULT_MAXIMUM_SIZE)
            .expireAfterWrite(DEFAULT_EXPIRE_SECONDS, TimeUnit.SECONDS)
            .recordStats()
            .build();

    // ============================= 二级缓存操作 =============================

    /**
     * 从缓存获取数据（优先本地缓存，再查Redis）
     *
     * @param key   缓存键
     * @param clazz 返回类型
     * @param <T>   类型参数
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        // 1. 先从本地缓存获取
        Object localValue = defaultLocalCache.getIfPresent(key);
        if (localValue != null) {
            log.debug("从本地缓存获取数据，key：{}", key);
            if (NULL_VALUE.equals(localValue)) {
                return null;
            }
            return (T) localValue;
        }

        // 2. 从Redis获取
        Object redisValue = redisUtils.get(key);
        if (redisValue != null) {
            log.debug("从Redis缓存获取数据，key：{}", key);
            // 同步到本地缓存
            defaultLocalCache.put(key, redisValue);
            if (NULL_VALUE.equals(redisValue)) {
                return null;
            }
            return (T) redisValue;
        }

        return null;
    }

    /**
     * 设置缓存（同时设置本地缓存和Redis）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间（秒）
     */
    public void set(String key, Object value, long timeout) {
        // 处理空值
        Object cacheValue = value;
        if (value == null) {
            cacheValue = NULL_VALUE;
        }

        // 1. 设置Redis缓存
        redisUtils.setEx(key, cacheValue, timeout);

        // 2. 设置本地缓存
        defaultLocalCache.put(key, cacheValue);

        log.debug("设置二级缓存，key：{}，过期时间：{}秒", key, timeout);
    }

    /**
     * 获取或加载缓存（防止缓存穿透和击穿）
     *
     * @param key     缓存键
     * @param loader  数据加载器
     * @param timeout 过期时间（秒）
     * @param clazz   返回类型
     * @param <T>     类型参数
     * @return 缓存值
     */
    public <T> T getOrLoad(String key, Supplier<T> loader, long timeout, Class<T> clazz) {
        // 1. 先尝试从缓存获取
        T value = get(key, clazz);
        if (value != null) {
            return value;
        }

        // 2. 使用分布式锁防止缓存击穿
        String lockKey = "cache:lock:" + key;
        return redissonUtils.executeWithLock(lockKey, 5, 10, TimeUnit.SECONDS, () -> {
            // 双重检查
            T doubleCheckValue = get(key, clazz);
            if (doubleCheckValue != null) {
                return doubleCheckValue;
            }

            // 3. 加载数据
            log.info("缓存未命中，从数据源加载数据，key：{}", key);
            T loadedValue = loader.get();

            // 4. 设置缓存（包括空值，防止缓存穿透）
            // 添加随机过期时间，防止缓存雪崩
            long randomTimeout = timeout + (long) (Math.random() * 60);
            set(key, loadedValue, randomTimeout);

            return loadedValue;
        });
    }

    /**
     * 获取或加载缓存（使用默认过期时间）
     *
     * @param key    缓存键
     * @param loader 数据加载器
     * @param clazz  返回类型
     * @param <T>    类型参数
     * @return 缓存值
     */
    public <T> T getOrLoad(String key, Supplier<T> loader, Class<T> clazz) {
        return getOrLoad(key, loader, DEFAULT_EXPIRE_SECONDS, clazz);
    }

    /**
     * 删除缓存（同时删除本地和Redis）
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        // 1. 删除Redis缓存
        redisUtils.delete(key);

        // 2. 删除本地缓存
        defaultLocalCache.invalidate(key);

        log.debug("删除二级缓存，key：{}", key);
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键数组
     */
    public void delete(String... keys) {
        for (String key : keys) {
            delete(key);
        }
    }

    /**
     * 根据前缀批量删除缓存
     *
     * @param prefix 前缀
     */
    public void deleteByPrefix(String prefix) {
        // 1. 删除Redis缓存
        redisUtils.deleteByPrefix(prefix);

        // 2. 删除本地缓存
        defaultLocalCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));

        log.info("批量删除缓存，前缀：{}", prefix);
    }

    // ============================= 自定义本地缓存 =============================

    /**
     * 创建自定义本地缓存
     *
     * @param name            缓存名称
     * @param maximumSize     最大容量
     * @param expireAfterWrite 写入后过期时间（秒）
     * @return 本地缓存实例
     */
    public Cache<String, Object> createLocalCache(String name, long maximumSize, long expireAfterWrite) {
        Cache<String, Object> cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expireAfterWrite, TimeUnit.SECONDS)
                .recordStats()
                .build();

        localCaches.put(name, cache);
        log.info("创建本地缓存，名称：{}，最大容量：{}，过期时间：{}秒", name, maximumSize, expireAfterWrite);

        return cache;
    }

    /**
     * 获取自定义本地缓存
     *
     * @param name 缓存名称
     * @return 本地缓存实例
     */
    public Cache<String, Object> getLocalCache(String name) {
        return localCaches.get(name);
    }

    /**
     * 设置本地缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     */
    public void setLocal(String cacheName, String key, Object value) {
        Cache<String, Object> cache = localCaches.get(cacheName);
        if (cache != null) {
            cache.put(key, value);
        } else {
            defaultLocalCache.put(key, value);
        }
    }

    /**
     * 获取本地缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @return 缓存值
     */
    public Object getLocal(String cacheName, String key) {
        Cache<String, Object> cache = localCaches.get(cacheName);
        if (cache != null) {
            return cache.getIfPresent(key);
        }
        return defaultLocalCache.getIfPresent(key);
    }

    // ============================= 缓存预热 =============================

    /**
     * 缓存预热
     *
     * @param key     缓存键
     * @param loader  数据加载器
     * @param timeout 过期时间（秒）
     * @param <T>     类型参数
     */
    public <T> void warmUp(String key, Supplier<T> loader, long timeout) {
        try {
            T value = loader.get();
            set(key, value, timeout);
            log.info("缓存预热成功，key：{}", key);
        } catch (Exception e) {
            log.error("缓存预热失败，key：{}", key, e);
        }
    }

    /**
     * 批量缓存预热
     *
     * @param warmUpTasks 预热任务Map（key -> loader）
     * @param timeout     过期时间（秒）
     */
    public void warmUp(java.util.Map<String, Supplier<?>> warmUpTasks, long timeout) {
        warmUpTasks.forEach((key, loader) -> warmUp(key, loader, timeout));
    }

    // ============================= 缓存统计 =============================

    /**
     * 获取本地缓存统计信息
     *
     * @return 统计信息
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getLocalCacheStats() {
        return defaultLocalCache.stats();
    }

    /**
     * 获取指定本地缓存的统计信息
     *
     * @param cacheName 缓存名称
     * @return 统计信息
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getLocalCacheStats(String cacheName) {
        Cache<String, Object> cache = localCaches.get(cacheName);
        return cache != null ? cache.stats() : null;
    }

    /**
     * 打印缓存统计信息
     */
    public void printCacheStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = defaultLocalCache.stats();
        log.info("本地缓存统计 - 命中率：{:.2f}%，命中次数：{}，未命中次数：{}，加载次数：{}，驱逐次数：{}",
                stats.hitRate() * 100,
                stats.hitCount(),
                stats.missCount(),
                stats.loadCount(),
                stats.evictionCount());
    }

    // ============================= 缓存清理 =============================

    /**
     * 清理所有本地缓存
     */
    public void clearAllLocalCache() {
        defaultLocalCache.invalidateAll();
        localCaches.values().forEach(Cache::invalidateAll);
        log.info("清理所有本地缓存");
    }

    /**
     * 清理指定本地缓存
     *
     * @param cacheName 缓存名称
     */
    public void clearLocalCache(String cacheName) {
        Cache<String, Object> cache = localCaches.get(cacheName);
        if (cache != null) {
            cache.invalidateAll();
            log.info("清理本地缓存：{}", cacheName);
        }
    }

    /**
     * 清理过期的缓存
     */
    public void cleanUp() {
        defaultLocalCache.cleanUp();
        localCaches.values().forEach(Cache::cleanUp);
        log.debug("执行缓存清理");
    }
}
