package com.nexusvoice.infrastructure.config;

import com.nexusvoice.infrastructure.mq.interceptor.MessageTraceInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ配置类
 * 
 * @author Dlow
 * @date 2025/10/18
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class RocketMQConfig {
    
    /**
     * 消息追踪拦截器
     */
    @Bean
    public MessageTraceInterceptor messageTraceInterceptor() {
        log.info("RocketMQ消息追踪拦截器配置完成");
        return new MessageTraceInterceptor();
    }
    
    /**
     * 说明：
     * 1. RocketMQ Spring Boot Starter 2.3.0会自动配置RocketMQTemplate
     * 2. RocketMQTemplate会自动使用Spring容器中的ObjectMapper
     * 3. 消息序列化和反序列化会使用我们统一配置的Jackson ObjectMapper
     * 4. 不需要额外配置RocketMQMessageConverter
     */
}
