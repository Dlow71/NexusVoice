package com.nexusvoice.utils;

import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 限流工具类
 * 提供API限流、用户级限流、IP限流等功能
 *
 * @author NexusVoice
 * @since 2025-10-17
 */
@Component
public class RateLimiterUtils {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterUtils.class);

    @Autowired
    private RedissonUtils redissonUtils;

    @Autowired(required = false)
    private HttpServletRequest httpServletRequest;

    /**
     * 限流器配置缓存
     */
    private final ConcurrentHashMap<String, RateLimiterConfig> limiterConfigs = new ConcurrentHashMap<>();

    /**
     * 默认API限流配置
     */
    private static final long DEFAULT_API_RATE = 100;
    private static final long DEFAULT_API_INTERVAL = 1;
    private static final RateIntervalUnit DEFAULT_API_INTERVAL_UNIT = RateIntervalUnit.SECONDS;

    /**
     * 默认用户限流配置
     */
    private static final long DEFAULT_USER_RATE = 60;
    private static final long DEFAULT_USER_INTERVAL = 1;
    private static final RateIntervalUnit DEFAULT_USER_INTERVAL_UNIT = RateIntervalUnit.MINUTES;

    /**
     * 默认IP限流配置
     */
    private static final long DEFAULT_IP_RATE = 100;
    private static final long DEFAULT_IP_INTERVAL = 1;
    private static final RateIntervalUnit DEFAULT_IP_INTERVAL_UNIT = RateIntervalUnit.MINUTES;

    /**
     * 限流器配置类
     */
    public static class RateLimiterConfig {
        private final long rate;
        private final long interval;
        private final RateIntervalUnit intervalUnit;
        private final RateType rateType;

        public RateLimiterConfig(long rate, long interval, RateIntervalUnit intervalUnit) {
            this(rate, interval, intervalUnit, RateType.OVERALL);
        }

        public RateLimiterConfig(long rate, long interval, RateIntervalUnit intervalUnit, RateType rateType) {
            this.rate = rate;
            this.interval = interval;
            this.intervalUnit = intervalUnit;
            this.rateType = rateType;
        }

        public long getRate() {
            return rate;
        }

        public long getInterval() {
            return interval;
        }

        public RateIntervalUnit getIntervalUnit() {
            return intervalUnit;
        }

        public RateType getRateType() {
            return rateType;
        }
    }

    /**
     * API限流
     *
     * @param api API路径
     * @return 是否通过限流
     */
    public boolean tryAcquireApi(String api) {
        return tryAcquireApi(api, DEFAULT_API_RATE, DEFAULT_API_INTERVAL, DEFAULT_API_INTERVAL_UNIT);
    }

    /**
     * API限流（自定义配置）
     *
     * @param api          API路径
     * @param rate         速率
     * @param interval     时间间隔
     * @param intervalUnit 时间间隔单位
     * @return 是否通过限流
     */
    public boolean tryAcquireApi(String api, long rate, long interval, RateIntervalUnit intervalUnit) {
        String rateLimiterKey = buildApiRateLimiterKey(api);
        return tryAcquire(rateLimiterKey, rate, interval, intervalUnit, 1);
    }

    /**
     * 用户限流
     *
     * @param userId 用户ID
     * @return 是否通过限流
     */
    public boolean tryAcquireUser(Long userId) {
        return tryAcquireUser(userId, DEFAULT_USER_RATE, DEFAULT_USER_INTERVAL, DEFAULT_USER_INTERVAL_UNIT);
    }

    /**
     * 用户限流（自定义配置）
     *
     * @param userId       用户ID
     * @param rate         速率
     * @param interval     时间间隔
     * @param intervalUnit 时间间隔单位
     * @return 是否通过限流
     */
    public boolean tryAcquireUser(Long userId, long rate, long interval, RateIntervalUnit intervalUnit) {
        String rateLimiterKey = buildUserRateLimiterKey(userId);
        return tryAcquire(rateLimiterKey, rate, interval, intervalUnit, 1);
    }

    /**
     * IP限流
     *
     * @param ip IP地址
     * @return 是否通过限流
     */
    public boolean tryAcquireIp(String ip) {
        return tryAcquireIp(ip, DEFAULT_IP_RATE, DEFAULT_IP_INTERVAL, DEFAULT_IP_INTERVAL_UNIT);
    }

    /**
     * IP限流（自定义配置）
     *
     * @param ip           IP地址
     * @param rate         速率
     * @param interval     时间间隔
     * @param intervalUnit 时间间隔单位
     * @return 是否通过限流
     */
    public boolean tryAcquireIp(String ip, long rate, long interval, RateIntervalUnit intervalUnit) {
        String rateLimiterKey = buildIpRateLimiterKey(ip);
        return tryAcquire(rateLimiterKey, rate, interval, intervalUnit, 1);
    }

    /**
     * 通用限流方法
     *
     * @param key          限流器键
     * @param rate         速率
     * @param interval     时间间隔
     * @param intervalUnit 时间间隔单位
     * @param permits      许可数量
     * @return 是否通过限流
     */
    public boolean tryAcquire(String key, long rate, long interval, RateIntervalUnit intervalUnit, long permits) {
        try {
            // 获取或创建限流器
            RRateLimiter rateLimiter = getOrCreateRateLimiter(key, rate, interval, intervalUnit);
            
            // 尝试获取许可
            boolean acquired = rateLimiter.tryAcquire(permits);
            
            if (!acquired) {
                log.warn("限流触发，key：{}，速率：{}/{}{}，请求许可：{}", 
                    key, rate, interval, intervalUnit, permits);
            } else {
                log.debug("限流通过，key：{}，剩余许可：{}", key, rateLimiter.availablePermits());
            }
            
            return acquired;
        } catch (Exception e) {
            log.error("限流器执行失败，key：{}", key, e);
            // 限流器失败时，默认放行，避免影响业务
            return true;
        }
    }

    /**
     * 通用限流方法（带超时）
     *
     * @param key          限流器键
     * @param rate         速率
     * @param interval     时间间隔
     * @param intervalUnit 时间间隔单位
     * @param permits      许可数量
     * @param timeout      超时时间
     * @param timeUnit     时间单位
     * @return 是否通过限流
     */
    public boolean tryAcquire(String key, long rate, long interval, RateIntervalUnit intervalUnit, 
                              long permits, long timeout, TimeUnit timeUnit) {
        try {
            // 获取或创建限流器
            RRateLimiter rateLimiter = getOrCreateRateLimiter(key, rate, interval, intervalUnit);
            
            // 尝试获取许可（带超时）
            boolean acquired = rateLimiter.tryAcquire(permits, timeout, timeUnit);
            
            if (!acquired) {
                log.warn("限流触发，key：{}，速率：{}/{}{}，请求许可：{}，超时：{}{}", 
                    key, rate, interval, intervalUnit, permits, timeout, timeUnit);
            } else {
                log.debug("限流通过，key：{}，剩余许可：{}", key, rateLimiter.availablePermits());
            }
            
            return acquired;
        } catch (Exception e) {
            log.error("限流器执行失败，key：{}", key, e);
            // 限流器失败时，默认放行，避免影响业务
            return true;
        }
    }

    /**
     * 检查并抛出限流异常
     *
     * @param key          限流器键
     * @param rate         速率
     * @param interval     时间间隔
     * @param intervalUnit 时间间隔单位
     * @param permits      许可数量
     */
    public void checkRateLimit(String key, long rate, long interval, RateIntervalUnit intervalUnit, long permits) {
        if (!tryAcquire(key, rate, interval, intervalUnit, permits)) {
            throw new BizException(ErrorCodeEnum.RATE_LIMIT_EXCEEDED);
        }
    }

    /**
     * 获取或创建限流器
     *
     * @param key          限流器键
     * @param rate         速率
     * @param interval     时间间隔
     * @param intervalUnit 时间间隔单位
     * @return 限流器
     */
    private RRateLimiter getOrCreateRateLimiter(String key, long rate, long interval, RateIntervalUnit intervalUnit) {
        RRateLimiter rateLimiter = redissonUtils.getRateLimiter(key);
        
        // 检查配置是否变化
        RateLimiterConfig config = limiterConfigs.get(key);
        if (config == null || config.getRate() != rate || config.getInterval() != interval 
            || config.getIntervalUnit() != intervalUnit) {
            
            // 更新配置
            boolean success = rateLimiter.trySetRate(RateType.OVERALL, rate, interval, intervalUnit);
            if (success) {
                limiterConfigs.put(key, new RateLimiterConfig(rate, interval, intervalUnit));
                log.info("限流器配置更新，key：{}，速率：{}/{}{}", key, rate, interval, intervalUnit);
            }
        }
        
        return rateLimiter;
    }

    /**
     * 获取当前请求的IP地址
     *
     * @return IP地址
     */
    public String getCurrentIp() {
        if (httpServletRequest == null) {
            return "unknown";
        }
        
        String ip = httpServletRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getRemoteAddr();
        }
        
        // 对于多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

    /**
     * 获取当前请求的API路径
     *
     * @return API路径
     */
    public String getCurrentApi() {
        if (httpServletRequest == null) {
            return "unknown";
        }
        
        String method = httpServletRequest.getMethod();
        String path = httpServletRequest.getRequestURI();
        return method + ":" + path;
    }

    /**
     * 重置限流器
     *
     * @param key 限流器键
     */
    public void resetRateLimiter(String key) {
        RRateLimiter rateLimiter = redissonUtils.getRateLimiter(key);
        rateLimiter.delete();
        limiterConfigs.remove(key);
        log.info("重置限流器，key：{}", key);
    }

    /**
     * 获取限流器状态
     *
     * @param key 限流器键
     * @return 状态信息
     */
    public RateLimiterStatus getRateLimiterStatus(String key) {
        RRateLimiter rateLimiter = redissonUtils.getRateLimiter(key);
        RateLimiterConfig config = limiterConfigs.get(key);
        
        return new RateLimiterStatus(
            key,
            config != null ? config.getRate() : 0,
            config != null ? config.getInterval() : 0,
            config != null ? config.getIntervalUnit() : null,
            rateLimiter.availablePermits(),
            rateLimiter.isExists()
        );
    }

    /**
     * 限流器状态类
     */
    public static class RateLimiterStatus {
        private final String key;
        private final long rate;
        private final long interval;
        private final RateIntervalUnit intervalUnit;
        private final long availablePermits;
        private final boolean exists;

        public RateLimiterStatus(String key, long rate, long interval, RateIntervalUnit intervalUnit, 
                                 long availablePermits, boolean exists) {
            this.key = key;
            this.rate = rate;
            this.interval = interval;
            this.intervalUnit = intervalUnit;
            this.availablePermits = availablePermits;
            this.exists = exists;
        }

        // Getters
        public String getKey() {
            return key;
        }

        public long getRate() {
            return rate;
        }

        public long getInterval() {
            return interval;
        }

        public RateIntervalUnit getIntervalUnit() {
            return intervalUnit;
        }

        public long getAvailablePermits() {
            return availablePermits;
        }

        public boolean isExists() {
            return exists;
        }

        @Override
        public String toString() {
            return String.format("RateLimiterStatus{key='%s', rate=%d/%d%s, availablePermits=%d, exists=%s}",
                key, rate, interval, intervalUnit, availablePermits, exists);
        }
    }

    // ============================= 构建限流器键的辅助方法 =============================

    /**
     * 构建API限流器键
     *
     * @param api API路径
     * @return 限流器键
     */
    private static String buildApiRateLimiterKey(String api) {
        return "rate_limiter:api:" + api.replace("/", "_").replace(":", "_");
    }

    /**
     * 构建用户限流器键
     *
     * @param userId 用户ID
     * @return 限流器键
     */
    private static String buildUserRateLimiterKey(Long userId) {
        return "rate_limiter:user:" + userId;
    }

    /**
     * 构建IP限流器键
     *
     * @param ip IP地址
     * @return 限流器键
     */
    private static String buildIpRateLimiterKey(String ip) {
        return "rate_limiter:ip:" + ip.replace(".", "_");
    }

    /**
     * 构建自定义限流器键
     *
     * @param prefix 前缀
     * @param keys   键值
     * @return 限流器键
     */
    public static String buildRateLimiterKey(String prefix, Object... keys) {
        if (keys == null || keys.length == 0) {
            return "rate_limiter:" + prefix;
        }
        
        StringBuilder sb = new StringBuilder("rate_limiter:").append(prefix);
        for (Object key : keys) {
            sb.append(":").append(key);
        }
        return sb.toString();
    }
}
