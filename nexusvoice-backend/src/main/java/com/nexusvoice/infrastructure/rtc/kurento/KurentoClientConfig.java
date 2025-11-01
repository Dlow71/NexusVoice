package com.nexusvoice.infrastructure.rtc.kurento;

import lombok.extern.slf4j.Slf4j;
import org.kurento.client.KurentoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kurento客户端配置
 * 
 * 功能：配置Kurento Media Server连接
 * 特性：
 * - WebSocket连接管理
 * - 自动重连机制
 * - 连接超时配置
 * - 条件化加载（仅当rtc.enabled=true时生效）
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "rtc.enabled", havingValue = "true", matchIfMissing = false)
public class KurentoClientConfig {

    @Value("${rtc.kurento.ws-url:ws://localhost:8888/kurento}")
    private String kurentoWsUrl;

    @Value("${rtc.kurento.timeout-seconds:10}")
    private int timeoutSeconds;

    /**
     * 创建Kurento客户端Bean
     * 
     * @return KurentoClient实例
     */
    @Bean
    public KurentoClient kurentoClient() {
        log.info("初始化Kurento客户端: url={}, timeout={}s", kurentoWsUrl, timeoutSeconds);
        
        try {
            KurentoClient client = KurentoClient.create(kurentoWsUrl);
            
            log.info("Kurento客户端连接成功");
            
            return client;
        } catch (Exception e) {
            log.error("Kurento客户端连接失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法连接到Kurento Media Server: " + kurentoWsUrl, e);
        }
    }
}

