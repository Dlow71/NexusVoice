package com.nexusvoice.domain.rag.model.entity;

import com.nexusvoice.domain.common.BaseDomainEntity;

/**
 * RAG版本文档快照领域实体
 * 保存版本快照中的文档内容
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public class RagVersionDocument extends BaseDomainEntity {
    
    /**
     * 关联的版本ID
     */
    private Long ragVersionId;
    
    /**
     * 关联的版本文件ID
     */
    private Long ragVersionFileId;
    
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
    public RagVersionDocument() {
        super();
    }
    
    // 业务方法
    
    /**
     * 创建文档快照
     */
    public void createSnapshot(Long ragVersionId, Long ragVersionFileId, Long originalDocumentId) {
        this.ragVersionId = ragVersionId;
        this.ragVersionFileId = ragVersionFileId;
        this.originalDocumentId = originalDocumentId;
        this.onCreate();
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
    
    public Long getRagVersionId() {
        return ragVersionId;
    }
    
    public void setRagVersionId(Long ragVersionId) {
        this.ragVersionId = ragVersionId;
    }
    
    public Long getRagVersionFileId() {
        return ragVersionFileId;
    }
    
    public void setRagVersionFileId(Long ragVersionFileId) {
        this.ragVersionFileId = ragVersionFileId;
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
