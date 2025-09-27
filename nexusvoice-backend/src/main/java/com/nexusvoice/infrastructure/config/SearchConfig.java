package com.nexusvoice.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 搜索功能配置
 *
 * @author NexusVoice
 * @since 2025-09-27
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "nexusvoice.search.enabled", havingValue = "true", matchIfMissing = true)
public class SearchConfig {
    
    /**
     * HTTP客户端请求工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 设置连接超时时间
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        // 设置读取超时时间
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        return factory;
    }
    
    /**
     * REST模板用于HTTP请求
     */
    @Bean
    @ConditionalOnMissingBean(name = "searchRestTemplate")
    public RestTemplate searchRestTemplate(ClientHttpRequestFactory clientHttpRequestFactory) {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        log.info("搜索服务RestTemplate配置完成，连接超时：10s，读取超时：30s");
        return restTemplate;
    }
    
}
