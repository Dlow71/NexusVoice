package com.nexusvoice.interfaces.api.test;

import com.nexusvoice.common.Result;
import com.nexusvoice.utils.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.redisson.api.RateIntervalUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis测试控制器
 * 提供Redis和Redisson功能测试接口
 *
 * @author NexusVoice
 * @since 2025-10-17
 */
@RestController
@RequestMapping("/api/dev/redis")
@Profile({"local", "dev", "test"})
@Tag(name = "开发测试-Redis测试", description = "Redis和Redisson功能测试接口（仅开发环境）")
public class RedisTestController {

    private static final Logger log = LoggerFactory.getLogger(RedisTestController.class);

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RedissonUtils redissonUtils;

    @Autowired
    private CacheUtils cacheUtils;

    @Autowired
    private LockUtils lockUtils;

    @Autowired
    private RateLimiterUtils rateLimiterUtils;

    /**
     * Redis连接测试
     */
    @GetMapping("/ping")
    @Operation(summary = "Redis连接测试", description = "测试Redis连接是否正常")
    public Result<String> ping() {
        try {
            String testKey = "test:ping";
            redisUtils.set(testKey, "pong");
            String result = (String) redisUtils.get(testKey);
            redisUtils.delete(testKey);
            
            if ("pong".equals(result)) {
                log.info("Redis连接测试成功");
                return Result.success("Redis连接正常");
            } else {
                log.error("Redis连接测试失败，返回值不匹配");
                return Result.error("Redis连接异常");
            }
        } catch (Exception e) {
            log.error("Redis连接测试失败", e);
            return Result.error("Redis连接失败: " + e.getMessage());
        }
    }

    /**
     * 设置缓存值
     */
    @PostMapping("/set")
    @Operation(summary = "设置缓存值", description = "设置一个键值对到Redis")
    public Result<Void> setValue(
            @RequestParam @Parameter(description = "缓存键") String key,
            @RequestParam @Parameter(description = "缓存值") String value,
            @RequestParam(defaultValue = "300") @Parameter(description = "过期时间（秒）") Long timeout) {
        
        try {
            boolean success = redisUtils.setEx(key, value, timeout);
            if (success) {
                log.info("设置缓存成功，key：{}，过期时间：{}秒", key, timeout);
                return Result.success();
            } else {
                log.error("设置缓存失败，key：{}", key);
                return Result.error("设置缓存失败");
            }
        } catch (Exception e) {
            log.error("设置缓存异常，key：{}", key, e);
            return Result.error("设置缓存异常: " + e.getMessage());
        }
    }

    /**
     * 获取缓存值
     */
    @GetMapping("/get")
    @Operation(summary = "获取缓存值", description = "从Redis获取指定键的值")
    public Result<Object> getValue(@RequestParam @Parameter(description = "缓存键") String key) {
        try {
            Object value = redisUtils.get(key);
            if (value != null) {
                long ttl = redisUtils.getExpire(key);
                Map<String, Object> result = new HashMap<>();
                result.put("value", value);
                result.put("ttl", ttl);
                log.info("获取缓存成功，key：{}，剩余过期时间：{}秒", key, ttl);
                return Result.success(result);
            } else {
                log.info("缓存不存在，key：{}", key);
                return Result.error("缓存键不存在");
            }
        } catch (Exception e) {
            log.error("获取缓存异常，key：{}", key, e);
            return Result.error("获取缓存异常: " + e.getMessage());
        }
    }

    /**
     * 删除缓存值
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除缓存值", description = "从Redis删除指定键")
    public Result<Void> deleteValue(@RequestParam @Parameter(description = "缓存键") String key) {
        try {
            redisUtils.delete(key);
            log.info("删除缓存成功，key：{}", key);
            return Result.success();
        } catch (Exception e) {
            log.error("删除缓存异常，key：{}", key, e);
            return Result.error("删除缓存异常: " + e.getMessage());
        }
    }

    /**
     * 分布式锁测试
     */
    @PostMapping("/lock")
    @Operation(summary = "分布式锁测试", description = "测试分布式锁功能")
    public Result<Map<String, Object>> testLock(
            @RequestParam @Parameter(description = "锁键") String lockKey,
            @RequestParam(defaultValue = "5") @Parameter(description = "持有时间（秒）") Long holdTime) {
        
        Map<String, Object> result = new HashMap<>();
        String threadName = Thread.currentThread().getName();
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("线程{}尝试获取锁：{}", threadName, lockKey);
            
            boolean acquired = lockUtils.tryLock(lockKey, 3, holdTime, TimeUnit.SECONDS);
            
            if (acquired) {
                log.info("线程{}成功获取锁：{}", threadName, lockKey);
                result.put("acquired", true);
                result.put("thread", threadName);
                result.put("holdTime", holdTime);
                
                // 模拟业务处理
                Thread.sleep(2000);
                
                // 释放锁
                lockUtils.unlock(lockKey);
                log.info("线程{}释放锁：{}", threadName, lockKey);
            } else {
                log.warn("线程{}获取锁失败：{}", threadName, lockKey);
                result.put("acquired", false);
                result.put("thread", threadName);
                result.put("reason", "锁被其他线程持有");
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            result.put("elapsedTime", elapsedTime + "ms");
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("分布式锁测试失败", e);
            return Result.error("分布式锁测试失败: " + e.getMessage());
        }
    }

