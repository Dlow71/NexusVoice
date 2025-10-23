package com.nexusvoice.domain.rag.repository;

import com.nexusvoice.domain.rag.model.entity.FileDetail;
import com.nexusvoice.domain.rag.model.enums.FileType;
import com.nexusvoice.domain.rag.model.enums.ProcessStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文件详情仓储接口
 * 纯领域层接口，不依赖任何基础设施
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface FileDetailRepository {
    
    /**
     * 保存文件详情
     * @param fileDetail 文件详情实体
     * @return 保存后的文件详情
     */
    FileDetail save(FileDetail fileDetail);
    
    /**
     * 根据ID查找文件详情
     * @param id 文件ID
     * @return 文件详情实体
     */
    Optional<FileDetail> findById(Long id);
    
    /**
     * 根据用户ID查找文件列表
     * @param userId 用户ID
     * @return 文件列表
     */
    List<FileDetail> findByUserId(Long userId);
    
    /**
     * 根据知识库ID查找文件列表
     * @param knowledgeBaseId 知识库ID
     * @return 文件列表
     */
    List<FileDetail> findByKnowledgeBaseId(Long knowledgeBaseId);
    
    /**
     * 根据状态查找文件列表
     * @param status 处理状态
     * @return 文件列表
     */
    List<FileDetail> findByStatus(ProcessStatus status);
    
    /**
     * 根据文件哈希查找
     * @param fileHash 文件哈希值
     * @return 文件详情实体
     */
    Optional<FileDetail> findByFileHash(String fileHash);
    
    /**
     * 更新文件详情
     * @param fileDetail 文件详情实体
     * @return 更新后的文件详情
     */
    FileDetail update(FileDetail fileDetail);
    
    /**
     * 删除文件（逻辑删除）
     * @param id 文件ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);
    
    /**
     * 批量删除文件（逻辑删除）
     * @param ids 文件ID列表
     * @return 删除的数量
     */
    int deleteByIds(List<Long> ids);
    
    /**
     * 更新处理状态
     * @param id 文件ID
     * @param status 新状态
     */
    void updateStatus(Long id, ProcessStatus status);
    
    /**
     * 更新处理进度
     * @param id 文件ID
     * @param currentPage 当前页数
     * @param progress 进度百分比
     */
    void updateProgress(Long id, Integer currentPage, BigDecimal progress);
    
    /**
     * 更新错误信息
     * @param id 文件ID
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     */
    void updateError(Long id, String errorCode, String errorMessage);
    
    /**
     * 统计用户文件数量
     * @param userId 用户ID
     * @return 文件数量
     */
    int countByUserId(Long userId);
    
    /**
     * 统计知识库文件数量
     * @param knowledgeBaseId 知识库ID
     * @return 文件数量
     */
    int countByKnowledgeBaseId(Long knowledgeBaseId);
    
    /**
     * 计算用户文件总大小
     * @param userId 用户ID
     * @return 总大小（字节）
     */
    Long sumFileSizeByUserId(Long userId);
    
    /**
     * 计算知识库文件总大小
     * @param knowledgeBaseId 知识库ID
     * @return 总大小（字节）
     */
    Long sumFileSizeByKnowledgeBaseId(Long knowledgeBaseId);
    
    /**
     * 查找待处理的文件
     * @param limit 限制数量
     * @return 文件列表
     */
    List<FileDetail> findPendingFiles(int limit);
    
    /**
     * 查找处理失败的文件
     * @param userId 用户ID
     * @return 文件列表
     */
    List<FileDetail> findFailedFilesByUserId(Long userId);
    
    /**
     * 查找指定时间之前的文件
     * @param beforeTime 时间点
     * @return 文件列表
     */
    List<FileDetail> findByCreatedBefore(LocalDateTime beforeTime);
    
    /**
     * 检查文件是否属于用户
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否属于
     */
    boolean belongsToUser(Long fileId, Long userId);
    
    /**
     * 根据文件类型统计
     * @param userId 用户ID
     * @param fileType 文件类型
     * @return 文件数量
     */
    int countByUserIdAndFileType(Long userId, FileType fileType);
}
