package com.nexusvoice.infrastructure.repository.storage;

import com.nexusvoice.domain.config.service.SystemConfigService;
import com.nexusvoice.domain.storage.enums.StorageProvider;
import com.nexusvoice.domain.storage.model.MinioStorageConfig;
import com.nexusvoice.domain.storage.model.QiniuStorageConfig;
import com.nexusvoice.domain.storage.model.StorageConfig;
import com.nexusvoice.domain.storage.repository.StorageRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储策略管理器
 * 负责管理所有存储实例，根据配置动态选择存储提供商
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
@Slf4j
@Component
public class StorageStrategyManager {
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    /**
     * 存储实例缓存
     * key: StorageProvider
     * value: StorageRepository实例
     */
    private final Map<StorageProvider, StorageRepository> repositoryCache = new ConcurrentHashMap<>();
    
    /**
     * 当前默认存储提供商
     */
    private volatile StorageProvider defaultProvider;
    
    /**
     * 备用存储提供商
     */
    private volatile StorageProvider fallbackProvider;
    
    /**
     * 是否启用备用存储
     */
    private volatile boolean fallbackEnabled;
    
    /**
     * 健康检查间隔（秒）
     */
    private volatile int healthCheckInterval;
    
    /**
     * 最后一次健康检查时间
     */
    private volatile long lastHealthCheckTime = 0;
    
    @PostConstruct
    public void init() {
        log.info("初始化存储策略管理器");
        
        // 加载配置
        loadConfiguration();
        
        // 初始化存储实例
        initializeStorageRepositories();
        
        // 检查是否至少有一个存储服务可用
        if (repositoryCache.isEmpty()) {
            log.error("存储服务初始化失败：没有任何可用的存储服务！请检查数据库配置：");
            log.error("  1. 检查 storage.qiniu.enabled 或 storage.minio.enabled 是否为 true");
            log.error("  2. 检查存储配置是否完整（accessKey、secretKey、domain等）");
            log.error("  3. 七牛云必填项：access_key、secret_key、bucket、domain");
            log.error("  4. MinIO必填项：endpoint、access_key、secret_key、bucket");
            throw BizException.of(ErrorCodeEnum.SYSTEM_ERROR, 
                "存储服务初始化失败：没有任何可用的存储服务，请检查数据库配置");
        }
        
        // 执行健康检查
        performHealthCheck();
        
        log.info("存储策略管理器初始化完成，默认存储：{}，备用存储：{}，可用存储数：{}", 
                defaultProvider, fallbackEnabled ? fallbackProvider : "未启用", repositoryCache.size());
    }
    
    @PreDestroy
    public void destroy() {
        log.info("销毁存储策略管理器");
        
        // 销毁所有存储实例
        repositoryCache.values().forEach(repo -> {
            try {
                repo.destroy();
            } catch (Exception e) {
                log.error("销毁存储实例失败：{}", e.getMessage());
            }
        });
        
        repositoryCache.clear();
    }
    
    /**
     * 获取默认存储实例（动态读取配置）
     */
    public StorageRepository getDefaultRepository() {
        // 每次都从数据库读取最新的存储提供商配置
        String currentProviderCode = systemConfigService.getString("storage.provider", "qiniu");
        StorageProvider currentProvider = StorageProvider.fromCode(currentProviderCode);
        
        // 如果配置变更了，更新缓存的provider
        if (currentProvider != defaultProvider) {
            log.info("检测到存储提供商配置变更：{} -> {}", defaultProvider, currentProvider);
            defaultProvider = currentProvider;
            
            // 如果新的provider还没有初始化，立即初始化
            if (!repositoryCache.containsKey(currentProvider)) {
                log.info("初始化新的存储提供商：{}", currentProvider);
                initializeStorageRepositories();
            }
        }
        
        // 检查是否需要执行健康检查
        checkAndPerformHealthCheck();
        
        StorageRepository repository = repositoryCache.get(currentProvider);
        
        // 如果默认存储不可用，尝试使用备用存储
        if (repository == null || !repository.isAvailable()) {
            log.error("默认存储服务不可用：{}", currentProvider);
            
            if (repository == null) {
                log.error("存储实例未初始化，可能原因：配置不完整或未启用");
                log.error("当前可用的存储服务：{}", repositoryCache.keySet());
            } else {
                log.error("存储服务健康检查失败，可能原因：网络不通或认证失败");
            }
            
            // 动态读取备用存储配置
            boolean currentFallbackEnabled = systemConfigService.getBoolean("storage.fallback.enabled", false);
            if (currentFallbackEnabled) {
                String fallbackCode = systemConfigService.getString("storage.fallback.provider", "minio");
                StorageProvider currentFallbackProvider = StorageProvider.fromCode(fallbackCode);
                
                repository = repositoryCache.get(currentFallbackProvider);
                
                if (repository != null && repository.isAvailable()) {
                    log.info("切换到备用存储服务：{}", currentFallbackProvider);
                    return repository;
                } else {
                    log.error("备用存储服务也不可用：{}", currentFallbackProvider);
                }
            } else {
                log.error("备用存储未启用，设置 storage.fallback.enabled=true 可启用备用存储");
            }
            
            throw BizException.of(ErrorCodeEnum.SYSTEM_ERROR, 
                String.format("存储服务不可用：%s（请检查数据库system_config表的存储配置）", currentProvider.getName()));
        }
        
        return repository;
    }
    
