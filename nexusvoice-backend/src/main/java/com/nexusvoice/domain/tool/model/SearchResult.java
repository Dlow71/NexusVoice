package com.nexusvoice.domain.tool.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索结果领域模型
 *
 * @author NexusVoice
 * @since 2025-09-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    
    /**
     * 搜索查询
     */
    private String query;
    
    /**
     * 搜索结果列表
     */
    private List<SearchItem> items;
    
    /**
     * 搜索耗时（毫秒）
     */
    private Long searchTimeMs;
    
    /**
     * 结果总数
     */
    private Integer totalCount;
    
    /**
     * 搜索来源
     */
    private String source;
    
    /**
     * 搜索结果项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchItem {
        /**
         * 标题
         */
        private String title;
        
        /**
         * 链接
         */
        private String link;
        
        /**
         * 摘要
         */
        private String snippet;
        
        /**
         * 发布时间（可选）
         */
        private String publishTime;
        
        /**
         * 相关度评分
         */
        private Double relevanceScore;
    }
}
