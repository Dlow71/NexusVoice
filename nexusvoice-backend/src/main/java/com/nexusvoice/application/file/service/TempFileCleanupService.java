package com.nexusvoice.application.file.service;

import com.nexusvoice.domain.config.service.SystemConfigService;
import com.nexusvoice.domain.file.model.TempFile;
import com.nexusvoice.domain.file.model.TempFile.FileCategory;
import com.nexusvoice.domain.file.model.TempFile.StorageProvider;
import com.nexusvoice.domain.file.repository.TempFileRepository;
import com.nexusvoice.domain.storage.repository.StorageRepository;
import com.nexusvoice.infrastructure.repository.storage.StorageStrategyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 临时文件清理服务
 * 负责各类临时文件的注册和异步清理（TTS音频、图片、PDF等）
 * 支持多种存储提供商（七牛云、MinIO等）
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
@Slf4j
@Service
public class TempFileCleanupService {
    
    private final TempFileRepository tempFileRepository;
    private final StorageStrategyManager storageStrategyManager;
    private final SystemConfigService systemConfigService;
    private final ExecutorService commonVirtualThreadExecutor;
    
    /**
     * 默认TTL：24小时
     */
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    
    public TempFileCleanupService(TempFileRepository tempFileRepository,
                                  StorageStrategyManager storageStrategyManager,
                                  SystemConfigService systemConfigService,
                                  @Qualifier("commonVirtualThreadExecutor") ExecutorService commonVirtualThreadExecutor) {
        this.tempFileRepository = tempFileRepository;
        this.storageStrategyManager = storageStrategyManager;
        this.systemConfigService = systemConfigService;
        this.commonVirtualThreadExecutor = commonVirtualThreadExecutor;
    }
    
    /**
     * 注册临时文件（使用默认TTL和当前存储提供商）
     * 
     * @param fileUrl 文件URL
     * @param category 文件分类
     * @param subType 文件子类型
     * @param conversationId 对话ID（可选）
     * @param fileSize 文件大小
     */
    public void registerTempFile(String fileUrl, FileCategory category, String subType,
                                 Long conversationId, Long fileSize) {
        registerTempFile(fileUrl, category, subType, conversationId, fileSize, DEFAULT_TTL);
    }
    
    /**
     * 注册临时文件（使用自定义TTL和当前存储提供商）
     * 
     * @param fileUrl 文件URL
     * @param category 文件分类
     * @param subType 文件子类型
     * @param conversationId 对话ID（可选）
     * @param fileSize 文件大小
     * @param ttl 存活时间
     */
    public void registerTempFile(String fileUrl, FileCategory category, String subType,
                                 Long conversationId, Long fileSize, Duration ttl) {
        // 从系统配置获取当前存储提供商
        String storageProviderCode = systemConfigService.getString("storage.provider", "qiniu");
        StorageProvider storageProvider = StorageProvider.fromCode(storageProviderCode);
        
        registerTempFile(fileUrl, category, subType, storageProvider, conversationId, null, fileSize, ttl);
    }
    
    /**
     * 注册临时文件（完整参数）
     * 
     * @param fileUrl 文件URL
     * @param category 文件分类
     * @param subType 文件子类型
     * @param storageProvider 存储提供商
     * @param conversationId 对话ID（可选）
     * @param userId 用户ID（可选）
     * @param fileSize 文件大小
     * @param ttl 存活时间
     */
    public void registerTempFile(String fileUrl, FileCategory category, String subType,
                                 StorageProvider storageProvider, Long conversationId, 
                                 Long userId, Long fileSize, Duration ttl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            log.warn("文件URL为空，跳过注册临时文件");
            return;
        }
        
        if (category == null) {
            log.warn("文件分类为空，跳过注册临时文件");
            return;
        }
        
