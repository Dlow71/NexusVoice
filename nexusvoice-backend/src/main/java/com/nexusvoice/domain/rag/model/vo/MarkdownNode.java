package com.nexusvoice.domain.rag.model.vo;

import lombok.Getter;
import java.util.Map;

/**
 * Markdown节点值对象
 * 
 * @author NexusVoice
 * @since 2025-11-11
 */
@Getter
public class MarkdownNode {
    
    /**
     * 节点类型枚举
     */
    public enum NodeType {
        HEADING,        // 标题
        PARAGRAPH,      // 段落
        CODE_BLOCK,     // 代码块
        TABLE,          // 表格
        IMAGE,          // 图片
        LIST,           // 列表
        BLOCKQUOTE      // 引用
    }
    
    private final NodeType type;
    private final int level;                    // 标题层级（1-6），其他类型为0
    private final String content;               // 节点内容（原始Markdown）
    private String translatedContent;           // 翻译后的内容（用于特殊节点增强）
    private final String language;              // 代码语言（仅代码块）
    private final Map<String, String> attributes;
    private Map<String, Object> metadata;       // 节点元数据
    
    /**
     * 构造函数
     */
    public MarkdownNode(NodeType type, int level, String content, 
                        String language, Map<String, String> attributes) {
        this.type = type;
        this.level = level;
        this.content = content;
        this.language = language;
        this.attributes = attributes;
    }
    
    /**
     * 设置翻译后的内容
     */
    public void setTranslatedContent(String translatedContent) {
        this.translatedContent = translatedContent;
    }
    
    /**
     * 设置元数据
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * 是否为标题
     */
    public boolean isHeading() {
        return type == NodeType.HEADING;
    }
    
    /**
     * 是否为一级标题
     */
    public boolean isH1() {
        return isHeading() && level == 1;
    }
    
    /**
     * 是否为代码块
     */
    public boolean isCodeBlock() {
        return type == NodeType.CODE_BLOCK;
    }
    
    /**
     * 是否为表格
     */
    public boolean isTable() {
        return type == NodeType.TABLE;
    }
    
    /**
     * 是否为图片
     */
    public boolean isImage() {
        return type == NodeType.IMAGE;
    }
    
    /**
     * 是否需要翻译增强
     */
    public boolean needsTranslation() {
        return isCodeBlock() || isTable() || isImage();
    }
    
    /**
     * 获取内容长度
     */
    public int length() {
        return content != null ? content.length() : 0;
    }
}
