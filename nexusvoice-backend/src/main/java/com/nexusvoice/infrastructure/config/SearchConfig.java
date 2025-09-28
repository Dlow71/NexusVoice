package com.nexusvoice.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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
        // 连接池配置
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(20);
        cm.closeIdle(TimeValue.ofSeconds(30));

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeouts.CONNECT)
                .setResponseTimeout(Timeouts.READ)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        // 冗余保护（Spring 侧超时）
        factory.setConnectTimeout((int) Timeouts.CONNECT.toMilliseconds());
        return factory;
    }
    
    /**
     * REST模板用于HTTP请求
     */
    @Bean
    @ConditionalOnMissingBean(name = "searchRestTemplate")
    public RestTemplate searchRestTemplate(ClientHttpRequestFactory clientHttpRequestFactory) {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        log.info("搜索服务RestTemplate配置完成，连接池启用，连接超时：{}，读取超时：{}", Timeouts.CONNECT, Timeouts.READ);
        return restTemplate;
    }

    // 内部超时常量
    private static final class Timeouts {
        static final org.apache.hc.core5.util.Timeout CONNECT = org.apache.hc.core5.util.Timeout.ofSeconds(5);
        static final org.apache.hc.core5.util.Timeout READ = org.apache.hc.core5.util.Timeout.ofSeconds(10);
    }
    
}
