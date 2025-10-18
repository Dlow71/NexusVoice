package com.nexusvoice.domain.config.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nexusvoice.domain.config.model.SystemConfig;
import com.nexusvoice.domain.config.repository.SystemConfigRepository;
import com.nexusvoice.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 系统配置缓存服务
 * 负责Redis缓存和本地二级缓存的统一管理
 * 
 * 缓存策略：
 * 1. 本地Caffeine缓存（TTL: 30秒） - 极致性能
 * 2. Redis缓存（TTL: 1小时） - 分布式一致性
 * 3. 数据库 - 最终数据源
 * 
 * 一致性保证：
 * 1. CUD操作后立即删除Redis缓存
 * 2. 通过Redis Pub/Sub通知所有实例清除本地缓存
 * 3. 下次读取时重新加载最新数据
 *
 * @author NexusVoice
 * @since 2025-10-18
 */
@Slf4j
@Service
public class SystemConfigCacheService {

    /**
     * Redis缓存Key前缀
     */
    private static final String CACHE_PREFIX = "system:config:";
    
    /**
     * 缓存空值的标识（防止缓存穿透）
     */
    private static final String NULL_VALUE = "NULL";
    
    /**
     * Redis缓存过期时间（秒）- 1小时
     */
    private static final long REDIS_CACHE_EXPIRE_SECONDS = 3600;
    
    /**
     * 本地缓存过期时间（秒）- 30秒
     */
    private static final long LOCAL_CACHE_EXPIRE_SECONDS = 30;
    
    /**
     * 空值缓存过期时间（秒）- 30秒（避免新建配置后长时间返回null）
     */
    private static final long NULL_CACHE_EXPIRE_SECONDS = 30;
    
    /**
     * Redis Pub/Sub频道名称
     */
    private static final String CACHE_EVICT_CHANNEL = "system:config:evict";
    
    @Autowired
    private RedisUtils redisUtils;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private SystemConfigRepository systemConfigRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 本地Caffeine缓存
     * 最大容量1000个配置项，30秒过期
     */
    private Cache<String, String> localCache;
    
    /**
     * 当前实例ID（用于识别事件来源）
     */
    private String instanceId;
    
    /**
     * 是否启用本地二级缓存
     */
    @Value("${system.config.cache.local.enabled:true}")
    private boolean localCacheEnabled;
    
    @PostConstruct
    public void init() {
        // 生成实例ID
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            String timestamp = String.valueOf(System.currentTimeMillis());
            this.instanceId = hostName + "-" + timestamp;
        } catch (Exception e) {
            this.instanceId = "instance-" + System.currentTimeMillis();
        }
        
        // 初始化本地缓存
        if (localCacheEnabled) {
            this.localCache = Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(LOCAL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
                    .build();
            log.info("本地配置缓存已启用，实例ID: {}", instanceId);
        } else {
            log.info("本地配置缓存已禁用，实例ID: {}", instanceId);
        }
        
        // 预热缓存
        warmUpCache();
    }
    
    /**
     * 获取配置值（三级缓存策略）
     *
     * @param configKey 配置键
     * @return 配置值，不存在返回null
     */
    public String getConfigValue(String configKey) {
        if (configKey == null || configKey.trim().isEmpty()) {
            return null;
        }
        
        // 1. 尝试从本地缓存获取
        if (localCacheEnabled && localCache != null) {
            String localValue = localCache.getIfPresent(configKey);
            if (localValue != null) {
                if (NULL_VALUE.equals(localValue)) {
                    log.debug("本地缓存命中（空值），配置键: {}", configKey);
                    return null;
                }
                log.debug("本地缓存命中，配置键: {}", configKey);
                return localValue;
            }
        }
        
        // 2. 尝试从Redis获取
        String redisKey = CACHE_PREFIX + configKey;
        String redisValue = redisUtils.getString(redisKey);
        
        if (redisValue != null) {
            if (NULL_VALUE.equals(redisValue)) {
                // 缓存了空值，避免缓存穿透
                if (localCacheEnabled && localCache != null) {
                    localCache.put(configKey, NULL_VALUE);
                }
                log.debug("Redis缓存命中（空值），配置键: {}", configKey);
                return null;
            }
            
            // 写入本地缓存
            if (localCacheEnabled && localCache != null) {
                localCache.put(configKey, redisValue);
            }
            log.debug("Redis缓存命中，配置键: {}", configKey);
            return redisValue;
        }
        
        // 3. 从数据库加载
        log.debug("缓存未命中，从数据库加载，配置键: {}", configKey);
        Optional<SystemConfig> configOpt = systemConfigRepository.findByKey(configKey);
        
        if (configOpt.isPresent() && configOpt.get().isActive()) {
            String value = configOpt.get().getConfigValue();
            
            // 写入Redis缓存（添加随机过期时间偏移，防止缓存雪崩）
            long expireSeconds = REDIS_CACHE_EXPIRE_SECONDS + 
                    ThreadLocalRandom.current().nextLong(-300, 300); // ±5分钟随机偏移
            redisUtils.setString(redisKey, value, expireSeconds);
            
            // 写入本地缓存
            if (localCacheEnabled && localCache != null) {
                localCache.put(configKey, value);
            }
            
            log.info("配置已加载并缓存，配置键: {}, 缓存过期时间: {}秒", configKey, expireSeconds);
            return value;
        }
        
        // 配置不存在，缓存空值（防止缓存穿透）
        log.debug("配置不存在，缓存空值，配置键: {}", configKey);
        redisUtils.setString(redisKey, NULL_VALUE, NULL_CACHE_EXPIRE_SECONDS);
        
        if (localCacheEnabled && localCache != null) {
            localCache.put(configKey, NULL_VALUE);
        }
        
        return null;
    }
    