    /**
     * 限流测试
     */
    @PostMapping("/ratelimit")
    @Operation(summary = "限流测试", description = "测试API限流功能")
    public Result<Map<String, Object>> testRateLimit(
            @RequestParam(defaultValue = "test:api") @Parameter(description = "限流键") String key,
            @RequestParam(defaultValue = "10") @Parameter(description = "速率（每秒允许的请求数）") Long rate) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 尝试获取许可
            boolean allowed = rateLimiterUtils.tryAcquire(key, rate, 1, RateIntervalUnit.SECONDS, 1);
            
            result.put("allowed", allowed);
            result.put("key", key);
            result.put("rate", rate + "/秒");
            
            if (allowed) {
                result.put("message", "请求通过限流");
                log.info("限流测试通过，key：{}，速率：{}/秒", key, rate);
            } else {
                result.put("message", "请求被限流");
                log.warn("限流测试被限制，key：{}，速率：{}/秒", key, rate);
            }
            
            // 获取限流器状态
            RateLimiterUtils.RateLimiterStatus status = rateLimiterUtils.getRateLimiterStatus(key);
            result.put("availablePermits", status.getAvailablePermits());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("限流测试失败", e);
            return Result.error("限流测试失败: " + e.getMessage());
        }
    }

    /**
     * 二级缓存测试
     */
    @PostMapping("/cache")
    @Operation(summary = "二级缓存测试", description = "测试本地缓存+Redis二级缓存功能")
    public Result<Map<String, Object>> testCache(
            @RequestParam @Parameter(description = "缓存键") String key,
            @RequestParam @Parameter(description = "缓存值") String value) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 设置缓存
            cacheUtils.set(key, value, 300);
            result.put("operation", "set");
            result.put("key", key);
            result.put("value", value);
            
            // 第一次获取（从Redis）
            long start1 = System.currentTimeMillis();
            Object cached1 = cacheUtils.get(key, String.class);
            long time1 = System.currentTimeMillis() - start1;
            result.put("firstGet", cached1);
            result.put("firstGetTime", time1 + "ms");
            
            // 第二次获取（从本地缓存）
            long start2 = System.currentTimeMillis();
            Object cached2 = cacheUtils.get(key, String.class);
            long time2 = System.currentTimeMillis() - start2;
            result.put("secondGet", cached2);
            result.put("secondGetTime", time2 + "ms");
            
            // 打印缓存统计
            cacheUtils.printCacheStats();
            
            log.info("二级缓存测试成功，key：{}，第一次耗时：{}ms，第二次耗时：{}ms", key, time1, time2);
            return Result.success(result);
        } catch (Exception e) {
            log.error("二级缓存测试失败", e);
            return Result.error("二级缓存测试失败: " + e.getMessage());
        }
    }

    /**
     * 布隆过滤器测试
     */
    @PostMapping("/bloom")
    @Operation(summary = "布隆过滤器测试", description = "测试布隆过滤器功能")
    public Result<Map<String, Object>> testBloomFilter(
            @RequestParam @Parameter(description = "过滤器键") String key,
            @RequestParam @Parameter(description = "测试元素") String element) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 初始化布隆过滤器
            String bloomKey = "bloom:" + key;
            redissonUtils.initBloomFilter(bloomKey, 10000, 0.01);
            
            // 检查元素是否存在（添加前）
            boolean existsBefore = redissonUtils.bloomContains(bloomKey, element);
            result.put("existsBefore", existsBefore);
            
            // 添加元素
            boolean added = redissonUtils.bloomAdd(bloomKey, element);
            result.put("added", added);
            
            // 检查元素是否存在（添加后）
            boolean existsAfter = redissonUtils.bloomContains(bloomKey, element);
            result.put("existsAfter", existsAfter);
            
            // 测试不存在的元素
            String nonExistElement = element + "_not_exist";
            boolean nonExistCheck = redissonUtils.bloomContains(bloomKey, nonExistElement);
            result.put("nonExistElement", nonExistElement);
            result.put("nonExistCheck", nonExistCheck);
            
            log.info("布隆过滤器测试成功，key：{}，元素：{}", bloomKey, element);
            return Result.success(result);
        } catch (Exception e) {
            log.error("布隆过滤器测试失败", e);
            return Result.error("布隆过滤器测试失败: " + e.getMessage());
        }
    }

    /**
     * 获取Redis信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取Redis信息", description = "获取Redis和Redisson的配置信息")
    public Result<Map<String, Object>> getRedisInfo() {
        Map<String, Object> info = new HashMap<>();
        
        try {
            // Redisson客户端信息
            Map<String, String> redissonInfo = redissonUtils.getClientInfo();
            info.put("redisson", redissonInfo);
            
            // 缓存统计信息
            com.github.benmanes.caffeine.cache.stats.CacheStats cacheStats = cacheUtils.getLocalCacheStats();
            Map<String, Object> stats = new HashMap<>();
            stats.put("hitRate", String.format("%.2f%%", cacheStats.hitRate() * 100));
            stats.put("hitCount", cacheStats.hitCount());
            stats.put("missCount", cacheStats.missCount());
            stats.put("loadCount", cacheStats.loadCount());
            stats.put("evictionCount", cacheStats.evictionCount());
            info.put("localCache", stats);
            
            log.info("获取Redis信息成功");
            return Result.success(info);
        } catch (Exception e) {
            log.error("获取Redis信息失败", e);
            return Result.error("获取Redis信息失败: " + e.getMessage());
        }
    }
}