        try {
            String fileKey = extractFileKeyFromUrl(fileUrl);
            if (fileKey == null || fileKey.isEmpty()) {
                log.warn("无法从URL提取fileKey，跳过注册：{}", fileUrl);
                return;
            }
            
            TempFile tempFile = TempFile.builder()
                    .fileUrl(fileUrl)
                    .fileKey(fileKey)
                    .fileCategory(category)
                    .fileSubType(subType)
                    .storageProvider(storageProvider != null ? storageProvider : StorageProvider.QINIU)
                    .createdAt(LocalDateTime.now())
                    .expireAt(LocalDateTime.now().plus(ttl))
                    .conversationId(conversationId)
                    .userId(userId)
                    .fileSize(fileSize)
                    .build();
            
            tempFileRepository.save(tempFile, ttl);
            
            log.debug("注册临时文件成功，fileKey: {}, 分类: {}, 存储: {}, TTL: {}小时", 
                fileKey, category, storageProvider, ttl.toHours());
                
        } catch (Exception e) {
            log.error("注册临时文件失败，fileUrl: {}", fileUrl, e);
        }
    }
    
    /**
     * 批量注册临时文件
     * 
     * @param fileUrls 文件URL列表
     * @param category 文件分类
     * @param subType 文件子类型
     * @param conversationId 对话ID（可选）
     */
    public void registerTempFileBatch(List<String> fileUrls, FileCategory category, String subType,
                                      Long conversationId) {
        registerTempFileBatch(fileUrls, category, subType, conversationId, DEFAULT_TTL);
    }
    
    /**
     * 批量注册临时文件
     * 
     * @param fileUrls 文件URL列表
     * @param category 文件分类
     * @param subType 文件子类型
     * @param conversationId 对话ID（可选）
     * @param ttl 存活时间
     */
    public void registerTempFileBatch(List<String> fileUrls, FileCategory category, String subType,
                                      Long conversationId, Duration ttl) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return;
        }
        
        log.info("批量注册{}个临时文件，分类: {}, TTL: {}小时", 
            fileUrls.size(), category, ttl.toHours());
        
        // 从系统配置获取当前存储提供商
        String storageProviderCode = systemConfigService.getString("storage.provider", "qiniu");
        StorageProvider storageProvider = StorageProvider.fromCode(storageProviderCode);
        
        List<TempFile> tempFiles = new ArrayList<>();
        
        for (String fileUrl : fileUrls) {
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                continue;
            }
            
            String fileKey = extractFileKeyFromUrl(fileUrl);
            if (fileKey == null || fileKey.isEmpty()) {
                log.warn("无法从URL提取fileKey，跳过：{}", fileUrl);
                continue;
            }
            
            TempFile tempFile = TempFile.builder()
                    .fileUrl(fileUrl)
                    .fileKey(fileKey)
                    .fileCategory(category)
                    .fileSubType(subType)
                    .storageProvider(storageProvider)
                    .createdAt(LocalDateTime.now())
                    .expireAt(LocalDateTime.now().plus(ttl))
                    .conversationId(conversationId)
                    .build();
            
            tempFiles.add(tempFile);
        }
        
        if (!tempFiles.isEmpty()) {
            tempFileRepository.batchSave(tempFiles, ttl);
        }
    }
    
    /**
     * 异步清理即将过期的临时文件
     * 由定时任务调用，根据存储提供商选择对应的Repository删除
     * 
     * @param batchSize 每批处理数量
     */
    public void cleanupExpiringSoonAsync(int batchSize) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始清理即将过期的临时文件，批次大小：{}", batchSize);
                
                // 查询即将过期的文件
                List<TempFile> expiringSoonFiles = tempFileRepository.findExpiringSoon(batchSize);
                
                if (expiringSoonFiles.isEmpty()) {
                    log.debug("没有即将过期的临时文件需要清理");
                    return;
                }
                
                log.info("查询到{}个即将过期的临时文件，开始清理", expiringSoonFiles.size());
                
                // 按存储提供商分组
                List<String> qiniuFileKeys = new ArrayList<>();
                List<String> minioFileKeys = new ArrayList<>();
                
                // 从系统配置获取当前默认存储提供商（用于处理storageProvider为null的孤儿文件）
                String currentStorageProviderCode = systemConfigService.getString("storage.provider", "qiniu");
                StorageProvider defaultProvider = StorageProvider.fromCode(currentStorageProviderCode);
                
                for (TempFile file : expiringSoonFiles) {
                    if (file.getFileKey() == null || file.getFileKey().isEmpty()) {
                        continue;
                    }
                    
                    // 获取存储提供商，如果为null则使用当前系统配置的默认提供商
                    StorageProvider provider = file.getStorageProvider();
                    if (provider == null) {
                        provider = defaultProvider;
                        log.warn("文件{}的storageProvider为null（可能是孤儿记录），使用当前系统配置的默认提供商：{}", 
                                file.getFileKey(), provider.getName());
                    }
                    
                    if (provider == StorageProvider.MINIO) {
                        minioFileKeys.add(file.getFileKey());
                    } else {
                        qiniuFileKeys.add(file.getFileKey());
                    }
                }
                
                // 分别删除不同存储的文件
                int totalDeleted = 0;
                int totalFailed = 0;
                
                if (!qiniuFileKeys.isEmpty()) {
                    int[] result = deleteFilesFromStorage(qiniuFileKeys, StorageProvider.QINIU);
                    totalDeleted += result[0];
                    totalFailed += result[1];
                }
                
                if (!minioFileKeys.isEmpty()) {
                    int[] result = deleteFilesFromStorage(minioFileKeys, StorageProvider.MINIO);
                    totalDeleted += result[0];
                    totalFailed += result[1];
                }
                
                log.info("临时文件清理完成，总数：{}，成功：{}，失败：{}", 
                    expiringSoonFiles.size(), totalDeleted, totalFailed);
                
            } catch (Exception e) {
                log.error("清理临时文件异常", e);
            }
        }, commonVirtualThreadExecutor);
    }
    
    /**
     * 从指定存储删除文件
     */
    private int[] deleteFilesFromStorage(List<String> fileKeys, StorageProvider provider) {
        int deleted = 0;
        int failed = 0;
        
        try {
            // 将TempFile.StorageProvider转换为domain.storage.enums.StorageProvider
            com.nexusvoice.domain.storage.enums.StorageProvider storageProvider;
            if (provider == StorageProvider.MINIO) {
                storageProvider = com.nexusvoice.domain.storage.enums.StorageProvider.MINIO;
            } else {
                storageProvider = com.nexusvoice.domain.storage.enums.StorageProvider.QINIU;
            }
            
            // 根据provider选择对应的StorageRepository
            StorageRepository repository = storageStrategyManager.getRepository(storageProvider);
            
            List<String> deletedKeys = repository.batchDelete(fileKeys);
            
            deleted = deletedKeys.size();
            failed = fileKeys.size() - deletedKeys.size();
            
            log.info("{}存储文件删除完成，总数：{}，成功：{}，失败：{}", 
                provider.getName(), fileKeys.size(), deleted, failed);
            
            // 删除Redis中的记录
            tempFileRepository.batchDelete(deletedKeys);
            
        } catch (Exception e) {
            log.error("{}存储文件批量删除失败", provider.getName(), e);
            failed = fileKeys.size();
        }
        
        return new int[]{deleted, failed};
    }
    
    /**
     * 立即异步删除临时文件（紧急清理场景）
     * 
     * @param fileUrl 文件URL
     */
    public void deleteImmediatelyAsync(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                String fileKey = extractFileKeyFromUrl(fileUrl);
                if (fileKey == null || fileKey.isEmpty()) {
                    log.warn("无法从URL提取fileKey，跳过删除：{}", fileUrl);
                    return;
                }
                
                // 查询文件信息以获取存储提供商
                tempFileRepository.findByFileKey(fileKey).ifPresentOrElse(tempFile -> {
                    // 删除存储中的文件（TODO: 根据provider选择对应的Repository）
                    boolean deleted = storageStrategyManager
                        .getDefaultRepository()
                        .delete(fileKey);
                    
                    if (deleted) {
                        // 删除Redis中的记录
                        tempFileRepository.delete(fileKey);
                        log.info("立即删除临时文件成功：{}", fileUrl);
                    } else {
                        log.warn("立即删除临时文件失败：{}", fileUrl);
                    }
                }, () -> {
                    // Redis中没有记录，直接尝试删除存储中的文件
                    boolean deleted = storageStrategyManager
                        .getDefaultRepository()
                        .delete(fileKey);
                    log.info("临时文件{}不在Redis中，直接删除存储，结果：{}", fileUrl, deleted);
                });
                
            } catch (Exception e) {
                log.error("立即删除临时文件异常，fileUrl: {}", fileUrl, e);
            }
        }, commonVirtualThreadExecutor);
    }
    
    /**
     * 获取临时文件统计信息
     * 
     * @return 临时文件总数
     */
    public long getTempFileCount() {
        return tempFileRepository.count();
    }
    
    /**
     * 按分类获取临时文件统计信息
     * 
     * @param category 文件分类
     * @return 该分类的文件数量
     */
    public long getTempFileCountByCategory(FileCategory category) {
        return tempFileRepository.countByCategory(category);
    }
    
    /**
     * 从CDN URL中提取文件Key
     */
    private String extractFileKeyFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            URI uri = URI.create(url.trim());
            String path = uri.getPath();
            
            // 移除开头的斜杠
            if (path != null && path.startsWith("/")) {
                path = path.substring(1);
            }
            
            return (path != null && !path.isEmpty()) ? path : null;
            
        } catch (Exception e) {
            log.error("从URL中提取fileKey失败：{}", url, e);
            return null;
        }
    }
}
