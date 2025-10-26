package com.nexusvoice.domain.file.repository;

import com.nexusvoice.domain.file.model.TempFile;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 临时文件仓储接口
 * 管理各类临时文件的生命周期（TTS音频、图片、PDF等）
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
public interface TempFileRepository {
    
    /**
     * 保存临时文件记录
     * 
     * @param tempFile 临时文件
     * @param ttl 存活时间（Time To Live）
     */
    void save(TempFile tempFile, Duration ttl);
    
    /**
     * 批量保存临时文件记录
     * 
     * @param tempFiles 临时文件列表
     * @param ttl 存活时间
     */
    void batchSave(List<TempFile> tempFiles, Duration ttl);
    
    /**
     * 查询临时文件
     * 
     * @param fileKey 文件Key
     * @return 临时文件
     */
    Optional<TempFile> findByFileKey(String fileKey);
    
    /**
     * 获取即将过期的文件列表（用于提前清理）
     * 
     * @param limit 返回数量限制
     * @return 即将过期的文件列表
     */
    List<TempFile> findExpiringSoon(int limit);
    
    /**
     * 删除临时文件记录
     * 
     * @param fileKey 文件Key
     */
    void delete(String fileKey);
    
    /**
     * 批量删除临时文件记录
     * 
     * @param fileKeys 文件Key列表
     */
    void batchDelete(List<String> fileKeys);
    
    /**
     * 统计临时文件数量
     * 
     * @return 临时文件总数
     */
    long count();
    
    /**
     * 按文件分类统计数量
     * 
     * @param category 文件分类
     * @return 该分类的文件数量
     */
    long countByCategory(TempFile.FileCategory category);
}
