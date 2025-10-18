package com.nexusvoice.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.config.service.SystemConfigCacheService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 系统配置缓存事件监听器
 * 监听Redis Pub/Sub消息，处理缓存失效事件
 * 
 * 工作原理：
 * 1. 监听Redis频道 "system:config:evict"
 * 2. 收到消息后解析配置键
 * 3. 清除本地Caffeine缓存
 * 4. 下次读取时重新从Redis/DB加载
 *
 * @author NexusVoice
 * @since 2025-10-18
 */
@Slf4j
@Configuration
public class SystemConfigCacheEventListener {

    private static final String CACHE_EVICT_CHANNEL = "system:config:evict";

    @Autowired
    private SystemConfigCacheService cacheService;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    /**
     * 初始化方法，注册监听器
     */
    @PostConstruct
    public void init() {
        // 创建消息监听器
        MessageListenerAdapter messageListener = new MessageListenerAdapter(new MessageHandler());
        messageListener.setDefaultListenerMethod("handleMessage");
        
        // 订阅系统配置缓存失效频道
        redisMessageListenerContainer.addMessageListener(
                messageListener,
                new PatternTopic(CACHE_EVICT_CHANNEL)
        );
        
        log.info("系统配置缓存监听器已注册到Redis消息监听容器，订阅频道: {}", CACHE_EVICT_CHANNEL);
    }
    
    /**
     * 内部消息处理类
     */
    private class MessageHandler {
        @SuppressWarnings("unused") // 通过反射调用，IDE误报警告
        public void handleMessage(String message) {
            try {
                log.debug("收到Redis缓存失效消息: {}", message);
                
                // 解析消息
                @SuppressWarnings("unchecked")
                Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
                
                String sourceInstanceId = (String) eventData.get("sourceInstanceId");
                Boolean clearAll = (Boolean) eventData.get("clearAll");
                
                @SuppressWarnings("unchecked")
                Set<String> configKeys = new HashSet<>((Set<String>) eventData.getOrDefault("configKeys", new HashSet<>()));
                
                // 委托给CacheService处理
                cacheService.handleCacheEvictEvent(
                        sourceInstanceId,
                        configKeys.isEmpty() ? null : configKeys,
                        Boolean.TRUE.equals(clearAll)
                );
                
            } catch (Exception e) {
                log.error("处理Redis缓存失效消息失败: {}", message, e);
            }
        }
    }
}
