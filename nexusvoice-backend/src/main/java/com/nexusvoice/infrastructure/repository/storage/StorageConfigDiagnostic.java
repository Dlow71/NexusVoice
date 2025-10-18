package com.nexusvoice.infrastructure.repository.storage;

import com.nexusvoice.domain.config.service.SystemConfigService;
import com.nexusvoice.domain.storage.enums.StorageProvider;
import com.nexusvoice.domain.storage.model.QiniuStorageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 存储配置诊断工具
 * 启动时输出详细的存储配置信息，帮助排查问题
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
@Slf4j
@Component
public class StorageConfigDiagnostic implements CommandLineRunner {
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("存储配置诊断工具启动");
        log.info("========================================");
        
        // 诊断基础配置
        diagnoseBaseConfig();
        
        // 诊断七牛云配置
        diagnoseQiniuConfig();
        
        // 诊断MinIO配置
        diagnoseMinioConfig();
        
        log.info("========================================");
        log.info("存储配置诊断完成");
        log.info("========================================");
    }
    
    private void diagnoseBaseConfig() {
        log.info("--- 基础存储配置 ---");
        String provider = systemConfigService.getString("storage.provider", "未配置");
        Boolean fallbackEnabled = systemConfigService.getBoolean("storage.fallback.enabled", false);
        String fallbackProvider = systemConfigService.getString("storage.fallback.provider", "未配置");
        
        log.info("默认存储提供商: {}", provider);
        log.info("备用存储启用: {}", fallbackEnabled);
        log.info("备用存储提供商: {}", fallbackProvider);
    }
    
    private void diagnoseQiniuConfig() {
        log.info("--- 七牛云存储配置 ---");
        
        Boolean enabled = systemConfigService.getBoolean("storage.qiniu.enabled", false);
        String accessKey = systemConfigService.getString("storage.qiniu.access_key", "");
        String secretKey = systemConfigService.getString("storage.qiniu.secret_key", "");
        String bucket = systemConfigService.getString("storage.qiniu.bucket", "");
        String domain = systemConfigService.getString("storage.qiniu.domain", "");
        String region = systemConfigService.getString("storage.qiniu.region", "");
        
        log.info("启用状态: {}", enabled);
        log.info("AccessKey: {} (长度: {})", maskSensitive(accessKey), accessKey.length());
        log.info("SecretKey: {} (长度: {})", maskSensitive(secretKey), secretKey.length());
        log.info("Bucket: {}", bucket.isEmpty() ? "❌ 未配置" : bucket);
        log.info("Domain: {}", domain.isEmpty() ? "❌ 未配置" : domain);
        log.info("Region: {}", region);
        
        // 验证配置有效性
        QiniuStorageConfig config = new QiniuStorageConfig();
        config.setEnabled(enabled);
        config.setAccessKey(accessKey);
        config.setSecretKey(secretKey);
        config.setBucket(bucket);
        config.setDomain(domain);
        
        boolean isValid = config.isValid();
        log.info("配置验证结果: {}", isValid ? "✓ 通过" : "❌ 不通过");
        
        if (!isValid) {
            log.error("七牛云配置无效，原因：");
            if (!enabled) {
                log.error("  - enabled 为 false");
            }
            if (accessKey == null || accessKey.isEmpty()) {
                log.error("  - accessKey 为空");
            }
            if (secretKey == null || secretKey.isEmpty()) {
                log.error("  - secretKey 为空");
            }
            if (bucket == null || bucket.isEmpty()) {
                log.error("  - bucket 为空");
            }
            if (domain == null || domain.isEmpty()) {
                log.error("  - domain 为空 ⚠️ 这是必填项！");
            }
        }
    }
    
    private void diagnoseMinioConfig() {
        log.info("--- MinIO存储配置 ---");
        
        Boolean enabled = systemConfigService.getBoolean("storage.minio.enabled", false);
        String endpoint = systemConfigService.getString("storage.minio.endpoint", "");
        String accessKey = systemConfigService.getString("storage.minio.access_key", "");
        String secretKey = systemConfigService.getString("storage.minio.secret_key", "");
        String bucket = systemConfigService.getString("storage.minio.bucket", "");
        
        log.info("启用状态: {}", enabled);
        log.info("Endpoint: {}", endpoint.isEmpty() ? "❌ 未配置" : endpoint);
        log.info("AccessKey: {} (长度: {})", maskSensitive(accessKey), accessKey.length());
        log.info("SecretKey: {} (长度: {})", maskSensitive(secretKey), secretKey.length());
        log.info("Bucket: {}", bucket.isEmpty() ? "❌ 未配置" : bucket);
    }
    
    /**
     * 脱敏显示敏感信息
     */
    private String maskSensitive(String value) {
        if (value == null || value.isEmpty()) {
            return "❌ 未配置";
        }
        if (value.length() <= 8) {
            return "***" + value.substring(value.length() - 2);
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
