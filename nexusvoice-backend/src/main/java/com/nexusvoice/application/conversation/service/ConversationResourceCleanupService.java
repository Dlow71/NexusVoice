package com.nexusvoice.application.conversation.service;

import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.model.MessageAttachment;
import com.nexusvoice.domain.conversation.repository.ConversationMessageRepository;
import com.nexusvoice.infrastructure.repository.storage.StorageStrategyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * 对话资源清理服务
 * 负责异步清理对话删除时产生的资源（图片、音频等CDN文件）
 * 使用虚拟线程提高并发性能
 *
 * @author NexusVoice Team
 * @since 2025-10-26
 */
@Slf4j
@Service
public class ConversationResourceCleanupService {

    private final ConversationMessageRepository messageRepository;
    private final StorageStrategyManager storageStrategyManager;

    public ConversationResourceCleanupService(ConversationMessageRepository messageRepository,
                                             StorageStrategyManager storageStrategyManager) {
        this.messageRepository = messageRepository;
        this.storageStrategyManager = storageStrategyManager;
    }

    /**
     * 异步清理对话的所有资源（推荐使用此重载方法，避免竞态条件）
     * 使用虚拟线程执行，不阻塞主线程
     *
     * @param conversationId 对话ID
     * @param messages 消息列表（在删除之前已查询好，避免异步查询时数据已被删除）
     */
    public void cleanupConversationResourcesAsync(Long conversationId, List<ConversationMessage> messages) {
        // 使用虚拟线程异步执行清理任务
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始清理对话资源，对话ID：{}，消息数量：{}", conversationId, 
                        messages != null ? messages.size() : 0);
                
                // 1. 验证消息列表
                if (messages == null || messages.isEmpty()) {
                    log.info("对话没有消息，无需清理资源，对话ID：{}", conversationId);
                    return;
                }
                
                // 2. 收集所有需要删除的文件URL
                Set<String> urlsToDelete = new HashSet<>();
                for (ConversationMessage message : messages) {
                    // 收集音频URL
                    if (message.getAudioUrl() != null && !message.getAudioUrl().trim().isEmpty()) {
                        urlsToDelete.add(message.getAudioUrl().trim());
                    }
                    
                    // 收集附件URL（图片、文档等）
                    if (message.hasAttachments()) {
                        for (MessageAttachment attachment : message.getAttachments()) {
                            if (attachment.getUrl() != null && !attachment.getUrl().trim().isEmpty()) {
                                urlsToDelete.add(attachment.getUrl().trim());
                            }
                            // 如果有缩略图，也要删除
                            if (attachment.getThumbnailUrl() != null && !attachment.getThumbnailUrl().trim().isEmpty()) {
                                urlsToDelete.add(attachment.getThumbnailUrl().trim());
                            }
                        }
                    }
                }
                
                if (urlsToDelete.isEmpty()) {
                    log.info("对话没有需要清理的资源文件，对话ID：{}", conversationId);
                    return;
                }
                
                log.info("对话共有{}个资源文件需要清理，对话ID：{}", urlsToDelete.size(), conversationId);
                
                // 3. 从URL提取fileKey并删除文件
                List<String> fileKeys = new ArrayList<>();
                for (String url : urlsToDelete) {
                    String fileKey = extractFileKeyFromUrl(url);
                    if (fileKey != null && !fileKey.isEmpty()) {
                        fileKeys.add(fileKey);
                    } else {
                        log.warn("无法从URL中提取fileKey，跳过删除：{}", url);
                    }
                }
                
                if (fileKeys.isEmpty()) {
                    log.warn("所有URL都无法提取fileKey，对话ID：{}", conversationId);
                    return;
                }
                
                // 4. 批量删除文件
                try {
                    List<String> deletedKeys = storageStrategyManager.getDefaultRepository().batchDelete(fileKeys);
                    log.info("对话资源清理完成，对话ID：{}，成功删除{}个文件，失败{}个", 
                            conversationId, deletedKeys.size(), fileKeys.size() - deletedKeys.size());
                    
                    if (deletedKeys.size() < fileKeys.size()) {
                        log.warn("部分文件删除失败，对话ID：{}，总数：{}，成功：{}，失败：{}", 
                                conversationId, fileKeys.size(), deletedKeys.size(), fileKeys.size() - deletedKeys.size());
                    }
                } catch (Exception e) {
                    log.error("批量删除文件失败，对话ID：{}，错误：{}", conversationId, e.getMessage(), e);
                }
                
            } catch (Exception e) {
                log.error("清理对话资源异常，对话ID：{}，错误：{}", conversationId, e.getMessage(), e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor()); // 使用虚拟线程执行器
    }

    /**
     * 异步清理对话的所有资源（旧方法，保留用于向后兼容）
     * ⚠️ 注意：此方法存在竞态条件风险，如果messages已被删除则无法清理资源
     * 建议使用 cleanupConversationResourcesAsync(Long, List<ConversationMessage>) 重载方法
     * 
     * @param conversationId 对话ID
     * @deprecated 建议使用传入消息列表的重载方法，避免竞态条件
     */
    @Deprecated
    public void cleanupConversationResourcesAsync(Long conversationId) {
        // 查询消息列表
        List<ConversationMessage> messages = messageRepository.findByConversationId(conversationId);
        // 调用新方法
        cleanupConversationResourcesAsync(conversationId, messages);
    }

    /**
     * 从CDN URL中提取文件Key
     * 支持七牛云和MinIO的URL格式
     *
     * @param url CDN URL
     * @return 文件Key（存储路径），如果提取失败返回null
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
            
            // 如果路径为空，返回null
            if (path == null || path.isEmpty()) {
                log.warn("URL路径为空，无法提取fileKey：{}", url);
                return null;
            }
            
            log.debug("从URL中提取fileKey成功：{} -> {}", url, path);
            return path;
            
        } catch (Exception e) {
            log.error("从URL中提取fileKey失败：{}，错误：{}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 同步清理单个文件（用于特殊场景）
     *
     * @param fileUrl 文件URL
     * @return 是否删除成功
     */
    public boolean cleanupSingleFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return false;
        }
        
        try {
            String fileKey = extractFileKeyFromUrl(fileUrl);
            if (fileKey == null || fileKey.isEmpty()) {
                log.warn("无法从URL中提取fileKey，跳过删除：{}", fileUrl);
                return false;
            }
            
            boolean deleted = storageStrategyManager.getDefaultRepository().delete(fileKey);
            if (deleted) {
                log.info("文件删除成功：{}", fileUrl);
            } else {
                log.warn("文件删除失败：{}", fileUrl);
            }
            return deleted;
            
        } catch (Exception e) {
            log.error("删除文件失败：{}，错误：{}", fileUrl, e.getMessage(), e);
            return false;
        }
    }
}
