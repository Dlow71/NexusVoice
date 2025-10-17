package com.nexusvoice.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Redisson配置类
 * 提供分布式锁、限流器、布隆过滤器等高级Redis功能
 *
 * @author NexusVoice
 * @since 2025-10-17
 */
@Configuration
@EnableCaching
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(prefix = "spring.data.redis", name = "host")
public class RedissonConfig {

    private static final Logger log = LoggerFactory.getLogger(RedissonConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.timeout:3000ms}")
    private String timeout;
    
    @Value("${spring.data.redis.lettuce.pool.max-active:20}")
    private int maxActive;
    
    @Value("${spring.data.redis.lettuce.pool.min-idle:5}")
    private int minIdle;

    @Value("${redisson.config:}")
    private String redissonConfigYaml;

    /**
     * 创建Redisson客户端
     * 支持单节点、主从、哨兵、集群等多种模式
     *
     * @return RedissonClient实例
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        log.info("初始化Redisson客户端");
        
        Config config;
        
        // 如果提供了完整的Redisson配置YAML，则直接使用
        if (StringUtils.hasText(redissonConfigYaml)) {
            try {
                log.info("使用自定义Redisson配置");
                config = Config.fromYAML(redissonConfigYaml);
            } catch (IOException e) {
                log.error("解析Redisson配置失败，将使用默认单节点配置", e);
                config = createSingleServerConfig();
            }
        } else {
            // 否则使用Spring配置构建单节点配置
            config = createSingleServerConfig();
        }
        
        // 设置编解码器为Jackson（与RedisTemplate保持一致）
        config.setCodec(new JsonJacksonCodec());
        
        // 设置线程池数量
        config.setThreads(Runtime.getRuntime().availableProcessors() * 2);
        
        // 设置Netty线程池数量
        config.setNettyThreads(Runtime.getRuntime().availableProcessors() * 2);
        
        // 创建Redisson客户端
        RedissonClient redissonClient = Redisson.create(config);
        
        // 测试连接
        try {
            redissonClient.getBucket("test:connection").set("OK");
            redissonClient.getBucket("test:connection").delete();
            log.info("Redisson客户端初始化成功，Redis连接正常");
        } catch (Exception e) {
            log.error("Redisson连接Redis失败", e);
            throw new RuntimeException("无法连接到Redis服务器", e);
        }
        
        return redissonClient;
    }

    /**
     * 创建单节点配置
     *
     * @return 单节点配置
     */
    private Config createSingleServerConfig() {
        log.info("创建Redisson单节点配置：{}:{}", redisHost, redisPort);
        
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer();
        
        // 设置Redis地址
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        singleServerConfig.setAddress(address);
        
        // 设置数据库索引
        singleServerConfig.setDatabase(redisDatabase);
        
        // 设置密码（如果有）
        if (StringUtils.hasText(redisPassword)) {
            singleServerConfig.setPassword(redisPassword);
            log.info("已配置Redis密码认证");
        }
        
        // 连接池配置
        singleServerConfig.setConnectionPoolSize(maxActive);
        singleServerConfig.setConnectionMinimumIdleSize(minIdle);
        
        // 连接超时时间
        int timeoutMs = parseTimeout(timeout);
        singleServerConfig.setConnectTimeout(timeoutMs);
        singleServerConfig.setTimeout(timeoutMs);
        singleServerConfig.setIdleConnectionTimeout(timeoutMs * 2);
        
        // 设置命令等待超时
        singleServerConfig.setRetryAttempts(3);
        singleServerConfig.setRetryInterval(1500);
        
        // 设置心跳检测
        singleServerConfig.setPingConnectionInterval(30000);
        
        // 设置DNS监控（用于容器环境）
        singleServerConfig.setDnsMonitoringInterval(5000L);
        
        // 开启连接池清理
        singleServerConfig.setSubscriptionConnectionPoolSize(10);
        singleServerConfig.setSubscriptionConnectionMinimumIdleSize(1);
        
        log.info("Redisson单节点配置创建完成");
        return config;
    }

