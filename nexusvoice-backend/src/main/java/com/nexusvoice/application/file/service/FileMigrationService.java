package com.nexusvoice.application.file.service;

import com.nexusvoice.domain.config.service.SystemConfigService;
import com.nexusvoice.domain.storage.enums.StorageProvider;
import com.nexusvoice.domain.storage.repository.StorageRepository;
import com.nexusvoice.infrastructure.repository.storage.StorageStrategyManager;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文件迁移服务
 * 支持在不同存储服务之间迁移文件
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
@Slf4j
@Service
public class FileMigrationService {
    
    @Autowired
    private StorageStrategyManager storageStrategyManager;
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    /**
     * 迁移任务执行器
     */
    private ExecutorService executorService;
    
    /**
     * 当前迁移任务
     */
    private volatile MigrationTask currentTask;
    
    /**
     * 迁移文件
     *
     * @param request 迁移请求
     * @return 迁移任务
     */
    public MigrationTask migrate(MigrationRequest request) {
        if (currentTask != null && !currentTask.isCompleted()) {
            throw new RuntimeException("已有迁移任务正在执行中");
        }
        
        // 验证存储服务可用性
        StorageRepository sourceRepo = storageStrategyManager.getRepository(request.getSourceProvider());
        StorageRepository targetRepo = storageStrategyManager.getRepository(request.getTargetProvider());
        
        if (!sourceRepo.isAvailable()) {
            throw new RuntimeException("源存储服务不可用：" + request.getSourceProvider());
        }
        
        if (!targetRepo.isAvailable()) {
            throw new RuntimeException("目标存储服务不可用：" + request.getTargetProvider());
        }
        
        // 创建迁移任务
        currentTask = MigrationTask.builder()
                .taskId(generateTaskId())
                .sourceProvider(request.getSourceProvider())
                .targetProvider(request.getTargetProvider())
                .fileKeys(request.getFileKeys())
                .totalCount(request.getFileKeys().size())
                .deleteSource(request.isDeleteSource())
                .startTime(System.currentTimeMillis())
                .status(MigrationStatus.RUNNING)
                .build();
        
        // 启动迁移
        startMigration(sourceRepo, targetRepo, currentTask);
        
        return currentTask;
    }
    
    /**
     * 获取当前迁移任务状态
     */
    public MigrationTask getCurrentTask() {
        return currentTask;
    }
    
