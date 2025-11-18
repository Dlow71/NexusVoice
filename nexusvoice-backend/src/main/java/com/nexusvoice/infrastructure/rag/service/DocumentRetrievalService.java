package com.nexusvoice.infrastructure.rag.service;

import com.nexusvoice.domain.rag.model.entity.VectorStore;
import com.nexusvoice.domain.rag.repository.VectorStoreRepository;
import com.nexusvoice.domain.config.repository.SystemConfigRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.infrastructure.ai.model.EmbeddingRequest;
import com.nexusvoice.infrastructure.ai.model.EmbeddingResponse;
import com.nexusvoice.infrastructure.ai.model.RerankRequest;
import com.nexusvoice.infrastructure.ai.model.RerankResponse;
import com.nexusvoice.infrastructure.ai.service.AiEmbeddingService;
import com.nexusvoice.infrastructure.ai.service.AiRerankService;
import com.nexusvoice.infrastructure.persistence.mapper.VectorStorePOMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档检索服务
 * 实现混合检索（向量检索+关键词检索）和Rerank重排序
 * 
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentRetrievalService {
    
    private final VectorStoreRepository vectorStoreRepository;
    private final VectorStorePOMapper vectorStorePOMapper;
    private final DynamicAiModelBeanManager modelBeanManager;
    private final SystemConfigRepository systemConfigRepository;
    
    // 配置键
    private static final String CONFIG_KEY_EMBEDDING_MODEL = "rag.embedding.model";
    private static final String CONFIG_KEY_RERANK_MODEL = "rag.rerank.model";
    private static final String CONFIG_KEY_SEARCH_TOPK = "rag.search.topk";
    private static final String CONFIG_KEY_MIN_SIMILARITY = "rag.search.similarity_threshold";
    private static final String CONFIG_KEY_RRF_K = "rag.search.rrf_k";
    
    // 默认值
    private static final String DEFAULT_EMBEDDING_MODEL = "siliconflow:netease-youdao/bce-embedding-base_v1";
    private static final String DEFAULT_RERANK_MODEL = "siliconflow:BAAI/bge-reranker-v2-m3";
    private static final int DEFAULT_TOPK = 15;
    private static final double DEFAULT_MIN_SIMILARITY = 0.7;
    private static final int DEFAULT_RRF_K = 60;
    
    /**
     * 混合检索：向量检索 + 关键词检索
     * 
     * @param query 查询文本
     * @param knowledgeBaseId 知识库ID
     * @param topK 返回数量
     * @return 检索结果列表
     */
    public List<RetrievalResult> hybridSearch(String query, Long knowledgeBaseId, int topK) {
        if (query == null || query.isEmpty()) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "查询文本不能为空");
        }
        
        log.info("开始混合检索 - 知识库ID: {}, 查询: {}, topK: {}", knowledgeBaseId, query, topK);
        
        try {
            // 1. 向量检索
            List<RetrievalResult> vectorResults = vectorSearch(query, knowledgeBaseId, topK * 2);
            
            // 2. 关键词检索
            List<RetrievalResult> keywordResults = keywordSearch(query, knowledgeBaseId, topK * 2);
            
            // 3. 融合结果（RRF - Reciprocal Rank Fusion）
            List<RetrievalResult> fusedResults = fuseResults(vectorResults, keywordResults);
            
            // 4. Rerank重排序
            List<RetrievalResult> rerankedResults = rerank(query, fusedResults, topK);
            
            log.info("混合检索完成 - 返回结果数: {}", rerankedResults.size());
            
            return rerankedResults;
            
        } catch (Exception e) {
            log.error("混合检索失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, "检索失败: " + e.getMessage());
        }
    }
    
    /**
     * 向量检索
     */
    private List<RetrievalResult> vectorSearch(String query, Long knowledgeBaseId, int topK) {
        try {
            // 1. 获取Embedding模型
            String embeddingModel = getEmbeddingModel();
            AiEmbeddingService embeddingService = modelBeanManager.getEmbeddingServiceByModelKey(embeddingModel);
            
            // 2. 将query转换为向量
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .text(query)
                    .build();
            EmbeddingResponse response = embeddingService.embed(request);
            
            if (!Boolean.TRUE.equals(response.getSuccess())) {
                log.error("向量化失败: {}", response.getErrorMessage());
                return new ArrayList<>();
            }
            
            List<Float> queryEmbedding = response.getVector();
            
            // 3. 使用pgvector进行相似度搜索
            double minSimilarity = getMinSimilarity();
            String embeddingStr = vectorToString(queryEmbedding);
            
            List<Map<String, Object>> results = vectorStorePOMapper.searchSimilarWithThreshold(
                    embeddingStr,
                    knowledgeBaseId,
                    minSimilarity,
                    topK
            );
            
            // 4. 转换为RetrievalResult
            return results.stream()
                    .map(this::mapToRetrievalResult)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("向量检索失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 关键词检索（PostgreSQL全文搜索）
     */
    private List<RetrievalResult> keywordSearch(String query, Long knowledgeBaseId, int topK) {
        try {
            // 使用PostgreSQL全文检索
            List<Map<String, Object>> results = vectorStorePOMapper.keywordSearch(
                    query,
                    knowledgeBaseId,
                    topK
            );
            
            return results.stream()
                    .map(this::mapToRetrievalResult)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("关键词检索失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 融合检索结果（RRF算法 - Reciprocal Rank Fusion）
     * RRF Score = sum(1 / (k + rank_i))
     */
    private List<RetrievalResult> fuseResults(List<RetrievalResult> vectorResults, 
                                              List<RetrievalResult> keywordResults) {
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, RetrievalResult> resultMap = new HashMap<>();
        int K = getRrfK();
        
        // 向量检索打分
        for (int i = 0; i < vectorResults.size(); i++) {
            RetrievalResult result = vectorResults.get(i);
            double score = 1.0 / (K + i + 1);
            rrfScores.put(result.getDocumentUnitId(), score);
            resultMap.put(result.getDocumentUnitId(), result);
        }
        
        // 关键词检索打分（累加）
        for (int i = 0; i < keywordResults.size(); i++) {
            RetrievalResult result = keywordResults.get(i);
            double score = 1.0 / (K + i + 1);
            rrfScores.merge(result.getDocumentUnitId(), score, Double::sum);
            resultMap.putIfAbsent(result.getDocumentUnitId(), result);
        }
        
        // 按RRF分数排序
        return rrfScores.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .map(e -> {
                    RetrievalResult r = resultMap.get(e.getKey());
                    r.setScore(e.getValue());
                    return r;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Rerank重排序
     */
    private List<RetrievalResult> rerank(String query, List<RetrievalResult> results, int topK) {
        if (results.isEmpty()) {
            return results;
        }
        
        // 如果结果数量较少，直接返回
        if (results.size() <= 3) {
            return results.subList(0, Math.min(topK, results.size()));
        }
        
        try {
            // 1. 获取Rerank服务
            String rerankModel = getRerankModel();
            AiRerankService rerankService = modelBeanManager.getRerankServiceByModelKey(rerankModel);
            
            // 2. 构建请求
            List<String> documents = results.stream()
                    .map(RetrievalResult::getContent)
                    .collect(Collectors.toList());
            
            RerankRequest request = RerankRequest.builder()
                    .query(query)
                    .documents(documents)
                    .topN(topK)
                    .build();
            
            // 3. 调用Rerank
            RerankResponse response = rerankService.rerank(request);
            
            if (!Boolean.TRUE.equals(response.getSuccess())) {
                log.warn("Rerank失败，使用原始排序: {}", response.getErrorMessage());
                return results.subList(0, Math.min(topK, results.size()));
            }
            
            // 4. 根据Rerank结果重新排序
            List<RetrievalResult> rerankedResults = new ArrayList<>();
            for (RerankResponse.RerankResult rerankResult : response.getResults()) {
                if (rerankResult.getIndex() < results.size()) {
                    RetrievalResult result = results.get(rerankResult.getIndex());
                    result.setScore(rerankResult.getScore());
                    rerankedResults.add(result);
                }
            }
            
            log.debug("Rerank重排序完成 - 输入: {}, 输出: {}", results.size(), rerankedResults.size());
            return rerankedResults;
            
        } catch (Exception e) {
            log.error("Rerank重排序失败，降级使用原始排序", e);
            return results.subList(0, Math.min(topK, results.size()));
        }
    }
    
    /**
     * 检索结果实体
     */
    public static class RetrievalResult {
        private Long documentUnitId;
        private String content;
        private Double score;
        private String title;
        private Long fileId;
        
        // Getters and Setters
        public Long getDocumentUnitId() { return documentUnitId; }
        public void setDocumentUnitId(Long documentUnitId) { this.documentUnitId = documentUnitId; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
    }
    
    // ==================== 配置读取方法 ====================
    
    /**
     * 获取Embedding模型配置
     */
    private String getEmbeddingModel() {
        return systemConfigRepository.findByKey(CONFIG_KEY_EMBEDDING_MODEL)
                .map(config -> config.getConfigValue())
                .orElse(DEFAULT_EMBEDDING_MODEL);
    }
    
    /**
     * 获取Rerank模型配置
     */
    private String getRerankModel() {
        return systemConfigRepository.findByKey(CONFIG_KEY_RERANK_MODEL)
                .map(config -> config.getConfigValue())
                .orElse(DEFAULT_RERANK_MODEL);
    }
    
    /**
     * 获取最小相似度阈值配置
     */
    private double getMinSimilarity() {
        return systemConfigRepository.findByKey(CONFIG_KEY_MIN_SIMILARITY)
                .map(config -> {
                    try {
                        return Double.parseDouble(config.getConfigValue());
                    } catch (Exception e) {
                        return DEFAULT_MIN_SIMILARITY;
                    }
                })
                .orElse(DEFAULT_MIN_SIMILARITY);
    }
    
    /**
     * 获取RRF常数K配置
     */
    private int getRrfK() {
        return systemConfigRepository.findByKey(CONFIG_KEY_RRF_K)
                .map(config -> {
                    try {
                        return Integer.parseInt(config.getConfigValue());
                    } catch (Exception e) {
                        return DEFAULT_RRF_K;
                    }
                })
                .orElse(DEFAULT_RRF_K);
    }
    
    // ==================== 数据转换方法 ====================
    
    /**
     * 将向量转换为PostgreSQL vector格式的字符串
     * 格式：[0.1,0.2,0.3,...]
     */
    private String vectorToString(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * 将数据库结果Map转换为RetrievalResult
     */
    private RetrievalResult mapToRetrievalResult(Map<String, Object> map) {
        RetrievalResult result = new RetrievalResult();
        
        // documentUnitId或document_unit_id
        Object docUnitId = map.get("document_unit_id");
        if (docUnitId == null) {
            docUnitId = map.get("documentUnitId");
        }
        if (docUnitId != null) {
            result.setDocumentUnitId(Long.valueOf(docUnitId.toString()));
        }
        
        // content
        Object content = map.get("content");
        if (content != null) {
            result.setContent(content.toString());
        }
        
        // score或similarity_score
        Object score = map.get("similarity_score");
        if (score == null) {
            score = map.get("score");
        }
        if (score != null) {
            result.setScore(Double.valueOf(score.toString()));
        }
        
        // title或file_name
        Object title = map.get("file_name");
        if (title == null) {
            title = map.get("title");
        }
        if (title != null) {
            result.setTitle(title.toString());
        }
        
        // fileId或file_id
        Object fileId = map.get("file_id");
        if (fileId == null) {
            fileId = map.get("fileId");
        }
        if (fileId != null) {
            result.setFileId(Long.valueOf(fileId.toString()));
        }
        
        return result;
    }
}
