package com.nexusvoice.domain.rag.model.entity;

import com.nexusvoice.domain.common.BaseDomainEntity;
import com.nexusvoice.domain.rag.model.enums.FileType;
import com.nexusvoice.domain.rag.model.enums.ParseStrategy;
import com.nexusvoice.domain.rag.model.enums.ProcessStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 文件详情领域实体（聚合根）
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public class FileDetail extends BaseDomainEntity {
    
    /**
     * 上传用户ID
     */
    private Long userId;
    
    /**
     * 所属知识库ID（可选）
     */
    private Long knowledgeBaseId;
    
    /**
     * 存储文件名（UUID命名）
     */
    private String fileName;
    
    /**
     * 原始文件名
     */
    private String originalName;
    
    /**
     * 文件类型
     */
    private FileType fileType;
    
    /**
     * MIME类型
     */
    private String mimeType;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 存储提供商（QINIU/MINIO）
     */
    private String storageProvider;
    
    /**
     * 存储key
     */
    private String storageKey;
    
    /**
     * 文件存储路径
     */
    private String filePath;
    
    /**
     * 文件MD5哈希值
     */
    private String fileHash;
    
    /**
     * 文件页数
     */
    private Integer filePageSize;
    
    /**
     * 当前处理页数
     */
    private Integer currentProcessPage;
    
    /**
     * 处理进度（%）
     */
    private BigDecimal processProgress;
    
    /**
     * 处理状态
     */
    private ProcessStatus processStatus;
    
    /**
     * 错误码
     */
    private String errorCode;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 解析策略
     */
    private ParseStrategy parseStrategy;
    
    /**
     * 处理完成时间
     */
    private LocalDateTime processedAt;
    
    // 构造函数
    public FileDetail() {
        super();
        this.processStatus = ProcessStatus.PENDING;
        this.currentProcessPage = 0;
        this.processProgress = BigDecimal.ZERO;
    }
    
    public FileDetail(Long id) {
        super(id);
        this.processStatus = ProcessStatus.PENDING;
        this.currentProcessPage = 0;
        this.processProgress = BigDecimal.ZERO;
    }
    
    // 业务方法
    
    /**
     * 开始处理
     */
    public void startProcessing() {
        if (this.processStatus == ProcessStatus.PENDING) {
            this.processStatus = ProcessStatus.UPLOADING;
            this.currentProcessPage = 0;
        }
    }
    
    /**
     * 开始解析
     */
    public void startParsing() {
        if (this.processStatus.canTransitionTo(ProcessStatus.PARSING)) {
            this.processStatus = ProcessStatus.PARSING;
            // 自动选择解析策略
            if (this.parseStrategy == null && this.fileType != null) {
                this.parseStrategy = ParseStrategy.getDefaultStrategy(this.fileType);
            }
        }
    }
    
    /**
     * 开始分割
     */
    public void startSplitting() {
        if (this.processStatus.canTransitionTo(ProcessStatus.SPLITTING)) {
            this.processStatus = ProcessStatus.SPLITTING;
        }
    }
    
    /**
     * 开始向量化
     */
    public void startVectorizing() {
        if (this.processStatus.canTransitionTo(ProcessStatus.VECTORIZING)) {
            this.processStatus = ProcessStatus.VECTORIZING;
        }
    }
    
    /**
     * 更新处理进度
     */
    public void updateProgress(int processedPages) {
        this.currentProcessPage = processedPages;
        if (this.filePageSize != null && this.filePageSize > 0) {
            this.processProgress = BigDecimal.valueOf(processedPages)
                .divide(BigDecimal.valueOf(this.filePageSize), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
    }
    
    /**
     * 更新进度百分比
     */
    public void updateProgressPercentage(BigDecimal percentage) {
        if (percentage != null) {
            this.processProgress = percentage.setScale(2, RoundingMode.HALF_UP);
        }
    }
    
    /**
     * 标记处理完成
     */
    public void markCompleted() {
        if (this.processStatus.canTransitionTo(ProcessStatus.COMPLETED)) {
            this.processStatus = ProcessStatus.COMPLETED;
            this.processProgress = BigDecimal.valueOf(100);
            this.processedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 标记处理失败
     */
    public void markFailed(String errorCode, String errorMessage) {
        this.processStatus = ProcessStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 是否处理完成
     */
    public boolean isProcessCompleted() {
        return this.processStatus == ProcessStatus.COMPLETED;
    }
    
    /**
     * 是否处理失败
     */
    public boolean isProcessFailed() {
        return this.processStatus == ProcessStatus.FAILED;
    }
    
    /**
     * 是否正在处理
     */
    public boolean isProcessing() {
        return this.processStatus != null && this.processStatus.isProcessing();
    }
    
    /**
     * 是否需要OCR处理
     */
    public boolean needsOcr() {
        return this.fileType != null && this.fileType.needsOcr() 
            && this.parseStrategy != null && this.parseStrategy.requiresOcr();
    }
    
    /**
     * 能否重新处理
     */
    public boolean canReprocess() {
        return this.processStatus == ProcessStatus.FAILED && !isDeleted();
    }
    
    /**
     * 重置处理状态（重新处理时使用）
     */
    public void resetProcessStatus() {
        this.processStatus = ProcessStatus.PENDING;
        this.currentProcessPage = 0;
        this.processProgress = BigDecimal.ZERO;
        this.errorCode = null;
        this.errorMessage = null;
        this.processedAt = null;
    }
    
    // 领域规则验证
    
    /**
     * 验证文件大小是否合法（默认最大100MB）
     */
    public boolean isFileSizeValid(long maxSize) {
        return this.fileSize != null && this.fileSize <= maxSize;
    }
    
    /**
     * 验证文件类型是否支持
     */
    public boolean isFileTypeSupported() {
        return this.fileType != null;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }
    
    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getOriginalName() {
        return originalName;
    }
    
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
    
    public FileType getFileType() {
        return fileType;
    }
    
    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getStorageProvider() {
        return storageProvider;
    }
    
    public void setStorageProvider(String storageProvider) {
        this.storageProvider = storageProvider;
    }
    
    public String getStorageKey() {
        return storageKey;
    }
    
    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getFileHash() {
        return fileHash;
    }
    
    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }
    
    public Integer getFilePageSize() {
        return filePageSize;
    }
    
    public void setFilePageSize(Integer filePageSize) {
        this.filePageSize = filePageSize;
    }
    
    public Integer getCurrentProcessPage() {
        return currentProcessPage;
    }
    
    public void setCurrentProcessPage(Integer currentProcessPage) {
        this.currentProcessPage = currentProcessPage;
    }
    
    public BigDecimal getProcessProgress() {
        return processProgress;
    }
    
    public void setProcessProgress(BigDecimal processProgress) {
        this.processProgress = processProgress;
    }
    
    public ProcessStatus getProcessStatus() {
        return processStatus;
    }
    
    public void setProcessStatus(ProcessStatus processStatus) {
        this.processStatus = processStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public ParseStrategy getParseStrategy() {
        return parseStrategy;
    }
    
    public void setParseStrategy(ParseStrategy parseStrategy) {
        this.parseStrategy = parseStrategy;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
