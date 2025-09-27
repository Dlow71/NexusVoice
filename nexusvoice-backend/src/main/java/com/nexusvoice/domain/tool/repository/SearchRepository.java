package com.nexusvoice.domain.tool.repository;

import com.nexusvoice.domain.tool.model.SearchResult;

/**
 * 搜索仓储接口
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
public interface SearchRepository {
    
    /**
     * 执行网页搜索
     * 
     * @param query 搜索查询
     * @param maxResults 最大结果数量
     * @param language 搜索语言
     * @return 搜索结果
     */
    SearchResult searchWeb(String query, Integer maxResults, String language);
    
    /**
     * 执行新闻搜索
     * 
     * @param query 搜索查询
     * @param maxResults 最大结果数量
     * @param language 搜索语言
     * @return 搜索结果
     */
    SearchResult searchNews(String query, Integer maxResults, String language);
}
