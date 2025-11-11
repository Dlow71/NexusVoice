package com.nexusvoice.domain.rag.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 检索结果值对象
 * 
 * @author NexusVoice
 * @since 2025-11-11
 */
@Getter
@Setter
public class SearchResult {
    
    /**
     * 检索类型枚举
     */
    public enum SearchType {
        VECTOR,     // 向量检索
        KEYWORD,    // 关键词检索
        HYBRID      // 混合检索
    }
    
    private final Long documentUnitId;
    private final String content;
    private Double score;
    private final SearchType searchType;
    private final Map<String, Object> metadata;
    private Double fusionScore;  // RRF融合后的分数
    
    /**
     * 构造函数
     */
    public SearchResult(Long documentUnitId, String content, 
                       Double score, SearchType searchType,
                       Map<String, Object> metadata) {
        this.documentUnitId = documentUnitId;
        this.content = content;
        this.score = score;
        this.searchType = searchType;
        this.metadata = metadata;
    }
    
    /**
     * 获取内容预览
     */
    public String getContentPreview(int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
    
    /**
     * 获取元数据值
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * 是否有融合分数
     */
    public boolean hasFusionScore() {
        return fusionScore != null;
    }
}
