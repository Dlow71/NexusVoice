package com.nexusvoice.domain.rag.repository;

import com.nexusvoice.domain.rag.model.entity.UserRagDocument;
import java.util.List;
import java.util.Optional;

/**
 * 用户RAG文档仓储接口
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface UserRagDocumentRepository {
    
    /**
     * 保存用户RAG文档
     */
    UserRagDocument save(UserRagDocument userRagDocument);
    
    /**
     * 批量保存用户RAG文档
     */
    List<UserRagDocument> saveAll(List<UserRagDocument> userRagDocuments);
    
    /**
     * 根据ID查找
     */
    Optional<UserRagDocument> findById(Long id);
    
    /**
     * 根据用户RAG ID查找
     */
    List<UserRagDocument> findByUserRagId(Long userRagId);
    
    /**
     * 根据用户RAG文件ID查找
     */
    List<UserRagDocument> findByUserRagFileId(Long userRagFileId);
    
    /**
     * 根据原始文档ID查找
     */
    List<UserRagDocument> findByOriginalDocumentId(Long originalDocumentId);
    
    /**
     * 查找未向量化的文档
     */
    List<UserRagDocument> findUnvectorized(Long userRagId, int limit);
    
    /**
     * 更新用户RAG文档
     */
    UserRagDocument update(UserRagDocument userRagDocument);
    
    /**
     * 删除用户RAG文档
     */
    boolean deleteById(Long id);
    
    /**
     * 根据用户RAG ID删除所有文档
     */
    int deleteByUserRagId(Long userRagId);
    
    /**
     * 根据用户RAG文件ID删除文档
     */
    int deleteByUserRagFileId(Long userRagFileId);
    
    /**
     * 统计用户RAG文档数量
     */
    int countByUserRagId(Long userRagId);
    
    /**
     * 统计已向量化的文档数量
     */
    int countVectorizedByUserRagId(Long userRagId);
}
