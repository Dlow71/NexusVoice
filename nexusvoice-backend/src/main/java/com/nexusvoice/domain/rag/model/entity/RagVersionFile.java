package com.nexusvoice.domain.rag.model.entity;

import com.nexusvoice.domain.common.BaseDomainEntity;

/**
 * RAG版本文件快照领域实体
 * 保存版本快照中的文件信息
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public class RagVersionFile extends BaseDomainEntity {
    
    /**
     * 关联的版本ID
     */
    private Long ragVersionId;
    
    /**
     * 原始文件ID（仅标识用）
     */
    private Long originalFileId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件页数
     */
    private Integer filePageSize;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 文件存储路径
     */
    private String filePath;
    
    /**
     * 处理状态
     */
    private Integer processStatus;
    
    // 构造函数
    public RagVersionFile() {
        super();
        this.fileSize = 0L;
        this.filePageSize = 0;
    }
    
    // 业务方法
    
    /**
     * 创建文件快照
     */
    public void createSnapshot(Long ragVersionId, Long originalFileId, String fileName) {
        this.ragVersionId = ragVersionId;
        this.originalFileId = originalFileId;
        this.fileName = fileName;
        this.onCreate();
    }
    
    /**
     * 更新文件信息
     */
    public void updateFileInfo(Long fileSize, Integer filePageSize, String fileType) {
        this.fileSize = fileSize;
        this.filePageSize = filePageSize;
        this.fileType = fileType;
        this.onUpdate();
    }
    
    /**
     * 是否处理完成
     */
    public boolean isProcessed() {
        return processStatus != null && processStatus == 4; // 4-COMPLETED
    }
    
    // Getters and Setters
    
    public Long getRagVersionId() {
        return ragVersionId;
    }
    
    public void setRagVersionId(Long ragVersionId) {
        this.ragVersionId = ragVersionId;
    }
    
    public Long getOriginalFileId() {
        return originalFileId;
    }
    
    public void setOriginalFileId(Long originalFileId) {
        this.originalFileId = originalFileId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public Integer getFilePageSize() {
        return filePageSize;
    }
    
    public void setFilePageSize(Integer filePageSize) {
        this.filePageSize = filePageSize;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Integer getProcessStatus() {
        return processStatus;
    }
    
    public void setProcessStatus(Integer processStatus) {
        this.processStatus = processStatus;
    }
}
