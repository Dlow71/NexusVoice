package com.nexusvoice.domain.rag.model.entity;

import com.nexusvoice.domain.common.BaseDomainEntity;
import java.time.LocalDateTime;

/**
 * RAG版本快照领域实体（聚合根）
 * 用于保存知识库的版本快照，支持版本管理
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public class RagVersion extends BaseDomainEntity {
    
    /**
     * 原始知识库ID
     */
    private Long knowledgeBaseId;
    
    /**
     * 创建者用户ID
     */
    private Long userId;
    
    /**
     * 快照时的名称
     */
    private String name;
    
    /**
     * 快照时的图标URL
     */
    private String icon;
    
    /**
     * 快照时的描述
     */
    private String description;
    
    /**
     * 版本号（语义化版本：1.0.0、1.1.0等）
     */
    private String version;
    
    /**
     * 更新日志
     */
    private String changeLog;
    
    /**
     * 标签（JSON字符串格式存储）
     */
    private String labels;
    
    /**
     * 文件数量
     */
    private Integer fileCount;
    
    /**
     * 总大小（字节）
     */
    private Long totalSize;
    
    /**
     * 文档单元数量
     */
    private Integer documentCount;
    
    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;
    
    // 构造函数
    public RagVersion() {
        super();
        this.fileCount = 0;
        this.totalSize = 0L;
        this.documentCount = 0;
    }
    
    // 业务方法
    
    /**
     * 创建版本快照
     */
    public void createSnapshot(Long knowledgeBaseId, Long userId, String name, String version) {
        this.knowledgeBaseId = knowledgeBaseId;
        this.userId = userId;
        this.name = name;
        this.version = version;
        this.onCreate();
    }
    
    /**
     * 发布版本
     */
    public void publish() {
        this.publishedAt = LocalDateTime.now();
        this.onUpdate();
    }
    
    /**
     * 更新统计信息
     */
    public void updateStatistics(int fileCount, long totalSize, int documentCount) {
        this.fileCount = fileCount;
        this.totalSize = totalSize;
        this.documentCount = documentCount;
        this.onUpdate();
    }
    
    /**
     * 是否已发布
     */
    public boolean isPublished() {
        return this.publishedAt != null;
    }
    
    /**
     * 验证版本号格式
     */
    public boolean isValidVersion() {
        if (version == null || version.isEmpty()) {
            return false;
        }
        return version.matches("\\d+\\.\\d+\\.\\d+");
    }
    
    // Getters and Setters
    
    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }
    
    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getChangeLog() {
        return changeLog;
    }
    
    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }
    
    public String getLabels() {
        return labels;
    }
    
    public void setLabels(String labels) {
        this.labels = labels;
    }
    
    public Integer getFileCount() {
        return fileCount;
    }
    
    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }
    
    public Long getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }
    
    public Integer getDocumentCount() {
        return documentCount;
    }
    
    public void setDocumentCount(Integer documentCount) {
        this.documentCount = documentCount;
    }
    
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
