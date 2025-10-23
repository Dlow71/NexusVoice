package com.nexusvoice.domain.rag.repository;

import com.nexusvoice.domain.rag.model.entity.UserRagFile;
import java.util.List;
import java.util.Optional;

/**
 * 用户RAG文件仓储接口
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface UserRagFileRepository {
    
    /**
     * 保存用户RAG文件
     */
    UserRagFile save(UserRagFile userRagFile);
    
    /**
     * 批量保存用户RAG文件
     */
    List<UserRagFile> saveAll(List<UserRagFile> userRagFiles);
    
    /**
     * 根据ID查找
     */
    Optional<UserRagFile> findById(Long id);
    
    /**
     * 根据用户RAG ID查找
     */
    List<UserRagFile> findByUserRagId(Long userRagId);
    
    /**
     * 根据原始文件ID查找
     */
    List<UserRagFile> findByOriginalFileId(Long originalFileId);
    
    /**
     * 更新用户RAG文件
     */
    UserRagFile update(UserRagFile userRagFile);
    
    /**
     * 删除用户RAG文件
     */
    boolean deleteById(Long id);
    
    /**
     * 根据用户RAG ID删除所有文件
     */
    int deleteByUserRagId(Long userRagId);
    
    /**
     * 统计用户RAG文件数量
     */
    int countByUserRagId(Long userRagId);
    
    /**
     * 计算用户RAG文件总大小
     */
    Long sumFileSizeByUserRagId(Long userRagId);
}
