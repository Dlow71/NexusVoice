package com.nexusvoice.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis配置类
 * 负责配置Redis序列化器、RedisTemplate、缓存管理器等核心组件
 *
 * @author NexusVoice
 * @since 2025-10-17
 */
@Configuration
@EnableCaching
@ConditionalOnClass(RedisOperations.class)
@ConditionalOnProperty(prefix = "spring.data.redis", name = "host")
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * 配置Jackson序列化器
     * 使用Jackson2序列化对象，提供良好的兼容性和性能
     * 基于全局ObjectMapper进行克隆，添加Redis专用配置
     *
     * @param globalObjectMapper 全局ObjectMapper（来自JacksonConfig）
     * @return Jackson序列化器
     */
    @Bean
    public RedisSerializer<Object> jackson2JsonRedisSerializer(ObjectMapper globalObjectMapper) {
        log.info("初始化Redis Jackson序列化器，基于全局ObjectMapper配置");
        
        // 克隆全局ObjectMapper，避免影响原有配置
        ObjectMapper redisObjectMapper = globalObjectMapper.copy();
        
        // 指定要序列化的域，field、get和set，以及修饰符范围
        redisObjectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // 启用多态类型处理，存储类型信息（与Spring Security兼容）
        // 这是Redis专用配置，全局ObjectMapper不需要
        redisObjectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        // 创建序列化器
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);
        
        log.info("Redis Jackson序列化器初始化完成");
        return serializer;
    }

    /**
     * 配置RedisTemplate
     * 用于操作Redis的核心模板类，支持对象序列化
     *
     * @param connectionFactory Redis连接工厂
     * @param jackson2JsonRedisSerializer Jackson序列化器
     * @return RedisTemplate实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            RedisSerializer<Object> jackson2JsonRedisSerializer) {
        
        log.info("初始化RedisTemplate");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        // 初始化RedisTemplate
        template.afterPropertiesSet();
        
        log.info("RedisTemplate初始化完成");
        return template;
    }

    /**
     * 配置StringRedisTemplate
     * 专门用于处理字符串类型的Redis操作
     *
     * @param connectionFactory Redis连接工厂
     * @return StringRedisTemplate实例
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("初始化StringRedisTemplate");
        
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        
        // 使用StringRedisSerializer来序列化和反序列化redis的key和value
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(stringRedisSerializer);
        
        template.afterPropertiesSet();
        
        log.info("StringRedisTemplate初始化完成");
        return template;
    }

    /**
     * 配置Redis缓存管理器
     * 用于管理Spring Cache注解的缓存操作
     *
     * @param connectionFactory Redis连接工厂
     * @param jackson2JsonRedisSerializer Jackson序列化器
     * @return 缓存管理器
     */
    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            RedisSerializer<Object> jackson2JsonRedisSerializer) {
        
        log.info("初始化Redis缓存管理器");
        
        // 配置序列化（解决乱码的问题）
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 缓存默认有效期1小时
                .entryTtl(Duration.ofHours(1))
                // 使用StringRedisSerializer来序列化和反序列化redis的key值
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jackson2JsonRedisSerializer))
                // 禁用空值缓存
                .disableCachingNullValues()
                // 缓存key前缀
                .prefixCacheNameWith("nexusvoice:cache:");

        // 设置特定缓存空间的配置
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        
        // 用户缓存：30分钟
        configMap.put("user", config.entryTtl(Duration.ofMinutes(30)));
        
        // 角色缓存：1小时
        configMap.put("role", config.entryTtl(Duration.ofHours(1)));
        
        // 对话缓存：10分钟
        configMap.put("conversation", config.entryTtl(Duration.ofMinutes(10)));
        
        // 系统配置缓存：2小时
        configMap.put("config", config.entryTtl(Duration.ofHours(2)));
        
        // Token缓存：24小时
        configMap.put("token", config.entryTtl(Duration.ofHours(24)));
        
        // 短期缓存：5分钟（用于临时数据）
        configMap.put("temp", config.entryTtl(Duration.ofMinutes(5)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(configMap)
                .transactionAware()
                .build();

        log.info("Redis缓存管理器初始化完成，配置了{}个自定义缓存空间", configMap.size());
        return cacheManager;
    }

    /**
     * 配置Redis消息监听容器
     * 用于监听Redis的Key过期事件等
     *
     * @param connectionFactory Redis连接工厂
     * @return Redis消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        
        log.info("初始化Redis消息监听容器");
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // 可以在这里添加消息监听器
        // 例如：监听key过期事件
        // container.addMessageListener(keyExpirationListener, new PatternTopic("__keyevent@*__:expired"));
        
        log.info("Redis消息监听容器初始化完成");
        return container;
    }
}
