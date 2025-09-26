package com.nexusvoice.infrastructure.ai.config;

import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.infrastructure.ai.service.impl.DefaultAiChatService;
import com.nexusvoice.infrastructure.ai.service.impl.OpenAiChatServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务装配配置
 * 提供一个兜底的 AiChatService Bean，确保在未配置实际模型实现时系统仍可启动。
 */
//@Configuration
public class AiServiceConfig {


    /**
     * 兜底的 AiChatService，总是提供一个可用 Bean，
     * 若存在更高优先级（@Primary）的真实实现，将自动被优先注入。
     */
    @Bean
    public AiChatService aiChatServiceFallback() {
        return new DefaultAiChatService();
    }
}
