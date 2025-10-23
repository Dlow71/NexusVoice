package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.VectorStorePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

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
}
