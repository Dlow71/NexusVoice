package com.nexusvoice.domain.rag.model.vo;

import com.nexusvoice.domain.rag.model.entity.DocumentUnit;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 处理后的段落值对象
 * 
 * @author NexusVoice
 * @since 2025-11-11
 */
@Getter
public class ProcessedSegment {
    
    /**
     * 段落类型枚举
     */
    public enum SegmentType {
        TEXT,       // 普通文本
        CODE,       // 代码块
        TABLE       // 表格
    }
    
    private final String content;
    private final SegmentType type;
    @Setter
    private Integer order;
    private final String titleContext;  // 标题上下文
    private final MarkdownNode.NodeType nodeType;  // Markdown节点类型
    private final Integer headingLevel;  // 标题层级
    private final String language;       // 代码语言
    private final Map<String, Object> metadata;  // 元数据
    
    /**
     * 构造函数
     */
    private ProcessedSegment(Builder builder) {
        this.content = builder.content;
        this.type = builder.type;
        this.order = builder.order;
        this.titleContext = builder.titleContext;
        this.nodeType = builder.nodeType;
        this.headingLevel = builder.headingLevel;
        this.language = builder.language;
        this.metadata = builder.metadata;
    }
    
    /**
     * 创建Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder类
     */
    public static class Builder {
        private String content;
        private SegmentType type = SegmentType.TEXT;
        private Integer order;
        private String titleContext;
        private MarkdownNode.NodeType nodeType;
        private Integer headingLevel;
        private String language;
        private Map<String, Object> metadata;
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder type(SegmentType type) {
            this.type = type;
            return this;
        }
        
        public Builder order(Integer order) {
            this.order = order;
            return this;
        }
        
        public Builder titleContext(String titleContext) {
            this.titleContext = titleContext;
            return this;
        }
        
        public Builder nodeType(MarkdownNode.NodeType nodeType) {
            this.nodeType = nodeType;
            return this;
        }
        
        public Builder headingLevel(Integer headingLevel) {
            this.headingLevel = headingLevel;
            return this;
        }
        
        public Builder language(String language) {
            this.language = language;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public ProcessedSegment build() {
            return new ProcessedSegment(this);
        }
    }
    
    /**
     * 转换为DocumentUnit实体
     */
    public DocumentUnit toDocumentUnit(Long fileId, Integer page) {
        DocumentUnit unit = new DocumentUnit();
        unit.setFileId(fileId);
        unit.setPage(page);
        unit.setContent(content);
        unit.setUnitType(type.name());
        unit.setIsOcr(false);
        unit.setIsVector(false);
        unit.setParagraphIndex(order);
        unit.setCharCount(content != null ? content.length() : 0);
        return unit;
    }
    
    /**
     * 获取内容长度
     */
    public int length() {
        return content != null ? content.length() : 0;
    }
}
