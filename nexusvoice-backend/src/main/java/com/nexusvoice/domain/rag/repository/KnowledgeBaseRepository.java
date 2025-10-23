package com.nexusvoice.domain.rag.repository;

import com.nexusvoice.domain.rag.model.entity.KnowledgeBase;
import com.nexusvoice.domain.rag.model.enums.KnowledgeBaseStatus;

import java.util.List;
import java.util.Optional;

/**
 * 知识库仓储接口
 * 纯领域层接口，不依赖任何基础设施
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface KnowledgeBaseRepository {
    
    /**
     * 保存知识库
     * @param knowledgeBase 知识库实体
     * @return 保存后的知识库
     */
    KnowledgeBase save(KnowledgeBase knowledgeBase);
    
    /**
     * 根据ID查找知识库
     * @param id 知识库ID
     * @return 知识库实体
     */
    Optional<KnowledgeBase> findById(Long id);
    
    /**
     * 根据用户ID查找知识库列表
     * @param userId 用户ID
     * @return 知识库列表
     */
    List<KnowledgeBase> findByUserId(Long userId);
    
    /**
     * 根据用户ID和状态查找知识库列表
     * @param userId 用户ID
     * @param status 知识库状态
     * @return 知识库列表
     */
    List<KnowledgeBase> findByUserIdAndStatus(Long userId, KnowledgeBaseStatus status);
    
    /**
     * 根据名称查找知识库
     * @param userId 用户ID
     * @param name 知识库名称
     * @return 知识库实体
     */
    Optional<KnowledgeBase> findByUserIdAndName(Long userId, String name);
    
    /**
     * 更新知识库
     * @param knowledgeBase 知识库实体
     * @return 更新后的知识库
     */
    KnowledgeBase update(KnowledgeBase knowledgeBase);
    
    /**
     * 删除知识库（逻辑删除）
     * @param id 知识库ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);
    
    /**
     * 统计用户的知识库数量
     * @param userId 用户ID
     * @return 知识库数量
     */
    int countByUserId(Long userId);
    
    /**
     * 检查知识库名称是否存在
     * @param userId 用户ID
     * @param name 知识库名称
     * @return 是否存在
     */
    boolean existsByUserIdAndName(Long userId, String name);
    
    /**
     * 增加文件计数和大小
     * @param id 知识库ID
     * @param fileSize 文件大小
     */
    void incrementFileCount(Long id, Long fileSize);
    
    /**
     * 减少文件计数和大小
     * @param id 知识库ID
     * @param fileSize 文件大小
     */
    void decrementFileCount(Long id, Long fileSize);
    
    /**
     * 更新知识库状态
     * @param id 知识库ID
     * @param status 新状态
     */
    void updateStatus(Long id, KnowledgeBaseStatus status);
}
