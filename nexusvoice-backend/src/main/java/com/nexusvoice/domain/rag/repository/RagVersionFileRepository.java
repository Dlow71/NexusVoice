package com.nexusvoice.domain.rag.repository;

import com.nexusvoice.domain.rag.model.entity.RagVersionFile;
import java.util.List;
import java.util.Optional;

/**
 * RAG版本文件仓储接口
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface RagVersionFileRepository {
    
    /**
     * 保存版本文件
     */
    RagVersionFile save(RagVersionFile ragVersionFile);
    
    /**
     * 批量保存版本文件
     */
    List<RagVersionFile> saveAll(List<RagVersionFile> ragVersionFiles);
    
    /**
     * 根据ID查找
     */
    Optional<RagVersionFile> findById(Long id);
    
    /**
     * 根据版本ID查找所有文件
     */
    List<RagVersionFile> findByRagVersionId(Long ragVersionId);
    
    /**
     * 根据原始文件ID查找
     */
    List<RagVersionFile> findByOriginalFileId(Long originalFileId);
    
    /**
     * 更新版本文件
     */
    RagVersionFile update(RagVersionFile ragVersionFile);
    
    /**
     * 删除版本文件
     */
    boolean deleteById(Long id);
    
    /**
     * 根据版本ID删除所有文件
     */
    int deleteByRagVersionId(Long ragVersionId);
    
    /**
     * 统计版本文件数量
     */
    int countByRagVersionId(Long ragVersionId);
    
    /**
     * 计算版本文件总大小
     */
    Long sumFileSizeByRagVersionId(Long ragVersionId);
}
