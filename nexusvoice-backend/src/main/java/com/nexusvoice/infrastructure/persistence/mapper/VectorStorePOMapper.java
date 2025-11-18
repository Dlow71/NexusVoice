package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.VectorStorePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

/**
 * VectorStore持久化Mapper
 *
 * @author NexusVoice
 * @since 2025-10-23
 */
@Mapper
public interface VectorStorePOMapper extends BaseMapper<VectorStorePO> {
    
    /**
     * 相似向量搜索（使用pgvector的余弦相似度）
     * @param embedding 查询向量
     * @param limit 返回数量限制
     * @return 相似的向量列表
     */
    @Select("SELECT * FROM vector_store " +
            "WHERE deleted = 0 " +
            "ORDER BY embedding <=> #{embedding}::vector " +
            "LIMIT #{limit}")
    List<VectorStorePO> searchSimilar(@Param("embedding") String embedding, @Param("limit") int limit);

    /**
     * 在指定文件内搜索相似向量
     */
    @Select("SELECT vs.* FROM vector_store vs " +
            "JOIN document_units du ON vs.document_unit_id = du.id " +
            "WHERE du.file_id = #{fileId} " +
            "AND vs.deleted = 0 " +
            "ORDER BY vs.embedding <=> #{embedding}::vector " +
            "LIMIT #{limit}")
    List<VectorStorePO> searchSimilarInFile(
            @Param("embedding") String embedding,
            @Param("fileId") Long fileId,
            @Param("limit") int limit);
    
    /**
     * 在知识库中搜索相似向量
     * @param embedding 查询向量
     * @param knowledgeBaseId 知识库ID
     * @param limit 返回数量限制
     * @return 相似的向量列表
     */
    @Select("SELECT vs.* FROM vector_store vs " +
            "JOIN document_units du ON vs.document_unit_id = du.id " +
            "JOIN file_details fd ON du.file_id = fd.id " +
            "WHERE fd.knowledge_base_id = #{knowledgeBaseId} " +
            "AND vs.deleted = 0 " +
            "ORDER BY vs.embedding <=> #{embedding}::vector " +
            "LIMIT #{limit}")
    List<VectorStorePO> searchSimilarInKnowledgeBase(
            @Param("embedding") String embedding, 
            @Param("knowledgeBaseId") Long knowledgeBaseId, 
            @Param("limit") int limit);
    
    /**
     * 在知识库中搜索相似向量（带相似度阈值和分数）
     * 使用余弦相似度：1 - (embedding <=> query)
     * 返回Map包含：document_unit_id, content, page, file_name, file_id, similarity_score
     */
    @Select("SELECT du.id as document_unit_id, du.content, " +
            "       du.page_number as page, du.file_id, " +
            "       COALESCE(fd.file_name, fd.original_name, fd.filename) as file_name, " +
            "       (1 - (vs.embedding <=> #{embedding}::vector)) AS similarity_score " +
            "FROM vector_store vs " +
            "JOIN document_units du ON vs.document_unit_id = du.id " +
            "JOIN file_details fd ON du.file_id = fd.id " +
            "WHERE fd.knowledge_base_id = #{knowledgeBaseId} " +
            "AND vs.deleted = 0 " +
            "AND du.deleted = 0 " +
            "AND fd.deleted = 0 " +
            "AND (1 - (vs.embedding <=> #{embedding}::vector)) >= #{threshold} " +
            "ORDER BY vs.embedding <=> #{embedding}::vector " +
            "LIMIT #{limit}")
    List<Map<String, Object>> searchSimilarWithThreshold(
            @Param("embedding") String embedding,
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("threshold") double threshold,
            @Param("limit") int limit);
    
    /**
     * 关键词全文检索（PostgreSQL全文搜索）
     */
    @Select("SELECT du.id as document_unit_id, du.content, " +
            "       du.page_number as page, " +
            "       COALESCE(fd.file_name, fd.original_name, fd.filename) as file_name, " +
            "       ts_rank(to_tsvector('chinese_cfg', du.content), plainto_tsquery('chinese_cfg', #{query})) AS similarity_score " +
            "FROM document_units du " +
            "JOIN file_details fd ON du.file_id = fd.id " +
            "WHERE fd.knowledge_base_id = #{knowledgeBaseId} " +
            "AND du.deleted = 0 " +
            "AND fd.deleted = 0 " +
            "AND to_tsvector('chinese_cfg', du.content) @@ plainto_tsquery('chinese_cfg', #{query}) " +
            "ORDER BY similarity_score DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> keywordSearch(
            @Param("query") String query,
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("limit") int limit);
}