    /**
     * 根据存储类型获取存储实例
     */
    public StorageRepository getRepository(StorageProvider provider) {
        if (provider == null) {
            return getDefaultRepository();
        }
        
        StorageRepository repository = repositoryCache.get(provider);
        
        if (repository == null) {
            throw BizException.of(ErrorCodeEnum.SYSTEM_ERROR, 
                    "存储服务未配置或未启用：" + provider.getName());
        }
        
        if (!repository.isAvailable()) {
            throw BizException.of(ErrorCodeEnum.SYSTEM_ERROR, 
                    "存储服务不可用：" + provider.getName());
        }
        
        return repository;
    }
    
    /**
     * 根据名称获取存储实例
     */
    public StorageRepository getRepositoryByName(String name) {
        if (name == null || name.isEmpty()) {
            return getDefaultRepository();
        }
        
        try {
            StorageProvider provider = StorageProvider.fromCode(name.toLowerCase());
            return getRepository(provider);
        } catch (IllegalArgumentException e) {
            log.error("不支持的存储提供商：{}", name);
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "不支持的存储提供商：" + name);
        }
    }
    
    /**
     * 注册存储实例
     */
    public void registerRepository(StorageProvider provider, StorageRepository repository) {
        if (provider == null || repository == null) {
            return;
        }
        
        // 销毁旧实例
        StorageRepository oldRepository = repositoryCache.get(provider);
        if (oldRepository != null) {
            oldRepository.destroy();
        }
        
        // 注册新实例
        repositoryCache.put(provider, repository);
        log.info("注册存储实例成功：{}", provider.getName());
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfiguration() {
        log.info("重新加载存储配置");
        
        // 清除缓存，强制从数据库重新加载
        systemConfigService.clearCache();
        
        // 重新加载配置
        loadConfiguration();
        
        // 重新初始化存储实例
        initializeStorageRepositories();
        
        // 执行健康检查
        performHealthCheck();
    }
    
    /**
     * 加载配置
     */
    private void loadConfiguration() {
        // 获取默认存储提供商
        String providerCode = systemConfigService.getString("storage.provider", "qiniu");
        defaultProvider = StorageProvider.fromCode(providerCode);
        
        // 获取备用存储配置
        fallbackEnabled = systemConfigService.getBoolean("storage.fallback.enabled", false);
        if (fallbackEnabled) {
            String fallbackCode = systemConfigService.getString("storage.fallback.provider", "minio");
            fallbackProvider = StorageProvider.fromCode(fallbackCode);
        }
        
        // 获取健康检查配置
        boolean healthCheckEnabled = systemConfigService.getBoolean("storage.health.check.enabled", true);
        healthCheckInterval = healthCheckEnabled ? 
                systemConfigService.getInt("storage.health.check.interval", 300) : 0;
    }
    
    /**
     * 初始化存储实例
     */
    private void initializeStorageRepositories() {
        // 初始化七牛云存储
        if (systemConfigService.getBoolean("storage.qiniu.enabled", false)) {
            QiniuStorageConfig qiniuConfig = loadQiniuConfig();
            if (qiniuConfig.isValid()) {
                registerRepository(StorageProvider.QINIU, new QiniuStorageRepositoryImpl(qiniuConfig));
                log.info("七牛云存储初始化成功");
            } else {
                log.error("七牛云存储配置无效，跳过初始化！请检查数据库配置：");
                log.error("  - storage.qiniu.access_key: {}", 
                    qiniuConfig.getAccessKey() == null || qiniuConfig.getAccessKey().isEmpty() ? "❌ 未配置" : "✓ 已配置");
                log.error("  - storage.qiniu.secret_key: {}", 
                    qiniuConfig.getSecretKey() == null || qiniuConfig.getSecretKey().isEmpty() ? "❌ 未配置" : "✓ 已配置");
                log.error("  - storage.qiniu.bucket: {}", 
                    qiniuConfig.getBucket() == null || qiniuConfig.getBucket().isEmpty() ? "❌ 未配置" : qiniuConfig.getBucket());
                log.error("  - storage.qiniu.domain: {}", 
                    qiniuConfig.getDomain() == null || qiniuConfig.getDomain().isEmpty() ? "❌ 未配置（必填！）" : qiniuConfig.getDomain());
            }
        }
        
        // 初始化MinIO存储
        if (systemConfigService.getBoolean("storage.minio.enabled", false)) {
            MinioStorageConfig minioConfig = loadMinioConfig();
            if (minioConfig.isValid()) {
                registerRepository(StorageProvider.MINIO, new MinioStorageRepositoryImpl(minioConfig));
                log.info("MinIO存储初始化成功");
            } else {
                log.error("MinIO存储配置无效，跳过初始化！请检查数据库配置：");
                log.error("  - storage.minio.endpoint: {}", minioConfig.getEndpoint());
                log.error("  - storage.minio.access_key: {}", 
                    minioConfig.getAccessKey() == null || minioConfig.getAccessKey().isEmpty() ? "❌ 未配置" : "✓ 已配置");
                log.error("  - storage.minio.secret_key: {}", 
                    minioConfig.getSecretKey() == null || minioConfig.getSecretKey().isEmpty() ? "❌ 未配置" : "✓ 已配置");
                log.error("  - storage.minio.bucket: {}", minioConfig.getBucket());
            }
        }
    }
    
    /**
     * 加载七牛云配置
     */
    private QiniuStorageConfig loadQiniuConfig() {
        QiniuStorageConfig config = new QiniuStorageConfig();
        config.setEnabled(systemConfigService.getBoolean("storage.qiniu.enabled", false));
        config.setAccessKey(systemConfigService.getString("storage.qiniu.access_key", ""));
        config.setSecretKey(systemConfigService.getString("storage.qiniu.secret_key", ""));
        config.setBucket(systemConfigService.getString("storage.qiniu.bucket", "nexusvoice"));
        config.setDomain(systemConfigService.getString("storage.qiniu.domain", ""));
        config.setRegion(systemConfigService.getString("storage.qiniu.region", "auto"));
        config.setUseSSL(systemConfigService.getBoolean("storage.qiniu.use_https", true));
        
        // 加载目录配置
        Map<String, String> directories = new HashMap<>();
        directories.put("audio", systemConfigService.getString("storage.qiniu.dir.audio", "audio/"));
        directories.put("image", systemConfigService.getString("storage.qiniu.dir.image", "image/"));
        directories.put("video", systemConfigService.getString("storage.qiniu.dir.video", "video/"));
        directories.put("other", systemConfigService.getString("storage.qiniu.dir.other", "other/"));
        config.setDirectories(directories);
        
        return config;
    }
    
    /**
     * 加载MinIO配置
     */
    private MinioStorageConfig loadMinioConfig() {
        MinioStorageConfig config = new MinioStorageConfig();
        config.setEnabled(systemConfigService.getBoolean("storage.minio.enabled", false));
        config.setEndpoint(systemConfigService.getString("storage.minio.endpoint", "http://localhost:9000"));
        config.setAccessKey(systemConfigService.getString("storage.minio.access_key", "minioadmin"));
        config.setSecretKey(systemConfigService.getString("storage.minio.secret_key", "minioadmin"));
        config.setBucket(systemConfigService.getString("storage.minio.bucket", "nexusvoice"));
        config.setRegion(systemConfigService.getString("storage.minio.region", "us-east-1"));
        config.setPublicUrl(systemConfigService.getString("storage.minio.public_url", ""));
        config.setUseSSL(systemConfigService.getBoolean("storage.minio.use_ssl", false));
        
        // 加载目录配置
        Map<String, String> directories = new HashMap<>();
        directories.put("audio", systemConfigService.getString("storage.minio.dir.audio", "audio/"));
        directories.put("image", systemConfigService.getString("storage.minio.dir.image", "image/"));
        directories.put("video", systemConfigService.getString("storage.minio.dir.video", "video/"));
        directories.put("other", systemConfigService.getString("storage.minio.dir.other", "other/"));
        config.setDirectories(directories);
        
        return config;
    }
    
    /**
     * 检查并执行健康检查
     */
    private void checkAndPerformHealthCheck() {
        if (healthCheckInterval <= 0) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHealthCheckTime > healthCheckInterval * 1000L) {
            performHealthCheck();
            lastHealthCheckTime = currentTime;
        }
    }
    
    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        log.debug("执行存储服务健康检查");
        
        repositoryCache.forEach((provider, repository) -> {
            boolean isAvailable = repository.isAvailable();
            log.debug("存储服务 {} 健康状态：{}", provider.getName(), isAvailable ? "正常" : "异常");
        });
    }
    
    /**
     * 获取所有已注册的存储提供商
     */
    public Map<StorageProvider, Boolean> getAllProviderStatus() {
        Map<StorageProvider, Boolean> status = new HashMap<>();
        
        repositoryCache.forEach((provider, repository) -> {
            status.put(provider, repository.isAvailable());
        });
        
        return status;
    }
}
