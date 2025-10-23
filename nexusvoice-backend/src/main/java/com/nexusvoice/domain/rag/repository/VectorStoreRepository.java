package com.nexusvoice.domain.rag.repository;

import com.nexusvoice.domain.rag.model.entity.VectorStore;

import java.util.List;
import java.util.Optional;

/**
 * 向量存储仓储接口
 * 纯领域层接口，不依赖任何基础设施
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface VectorStoreRepository {
    
    /**
     * 保存向量
     * @param vectorStore 向量实体
     * @return 保存后的向量
     */
    VectorStore save(VectorStore vectorStore);
    
    /**
     * 批量保存向量
     * @param vectorStores 向量列表
     * @return 保存后的向量列表
     */
    List<VectorStore> saveAll(List<VectorStore> vectorStores);
    
    /**
     * 根据ID查找向量
     * @param id 向量ID（UUID字符串）
     * @return 向量实体
     */
    Optional<VectorStore> findById(String id);
    
    /**
     * 根据文档单元ID查找向量
     * @param documentUnitId 文档单元ID
     * @return 向量实体
     */
    Optional<VectorStore> findByDocumentUnitId(Long documentUnitId);
    
    /**
     * 根据文档单元ID列表查找向量
     * @param documentUnitIds 文档单元ID列表
     * @return 向量列表
     */
    List<VectorStore> findByDocumentUnitIds(List<Long> documentUnitIds);
    
    /**
     * 相似度搜索（使用余弦相似度）
     * @param queryEmbedding 查询向量
     * @param limit 返回数量限制
     * @return 相似的向量列表（按相似度降序）
     */
    List<VectorStore> findSimilar(List<Float> queryEmbedding, int limit);
    
    /**
     * 在指定文件中进行相似度搜索
     * @param fileId 文件ID
     * @param queryEmbedding 查询向量
     * @param limit 返回数量限制
     * @return 相似的向量列表
     */
    List<VectorStore> findSimilarInFile(Long fileId, List<Float> queryEmbedding, int limit);
    
    /**
     * 在指定知识库中进行相似度搜索
     * @param knowledgeBaseId 知识库ID
     * @param queryEmbedding 查询向量
     * @param limit 返回数量限制
     * @return 相似的向量列表
     */
    List<VectorStore> findSimilarInKnowledgeBase(Long knowledgeBaseId, List<Float> queryEmbedding, int limit);
    
    /**
     * 带相似度阈值的搜索
     * @param queryEmbedding 查询向量
     * @param similarityThreshold 相似度阈值（0-1）
     * @param limit 返回数量限制
     * @return 相似的向量列表
     */
    List<VectorStore> findSimilarWithThreshold(List<Float> queryEmbedding, double similarityThreshold, int limit);
    
    /**
     * 更新向量
     * @param embedding 向量实体
     * @return 更新后的向量
     */
    VectorStore update(VectorStore vectorStore);
    
    /**
     * 删除向量（逻辑删除）
     * @param id 向量ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);
    
    /**
     * 根据文档单元ID删除向量
     * @param documentUnitId 文档单元ID
     * @return 是否删除成功
     */
    boolean deleteByDocumentUnitId(Long documentUnitId);
    
    /**
     * 批量删除向量
     * @param ids 向量ID列表
     * @return 删除的数量
     */
    int deleteByIds(List<Long> ids);
    
    /**
     * 统计向量总数
     * @return 向量总数
     */
    long count();
    
    /**
     * 统计特定模型的向量数
     * @param embeddingModel 向量模型名称
     * @return 向量数量
     */
    long countByModel(String embeddingModel);
    
    /**
     * 根据模型查找向量
     * @param embeddingModel 向量模型名称
     * @param limit 限制数量
     * @return 向量列表
     */
    List<VectorStore> findByModel(String embeddingModel, int limit);
    
    /**
     * 检查文档单元是否已有向量
     * @param documentUnitId 文档单元ID
     * @return 是否存在
     */
    boolean existsByDocumentUnitId(Long documentUnitId);
    
    /**
     * 清理无效向量（文档单元已删除的）
     * @return 清理的数量
     */
    int cleanupOrphanedEmbeddings();
}