    /**
     * 失效指定配置的缓存
     * 会同时清除Redis和本地缓存，并通知其他实例
     *
     * @param configKey 配置键
     */
    public void evictConfig(String configKey) {
        if (configKey == null || configKey.trim().isEmpty()) {
            return;
        }
        
        Set<String> keys = Collections.singleton(configKey);
        evictConfigs(keys);
    }
    
    /**
     * 批量失效配置缓存
     *
     * @param configKeys 配置键集合
     */
    public void evictConfigs(Set<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return;
        }
        
        log.info("失效配置缓存，配置键: {}", configKeys);
        
        // 1. 删除Redis缓存
        for (String configKey : configKeys) {
            String redisKey = CACHE_PREFIX + configKey;
            redisUtils.delete(redisKey);
        }
        
        // 2. 删除本地缓存
        if (localCacheEnabled && localCache != null) {
            for (String configKey : configKeys) {
                localCache.invalidate(configKey);
            }
        }
        
        // 3. 发布Redis Pub/Sub消息，通知其他实例
        publishCacheEvictEvent(configKeys);
    }
    
    /**
     * 清空所有配置缓存
     */
    public void evictAllConfigs() {
        log.info("清空所有配置缓存");
        
        // 1. 删除所有Redis缓存
        redisUtils.deleteByPrefix(CACHE_PREFIX);
        
        // 2. 清空本地缓存
        if (localCacheEnabled && localCache != null) {
            localCache.invalidateAll();
        }
        
        // 3. 发布Redis Pub/Sub消息
        publishCacheEvictEvent(null);
    }
    
    /**
     * 预热缓存
     * 启动时将所有启用的配置加载到Redis
     */
    public void warmUpCache() {
        try {
            log.info("开始预热配置缓存...");
            
            List<SystemConfig> configs = systemConfigRepository.findAllEnabled();
            int count = 0;
            
            for (SystemConfig config : configs) {
                String redisKey = CACHE_PREFIX + config.getConfigKey();
                // 添加随机过期时间偏移，防止缓存雪崩
                long expireSeconds = REDIS_CACHE_EXPIRE_SECONDS + 
                        ThreadLocalRandom.current().nextLong(-300, 300);
                redisUtils.setString(redisKey, config.getConfigValue(), expireSeconds);
                count++;
            }
            
            log.info("配置缓存预热完成，加载了{}个配置项", count);
            
        } catch (Exception e) {
            log.error("配置缓存预热失败", e);
        }
    }
    
    /**
     * 发布缓存失效事件（通过Redis Pub/Sub）
     *
     * @param configKeys 配置键集合，null表示清空所有
     */
    private void publishCacheEvictEvent(Set<String> configKeys) {
        try {
            // 构建消息
            Map<String, Object> message = new HashMap<>();
            message.put("configKeys", configKeys != null ? configKeys : Collections.emptySet());
            message.put("clearAll", configKeys == null || configKeys.isEmpty());
            message.put("sourceInstanceId", instanceId);
            message.put("timestamp", System.currentTimeMillis());
            
            // 序列化并发布到Redis频道
            String messageJson = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(CACHE_EVICT_CHANNEL, messageJson);
            
            log.debug("发布缓存失效事件，配置键: {}, 清空所有: {}", 
                    configKeys, message.get("clearAll"));
            
        } catch (JsonProcessingException e) {
            log.error("序列化缓存失效事件失败", e);
        } catch (Exception e) {
            log.error("发布缓存失效事件失败", e);
        }
    }
    
    /**
     * 处理来自Redis Pub/Sub的缓存失效事件
     * 此方法由监听器调用
     *
     * @param sourceInstanceId 事件来源实例ID
     * @param configKeys 配置键集合
     * @param clearAll 是否清空所有
     */
    public void handleCacheEvictEvent(String sourceInstanceId, Set<String> configKeys, boolean clearAll) {
        // 忽略自己发布的事件
        if (instanceId.equals(sourceInstanceId)) {
            log.debug("忽略自己发布的缓存失效事件");
            return;
        }
        
        log.info("收到缓存失效事件，来源: {}, 清空所有: {}, 配置键: {}", 
                sourceInstanceId, clearAll, configKeys);
        
        if (!localCacheEnabled || localCache == null) {
            return;
        }
        
        // 清除本地缓存
        if (clearAll) {
            localCache.invalidateAll();
            log.info("已清空本地配置缓存");
        } else if (configKeys != null && !configKeys.isEmpty()) {
            for (String configKey : configKeys) {
                localCache.invalidate(configKey);
            }
            log.info("已清除本地配置缓存，配置键: {}", configKeys);
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("instanceId", instanceId);
        stats.put("localCacheEnabled", localCacheEnabled);
        
        if (localCacheEnabled && localCache != null) {
            stats.put("localCacheSize", localCache.estimatedSize());
            stats.put("localCacheStats", localCache.stats().toString());
        }
        
        return stats;
    }
    
    /**
     * 获取当前实例ID
     */
    public String getInstanceId() {
        return instanceId;
    }
    
    /**
     * 定时刷新本地缓存（每5分钟执行一次）
     * 作为Pub/Sub消息丢失的补充机制
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 300000)
    public void scheduledLocalCacheRefresh() {
        if (localCacheEnabled && localCache != null) {
            long cacheSize = localCache.estimatedSize();
            if (cacheSize > 0) {
                log.debug("定时刷新本地缓存，当前缓存大小: {}", cacheSize);
                localCache.invalidateAll();
                log.debug("本地缓存已清空，下次访问时将从Redis重新加载");
            }
        }
    }
}
