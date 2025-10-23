package com.nexusvoice.domain.rag.repository;

import com.nexusvoice.domain.rag.model.entity.UserRag;
import com.nexusvoice.domain.rag.model.enums.InstallType;
import java.util.List;
import java.util.Optional;

/**
 * 用户RAG仓储接口
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface UserRagRepository {
    
    /**
     * 保存用户RAG
     */
    UserRag save(UserRag userRag);
    
    /**
     * 根据ID查找
     */
    Optional<UserRag> findById(Long id);
    
    /**
     * 根据用户ID查找
     */
    List<UserRag> findByUserId(Long userId);
    
    /**
     * 根据版本ID查找
     */
    List<UserRag> findByRagVersionId(Long ragVersionId);
    
    /**
     * 根据用户ID和原始知识库ID查找
     */
    Optional<UserRag> findByUserIdAndOriginalKnowledgeBaseId(Long userId, Long originalKnowledgeBaseId);
    
    /**
     * 根据安装类型查找
     */
    List<UserRag> findByInstallType(InstallType installType, int limit);
    
    /**
     * 更新用户RAG
     */
    UserRag update(UserRag userRag);
    
    /**
     * 删除用户RAG
     */
    boolean deleteById(Long id);
    
    /**
     * 统计用户安装数量
     */
    int countByUserId(Long userId);
    
    /**
     * 统计版本使用数量
     */
    int countByRagVersionId(Long ragVersionId);
    
    /**
     * 检查用户是否已安装
     */
    boolean existsByUserIdAndRagVersionId(Long userId, Long ragVersionId);
}