    /**
     * 解析超时时间配置
     *
     * @param timeout 超时时间字符串（如：3000ms、3s）
     * @return 超时时间（毫秒）
     */
    private int parseTimeout(String timeout) {
        if (!StringUtils.hasText(timeout)) {
            return 3000;
        }
        
        timeout = timeout.toLowerCase().trim();
        
        try {
            if (timeout.endsWith("ms")) {
                return Integer.parseInt(timeout.substring(0, timeout.length() - 2));
            } else if (timeout.endsWith("s")) {
                return Integer.parseInt(timeout.substring(0, timeout.length() - 1)) * 1000;
            } else {
                return Integer.parseInt(timeout);
            }
        } catch (NumberFormatException e) {
            log.warn("解析超时时间失败：{}，使用默认值3000ms", timeout);
            return 3000;
        }
    }

    /**
     * 创建Redisson缓存管理器
     * 可以与Spring Cache注解集成使用
     *
     * @param redissonClient Redisson客户端
     * @return Redisson缓存管理器
     */
    @Bean
    @Primary
    public CacheManager redissonCacheManager(RedissonClient redissonClient) {
        log.info("初始化Redisson缓存管理器");
        
        Map<String, CacheConfig> config = new HashMap<>();
        
        // 配置不同缓存空间的过期时间和最大空闲时间
        // 用户缓存：30分钟过期，10分钟最大空闲
        config.put("user", new CacheConfig(30 * 60 * 1000, 10 * 60 * 1000));
        
        // 角色缓存：1小时过期，30分钟最大空闲
        config.put("role", new CacheConfig(60 * 60 * 1000, 30 * 60 * 1000));
        
        // 对话缓存：10分钟过期，5分钟最大空闲
        config.put("conversation", new CacheConfig(10 * 60 * 1000, 5 * 60 * 1000));
        
        // 配置缓存：2小时过期，1小时最大空闲
        config.put("config", new CacheConfig(2 * 60 * 60 * 1000, 60 * 60 * 1000));
        
        // Token缓存：24小时过期，不设置最大空闲时间
        config.put("token", new CacheConfig(24 * 60 * 60 * 1000, 0));
        
        // 临时缓存：5分钟过期，2分钟最大空闲
        config.put("temp", new CacheConfig(5 * 60 * 1000, 2 * 60 * 1000));
        
        // 限流缓存：1分钟过期（用于API限流）
        config.put("rateLimit", new CacheConfig(60 * 1000, 0));
        
        // 分布式锁缓存：30秒过期（用于短期锁）
        config.put("lock", new CacheConfig(30 * 1000, 0));
        
        RedissonSpringCacheManager cacheManager = new RedissonSpringCacheManager(redissonClient, config);
        
        log.info("Redisson缓存管理器初始化完成，配置了{}个缓存空间", config.size());
        return cacheManager;
    }

    /**
     * 获取Redisson配置信息（用于健康检查和监控）
     *
     * @param redissonClient Redisson客户端
     * @return 配置信息Map
     */
    @Bean
    public Map<String, Object> redissonInfo(RedissonClient redissonClient) {
        Map<String, Object> info = new HashMap<>();
        info.put("host", redisHost);
        info.put("port", redisPort);
        info.put("database", redisDatabase);
        info.put("hasPassword", StringUtils.hasText(redisPassword));
        info.put("maxConnections", maxActive);
        info.put("minIdleConnections", minIdle);
        
        try {
            // 测试Redisson功能 - 使用一个简单的键操作来测试连接
            redissonClient.getBucket("test:ping").set("pong");
            redissonClient.getBucket("test:ping").delete();
            info.put("connected", true);
            info.put("redissonVersion", Redisson.class.getPackage().getImplementationVersion());
        } catch (Exception e) {
            info.put("connected", false);
            info.put("error", e.getMessage());
        }
        
        log.info("Redisson配置信息: {}", info);
        return info;
    }
}
