package com.nexusvoice.domain.rag.repository;

import com.nexusvoice.domain.rag.model.entity.RagVersionDocument;
import java.util.List;
import java.util.Optional;

/**
 * RAG版本文档仓储接口
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface RagVersionDocumentRepository {
    
    /**
     * 保存版本文档
     */
    RagVersionDocument save(RagVersionDocument ragVersionDocument);
    
    /**
     * 批量保存版本文档
     */
    List<RagVersionDocument> saveAll(List<RagVersionDocument> ragVersionDocuments);
    
    /**
     * 根据ID查找
     */
    Optional<RagVersionDocument> findById(Long id);
    
    /**
     * 根据版本ID查找所有文档
     */
    List<RagVersionDocument> findByRagVersionId(Long ragVersionId);
    
    /**
     * 根据版本文件ID查找文档
     */
    List<RagVersionDocument> findByRagVersionFileId(Long ragVersionFileId);
    
    /**
     * 根据原始文档ID查找
     */
    List<RagVersionDocument> findByOriginalDocumentId(Long originalDocumentId);
    
    /**
     * 查找未向量化的文档
     */
    List<RagVersionDocument> findUnvectorized(Long ragVersionId, int limit);
    
    /**
     * 更新版本文档
     */
    RagVersionDocument update(RagVersionDocument ragVersionDocument);
    
    /**
     * 删除版本文档
     */
    boolean deleteById(Long id);
    
    /**
     * 根据版本ID删除所有文档
     */
    int deleteByRagVersionId(Long ragVersionId);
    
    /**
     * 根据版本文件ID删除文档
     */
    int deleteByRagVersionFileId(Long ragVersionFileId);
    
    /**
     * 统计版本文档数量
     */
    int countByRagVersionId(Long ragVersionId);
    
    /**
     * 统计已向量化的文档数量
     */
    int countVectorizedByRagVersionId(Long ragVersionId);
}
