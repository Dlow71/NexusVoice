package com.nexusvoice.domain.rag.repository;

import com.nexusvoice.domain.rag.model.entity.RagVersion;
import java.util.List;
import java.util.Optional;

/**
 * RAG版本仓储接口
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface RagVersionRepository {
    
    /**
     * 保存版本
     */
    RagVersion save(RagVersion ragVersion);
    
    /**
     * 根据ID查找
     */
    Optional<RagVersion> findById(Long id);
    
    /**
     * 根据知识库ID查找所有版本
     */
    List<RagVersion> findByKnowledgeBaseId(Long knowledgeBaseId);
    
    /**
     * 根据知识库ID和版本号查找
     */
    Optional<RagVersion> findByKnowledgeBaseIdAndVersion(Long knowledgeBaseId, String version);
    
    /**
     * 查找已发布的版本
     */
    List<RagVersion> findPublished(int limit);
    
    /**
     * 更新版本
     */
    RagVersion update(RagVersion ragVersion);
    
    /**
     * 删除版本
     */
    boolean deleteById(Long id);
    
    /**
     * 统计版本数量
     */
    int countByKnowledgeBaseId(Long knowledgeBaseId);
}
