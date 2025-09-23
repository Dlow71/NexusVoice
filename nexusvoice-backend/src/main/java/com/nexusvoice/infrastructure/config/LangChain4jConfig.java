package com.nexusvoice.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j配置类
 * 用于处理LangChain4j与MyBatis-Plus的兼容性问题
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Configuration
@ConditionalOnProperty(name = "langchain4j.enabled", havingValue = "true", matchIfMissing = false)
public class LangChain4jConfig {
    
    // 暂时禁用LangChain4j的自动配置，避免与MyBatis冲突
    // 后续需要时可以手动配置相关Bean
    
}
