package com.nexusvoice.domain.rag.service;

import com.nexusvoice.domain.rag.model.entity.DocumentUnit;

import java.util.List;
import java.util.Map;

/**
 * 向量搜索领域服务接口
 * 负责基于向量的文档检索
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface VectorSearchService {
    
    /**
     * 搜索相似文档
     * @param query 查询文本
     * @param limit 返回数量限制
     * @return 搜索结果列表
     */
    List<SearchResult> search(String query, int limit);
    
    /**
     * 在指定知识库中搜索
     * @param query 查询文本
     * @param knowledgeBaseId 知识库ID
     * @param limit 返回数量限制
     * @return 搜索结果列表
     */
    List<SearchResult> searchInKnowledgeBase(String query, Long knowledgeBaseId, int limit);
    
    /**
     * 在指定文件中搜索
     * @param query 查询文本
     * @param fileId 文件ID
     * @param limit 返回数量限制
     * @return 搜索结果列表
     */
    List<SearchResult> searchInFile(String query, Long fileId, int limit);
    
    /**
     * 高级搜索
     * @param query 查询文本
     * @param filters 过滤条件
     * @param limit 返回数量限制
     * @return 搜索结果列表
     */
    List<SearchResult> advancedSearch(String query, SearchFilters filters, int limit);
    
    /**
     * 混合搜索（向量搜索+关键词搜索）
     * @param query 查询文本
     * @param keywords 关键词列表
     * @param limit 返回数量限制
     * @return 搜索结果列表
     */
    List<SearchResult> hybridSearch(String query, List<String> keywords, int limit);
    
    /**
     * 重排序搜索结果
     * @param query 查询文本
     * @param results 初始搜索结果
     * @param topK 返回前K个结果
     * @return 重排序后的结果
     */
    List<SearchResult> rerank(String query, List<SearchResult> results, int topK);
    
    /**
     * 搜索结果
     */
    class SearchResult {
        private Long documentUnitId;
        private DocumentUnit documentUnit;
        private Double score;
        private Double distance;
        private String highlight;
        private Map<String, Object> metadata;
        
        // Getters and Setters
        public Long getDocumentUnitId() {
            return documentUnitId;
        }
        
        public void setDocumentUnitId(Long documentUnitId) {
            this.documentUnitId = documentUnitId;
        }
        
        public DocumentUnit getDocumentUnit() {
            return documentUnit;
        }
        
        public void setDocumentUnit(DocumentUnit documentUnit) {
            this.documentUnit = documentUnit;
        }
        
        public Double getScore() {
            return score;
        }
        
        public void setScore(Double score) {
            this.score = score;
        }
        
        public Double getDistance() {
            return distance;
        }
        
        public void setDistance(Double distance) {
            this.distance = distance;
        }
        
        public String getHighlight() {
            return highlight;
        }
        
        public void setHighlight(String highlight) {
            this.highlight = highlight;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
    
    /**
     * 搜索过滤器
     */
    class SearchFilters {
        private Long userId;
        private Long knowledgeBaseId;
        private List<Long> fileIds;
        private List<String> fileTypes;
        private String language;
        private Double minScore;
        private Map<String, Object> metadataFilters;
        
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
        
        public List<Long> getFileIds() {
            return fileIds;
        }
        
        public void setFileIds(List<Long> fileIds) {
            this.fileIds = fileIds;
        }
        
        public List<String> getFileTypes() {
            return fileTypes;
        }
        
        public void setFileTypes(List<String> fileTypes) {
            this.fileTypes = fileTypes;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public void setLanguage(String language) {
            this.language = language;
        }
        
        public Double getMinScore() {
            return minScore;
        }
        
        public void setMinScore(Double minScore) {
            this.minScore = minScore;
        }
        
        public Map<String, Object> getMetadataFilters() {
            return metadataFilters;
        }
        
        public void setMetadataFilters(Map<String, Object> metadataFilters) {
            this.metadataFilters = metadataFilters;
        }
    }
}
