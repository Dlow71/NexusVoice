package com.nexusvoice.domain.rag.model.entity;

import com.nexusvoice.domain.common.BaseDomainEntity;
import com.nexusvoice.domain.rag.model.enums.KnowledgeBaseStatus;

/**
 * 知识库领域实体
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public class KnowledgeBase extends BaseDomainEntity {
    
    /**
     * 所属用户ID
     */
    private Long userId;
    
    /**
     * 知识库名称
     */
    private String name;
    
    /**
     * 知识库描述
     */
    private String description;
    
    /**
     * 知识库图标URL
     */
    private String icon;
    
    /**
     * 标签（JSON字符串格式）
     */
    private String labels;
    
    /**
     * 状态
     */
    private KnowledgeBaseStatus status;
    
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
    
    // 构造函数
    public KnowledgeBase() {
        super();
        this.status = KnowledgeBaseStatus.ACTIVE;
        this.fileCount = 0;
        this.totalSize = 0L;
        this.documentCount = 0;
    }
    
    public KnowledgeBase(Long id) {
        super(id);
        this.status = KnowledgeBaseStatus.ACTIVE;
        this.fileCount = 0;
        this.totalSize = 0L;
        this.documentCount = 0;
    }
    
    // 业务方法
    
    /**
     * 增加文件
     */
    public void addFile(Long fileSize) {
        this.fileCount = this.fileCount + 1;
        this.totalSize = this.totalSize + fileSize;
    }
    
    /**
     * 移除文件
     */
    public void removeFile(Long fileSize) {
        if (this.fileCount > 0) {
            this.fileCount = this.fileCount - 1;
        }
        if (this.totalSize >= fileSize) {
            this.totalSize = this.totalSize - fileSize;
        } else {
            this.totalSize = 0L;
        }
    }
    
    /**
     * 归档知识库
     */
    public void archive() {
        if (this.status == KnowledgeBaseStatus.ACTIVE) {
            this.status = KnowledgeBaseStatus.ARCHIVED;
        }
    }
    
    /**
     * 激活知识库
     */
    public void activate() {
        if (this.status == KnowledgeBaseStatus.ARCHIVED) {
            this.status = KnowledgeBaseStatus.ACTIVE;
        }
    }
    
    /**
     * 设置为处理中
     */
    public void setProcessing() {
        this.status = KnowledgeBaseStatus.PROCESSING;
    }
    
    /**
     * 是否可用
     */
    public boolean isAvailable() {
        return this.status != null && this.status.isAvailable();
    }
    
    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return this.fileCount == null || this.fileCount == 0;
    }
    
    // 领域规则验证
    
    /**
     * 验证是否可以添加文件
     */
    public boolean canAddFile() {
        return this.status == KnowledgeBaseStatus.ACTIVE && !isDeleted();
    }
    
    /**
     * 验证是否可以删除
     */
    public boolean canDelete() {
        return isEmpty() && !isDeleted();
    }
    
    // Getters and Setters
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getLabels() {
        return labels;
    }
    
    public void setLabels(String labels) {
        this.labels = labels;
    }
    
    public KnowledgeBaseStatus getStatus() {
        return status;
    }
    
    public void setStatus(KnowledgeBaseStatus status) {
        this.status = status;
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
}
