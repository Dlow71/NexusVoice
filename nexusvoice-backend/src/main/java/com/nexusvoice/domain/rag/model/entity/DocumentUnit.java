package com.nexusvoice.domain.rag.model.entity;

import com.nexusvoice.domain.common.BaseDomainEntity;
import java.math.BigDecimal;

/**
 * 文档单元领域实体
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public class DocumentUnit extends BaseDomainEntity {
    
    /**
     * 关联文件ID
     */
    private Long fileId;
    
    /**
     * 单元类型：TEXT-文本，TABLE-表格，IMAGE-图片描述
     */
    private String unitType;
    
    /**
     * 文本内容
     */
    private String content;
    
    /**
     * 页码（从1开始）
     */
    private Integer page;
    
    /**
     * 段落索引
     */
    private Integer paragraphIndex;
    
    /**
     * 分块索引（同一页可能多个块）
     */
    private Integer chunkIndex;
    
    /**
     * 在原文中的起始位置
     */
    private Integer startPosition;
    
    /**
     * 在原文中的结束位置
     */
    private Integer endPosition;
    
    /**
     * 字符数
     */
    private Integer charCount;
    
    /**
     * Token数量
     */
    private Integer tokenCount;
    
    /**
     * 是否OCR处理
     */
    private Boolean isOcr;
    
    /**
     * OCR识别置信度
     */
    private BigDecimal ocrConfidence;
    
    /**
     * 是否已向量化
     */
    private Boolean isVector;
    
    /**
     * 语言代码：zh/en/ja等
     */
    private String language;
    
    /**
     * 元数据：标题、作者、关键词等（JSON格式）
     */
    private String metadata;
    
    // 构造函数
    public DocumentUnit() {
        super();
        this.unitType = "TEXT";
        this.isOcr = false;
        this.isVector = false;
    }
    
    public DocumentUnit(Long id) {
        super(id);
        this.unitType = "TEXT";
        this.isOcr = false;
        this.isVector = false;
    }
    
    // 业务方法
    
    /**
     * 标记为已向量化
     */
    public void markVectorized() {
        this.isVector = true;
        this.onUpdate();
    }
    
    /**
     * 标记为OCR处理
     */
    public void markAsOcr() {
        this.isOcr = true;
        this.onUpdate();
    }
    
    /**
     * 是否需要向量化
     */
    public boolean needsVectorization() {
        return !Boolean.TRUE.equals(this.isVector) && this.content != null && !this.content.isEmpty();
    }
    
    /**
     * 是否需要OCR处理
     */
    public boolean needsOcr() {
        return !Boolean.TRUE.equals(this.isOcr);
    }
    
    /**
     * 获取内容预览（最多100个字符）
     */
    public String getContentPreview() {
        if (this.content == null || this.content.isEmpty()) {
            return "";
        }
        if (this.content.length() <= 100) {
            return this.content;
        }
        return this.content.substring(0, 100) + "...";
    }
    
    /**
     * 验证内容长度是否合法（默认最大100000字符）
     */
    public boolean isContentLengthValid() {
        return this.content == null || this.content.length() <= 100000;
    }
    
    // Getters and Setters
    
    public Long getFileId() {
        return fileId;
    }
    
    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }
    
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = page;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Boolean getIsVector() {
        return isVector;
    }
    
    public void setIsVector(Boolean isVector) {
        this.isVector = isVector;
    }
    
    public Boolean getIsOcr() {
        return isOcr;
    }
    
    public void setIsOcr(Boolean isOcr) {
        this.isOcr = isOcr;
    }
    
    public String getUnitType() {
        return unitType;
    }
    
    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }
    
    public Integer getParagraphIndex() {
        return paragraphIndex;
    }
    
    public void setParagraphIndex(Integer paragraphIndex) {
        this.paragraphIndex = paragraphIndex;
    }
    
    public Integer getChunkIndex() {
        return chunkIndex;
    }
    
    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }
    
    public Integer getStartPosition() {
        return startPosition;
    }
    
    public void setStartPosition(Integer startPosition) {
        this.startPosition = startPosition;
    }
    
    public Integer getEndPosition() {
        return endPosition;
    }
    
    public void setEndPosition(Integer endPosition) {
        this.endPosition = endPosition;
    }
    
    public Integer getCharCount() {
        return charCount;
    }
    
    public void setCharCount(Integer charCount) {
        this.charCount = charCount;
    }
    
    public Integer getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    public BigDecimal getOcrConfidence() {
        return ocrConfidence;
    }
    
    public void setOcrConfidence(BigDecimal ocrConfidence) {
        this.ocrConfidence = ocrConfidence;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
