package com.nexusvoice.infrastructure.scheduler;

import com.nexusvoice.application.file.service.TempFileCleanupService;
import com.nexusvoice.domain.file.model.TempFile.FileCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 临时文件清理定时任务
 * 定期清理即将过期的临时文件（TTS音频、图片、PDF等）
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "nexusvoice.temp-file", name = "cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class TempFileCleanupScheduler {
    
    private final TempFileCleanupService tempFileCleanupService;
    
    /**
     * 每批处理的文件数量
     */
    private static final int BATCH_SIZE = 100;
    
    public TempFileCleanupScheduler(TempFileCleanupService tempFileCleanupService) {
        this.tempFileCleanupService = tempFileCleanupService;
    }
    
    /**
     * 定时清理即将过期的临时文件
     * 每30分钟执行一次
     */
//    @Scheduled(fixedRate = 1800000, initialDelay = 60000) // 30分钟，启动后1分钟开始
//    /**
//     * 定时清理即将过期的临时文件
//     * 每30秒执行一次（测试用，生产环境请注释掉）
//     */
    @Scheduled(fixedRate = 30000, initialDelay = 60000) // 30秒，启动后1分钟开始
    public void cleanupExpiringSoon() {
        try {
            log.debug("执行临时文件清理定时任务");
            tempFileCleanupService.cleanupExpiringSoonAsync(BATCH_SIZE);
        } catch (Exception e) {
            log.error("临时文件清理定时任务执行异常", e);
        }
    }
    
    /**
     * 定时记录临时文件统计信息
     * 每6小时执行一次
     */
    @Scheduled(fixedRate = 21600000, initialDelay = 300000) // 6小时，启动后5分钟开始
    public void logStatistics() {
        try {
            long totalCount = tempFileCleanupService.getTempFileCount();
            long audioCount = tempFileCleanupService.getTempFileCountByCategory(FileCategory.AUDIO);
            long imageCount = tempFileCleanupService.getTempFileCountByCategory(FileCategory.IMAGE);
            long pdfCount = tempFileCleanupService.getTempFileCountByCategory(FileCategory.PDF);
            long videoCount = tempFileCleanupService.getTempFileCountByCategory(FileCategory.VIDEO);
            long documentCount = tempFileCleanupService.getTempFileCountByCategory(FileCategory.DOCUMENT);
            
            log.info("临时文件统计：总数={}，音频={}，图片={}，PDF={}，视频={}，文档={}", 
                totalCount, audioCount, imageCount, pdfCount, videoCount, documentCount);
        } catch (Exception e) {
            log.error("临时文件统计异常", e);
        }
    }
}