    /**
     * 停止迁移任务
     */
    public void stopMigration() {
        if (currentTask != null && !currentTask.isCompleted()) {
            currentTask.setStatus(MigrationStatus.STOPPED);
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
            }
            log.info("迁移任务已停止，任务ID：{}", currentTask.getTaskId());
        }
    }
    
    /**
     * 启动迁移
     */
    private void startMigration(StorageRepository sourceRepo, StorageRepository targetRepo, MigrationTask task) {
        // 获取配置
        int batchSize = systemConfigService.getInt("storage.migration.batch_size", 100);
        int concurrent = systemConfigService.getInt("storage.migration.concurrent", 3);
        int retryTimes = systemConfigService.getInt("storage.migration.retry_times", 3);
        
        // 创建线程池
        executorService = Executors.newFixedThreadPool(concurrent);
        
        // 异步执行迁移
        CompletableFuture.runAsync(() -> {
            log.info("开始文件迁移，任务ID：{}，源：{}，目标：{}，文件数：{}", 
                    task.getTaskId(), task.getSourceProvider(), task.getTargetProvider(), task.getTotalCount());
            
            // 分批处理
            List<List<String>> batches = partition(task.getFileKeys(), batchSize);
            CountDownLatch latch = new CountDownLatch(batches.size());
            
            for (List<String> batch : batches) {
                executorService.submit(() -> {
                    try {
                        migrateBatch(sourceRepo, targetRepo, batch, task, retryTimes);
                    } catch (Exception e) {
                        log.error("批次迁移失败", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            try {
                // 等待所有批次完成
                latch.await();
                
                // 更新任务状态
                task.setEndTime(System.currentTimeMillis());
                task.setDuration(task.getEndTime() - task.getStartTime());
                
                if (task.getStatus() != MigrationStatus.STOPPED) {
                    if (task.getFailedCount() == 0) {
                        task.setStatus(MigrationStatus.COMPLETED);
                    } else {
                        task.setStatus(MigrationStatus.PARTIAL);
                    }
                }
                
                log.info("文件迁移完成，任务ID：{}，成功：{}，失败：{}，跳过：{}，耗时：{}ms", 
                        task.getTaskId(), task.getSuccessCount(), task.getFailedCount(), 
                        task.getSkippedCount(), task.getDuration());
                
            } catch (InterruptedException e) {
                log.error("迁移任务被中断", e);
                task.setStatus(MigrationStatus.FAILED);
                Thread.currentThread().interrupt();
            } finally {
                executorService.shutdown();
            }
        });
    }
    
    /**
     * 迁移批次文件
     */
    private void migrateBatch(StorageRepository sourceRepo, StorageRepository targetRepo, 
                              List<String> fileKeys, MigrationTask task, int retryTimes) {
        for (String fileKey : fileKeys) {
            if (task.getStatus() == MigrationStatus.STOPPED) {
                break;
            }
            
            boolean success = false;
            String error = null;
            
            // 重试机制
            for (int i = 0; i <= retryTimes; i++) {
                try {
                    // 检查目标是否已存在
                    if (targetRepo.exists(fileKey)) {
                        log.debug("文件已存在于目标存储，跳过：{}", fileKey);
                        task.incrementSkipped();
                        success = true;
                        break;
                    }
                    
                    // 下载文件
                    InputStream inputStream = sourceRepo.download(fileKey);
                    if (inputStream == null) {
                        throw new RuntimeException("下载文件失败：" + fileKey);
                    }
                    
                    // 获取文件信息
                    Map<String, Object> fileInfo = sourceRepo.getFileInfo(fileKey);
                    long fileSize = fileInfo != null && fileInfo.get("size") != null ? 
                            ((Number) fileInfo.get("size")).longValue() : -1;
                    
                    // 上传到目标存储
                    String fileName = extractFileName(fileKey);
                    targetRepo.upload(inputStream, fileName, fileSize, null);
                    
                    // 删除源文件（如果配置）
                    if (task.isDeleteSource()) {
                        sourceRepo.delete(fileKey);
                    }
                    
                    task.incrementSuccess();
                    task.addTransferredSize(fileSize);
                    success = true;
                    
                    log.debug("文件迁移成功：{}", fileKey);
                    break;
                    
                } catch (Exception e) {
                    error = e.getMessage();
                    if (i < retryTimes) {
                        log.warn("文件迁移失败，重试 {}/{}，文件：{}，错误：{}", 
                                i + 1, retryTimes, fileKey, error);
                        try {
                            Thread.sleep(1000 * (i + 1)); // 递增延迟
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            if (!success) {
                task.incrementFailed();
                task.addFailedFile(fileKey, error);
                log.error("文件迁移失败：{}，错误：{}", fileKey, error);
            }
        }
    }
    
    /**
     * 分割列表
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
    
    /**
     * 提取文件名
     */
    private String extractFileName(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return "unknown";
        }
        int lastSlash = fileKey.lastIndexOf('/');
        return lastSlash >= 0 ? fileKey.substring(lastSlash + 1) : fileKey;
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "MIGRATE_" + System.currentTimeMillis() + "_" + 
                ThreadLocalRandom.current().nextInt(1000, 9999);
    }
    
    /**
     * 迁移请求
     */
    @Data
    @Builder
    public static class MigrationRequest {
        private StorageProvider sourceProvider;
        private StorageProvider targetProvider;
        private List<String> fileKeys;
        private boolean deleteSource;
    }
    
    /**
     * 迁移任务
     */
    @Data
    @Builder
    public static class MigrationTask {
        private String taskId;
        private StorageProvider sourceProvider;
        private StorageProvider targetProvider;
        private List<String> fileKeys;
        private int totalCount;
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failedCount = new AtomicInteger(0);
        private final AtomicInteger skippedCount = new AtomicInteger(0);
        private final AtomicLong transferredSize = new AtomicLong(0);
        private final List<String> failedFiles = new CopyOnWriteArrayList<>();
        private final Map<String, String> failedReasons = new ConcurrentHashMap<>();
        private boolean deleteSource;
        private MigrationStatus status;
        private long startTime;
        private long endTime;
        private long duration;
        
        public void incrementSuccess() {
            successCount.incrementAndGet();
        }
        
        public void incrementFailed() {
            failedCount.incrementAndGet();
        }
        
        public void incrementSkipped() {
            skippedCount.incrementAndGet();
        }
        
        public void addTransferredSize(long size) {
            if (size > 0) {
                transferredSize.addAndGet(size);
            }
        }
        
        public void addFailedFile(String fileKey, String reason) {
            failedFiles.add(fileKey);
            if (reason != null) {
                failedReasons.put(fileKey, reason);
            }
        }
        
        public boolean isCompleted() {
            return status == MigrationStatus.COMPLETED || 
                   status == MigrationStatus.FAILED || 
                   status == MigrationStatus.STOPPED ||
                   status == MigrationStatus.PARTIAL;
        }
        
        public int getSuccessCount() {
            return successCount.get();
        }
        
        public int getFailedCount() {
            return failedCount.get();
        }
        
        public int getSkippedCount() {
            return skippedCount.get();
        }
        
        public long getTransferredSize() {
            return transferredSize.get();
        }
        
        public double getProgress() {
            if (totalCount == 0) {
                return 0;
            }
            int processed = successCount.get() + failedCount.get() + skippedCount.get();
            return (double) processed / totalCount * 100;
        }
    }
    
    /**
     * 迁移状态
     */
    public enum MigrationStatus {
        PENDING("待执行"),
        RUNNING("执行中"),
        COMPLETED("已完成"),
        PARTIAL("部分完成"),
        FAILED("失败"),
        STOPPED("已停止");
        
        private final String description;
        
        MigrationStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
