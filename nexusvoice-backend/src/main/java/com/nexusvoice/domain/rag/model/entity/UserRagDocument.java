package com.nexusvoice.domain.rag.model.entity;

import com.nexusvoice.domain.common.BaseDomainEntity;

/**
 * 用户RAG文档快照领域实体
 * 保存用户安装的RAG中的文档内容
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public class UserRagDocument extends BaseDomainEntity {
    
    /**
     * 关联的用户RAG ID
     */
    private Long userRagId;
    
    /**
     * 关联的用户RAG文件ID
     */
    private Long userRagFileId;
    
    /**
     * 原始文档单元ID（仅标识用）
     */
    private Long originalDocumentId;
    
    /**
     * 文档内容
     */
    private String content;
    
    /**
     * 页码
     */
    private Integer page;
    
    /**
     * 向量ID（在pgvector中的ID）
     */
    private String vectorId;
    
    // 构造函数
    public UserRagDocument() {
        super();
    }
    
    // 业务方法
    
    /**
     * 创建用户文档快照
     */
    public void createSnapshot(Long userRagId, Long userRagFileId, Long originalDocumentId) {
        this.userRagId = userRagId;
        this.userRagFileId = userRagFileId;
        this.originalDocumentId = originalDocumentId;
        this.onCreate();
    }
    
    /**
     * 从版本文档复制
     */
    public void copyFromVersionDocument(RagVersionDocument versionDocument) {
        this.originalDocumentId = versionDocument.getOriginalDocumentId();
        this.content = versionDocument.getContent();
        this.page = versionDocument.getPage();
        this.vectorId = versionDocument.getVectorId();
    }
    
    /**
     * 设置文档内容
     */
    public void setDocumentContent(String content, Integer page) {
        this.content = content;
        this.page = page;
        this.onUpdate();
    }
    
    /**
     * 关联向量
     */
    public void linkVector(String vectorId) {
        this.vectorId = vectorId;
        this.onUpdate();
    }
    
    /**
     * 是否已向量化
     */
    public boolean isVectorized() {
        return vectorId != null && !vectorId.isEmpty();
    }
    
    // Getters and Setters
    
    public Long getUserRagId() {
        return userRagId;
    }
    
    public void setUserRagId(Long userRagId) {
        this.userRagId = userRagId;
    }
    
    public Long getUserRagFileId() {
        return userRagFileId;
    }
    
    public void setUserRagFileId(Long userRagFileId) {
        this.userRagFileId = userRagFileId;
    }
    
    public Long getOriginalDocumentId() {
        return originalDocumentId;
    }
    
    public void setOriginalDocumentId(Long originalDocumentId) {
        this.originalDocumentId = originalDocumentId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = page;
    }
    
    public String getVectorId() {
        return vectorId;
    }
    
    public void setVectorId(String vectorId) {
        this.vectorId = vectorId;
    }
}
