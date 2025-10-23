package com.nexusvoice.domain.rag.repository;

import com.nexusvoice.domain.rag.model.entity.DocumentUnit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 文档单元仓储接口
 * 纯领域层接口，不依赖任何基础设施
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface DocumentUnitRepository {
    
    /**
     * 保存文档单元
     * @param documentUnit 文档单元实体
     * @return 保存后的文档单元
     */
    DocumentUnit save(DocumentUnit documentUnit);
    
    /**
     * 批量保存文档单元
     * @param documentUnits 文档单元列表
     * @return 保存后的文档单元列表
     */
    List<DocumentUnit> saveAll(List<DocumentUnit> documentUnits);
    
    /**
     * 根据ID查找文档单元
     * @param id 文档单元ID
     * @return 文档单元实体
     */
    Optional<DocumentUnit> findById(Long id);
    
    /**
     * 根据文件ID查找文档单元列表
     * @param fileId 文件ID
     * @return 文档单元列表
     */
    List<DocumentUnit> findByFileId(Long fileId);
    
    /**
     * 根据文件ID和页码查找文档单元
     * @param fileId 文件ID
     * @param pageNumber 页码
     * @return 文档单元列表
     */
    List<DocumentUnit> findByFileIdAndPageNumber(Long fileId, Integer pageNumber);
    
    /**
     * 根据文件ID和分块索引查找文档单元
     * @param fileId 文件ID
     * @param chunkIndex 分块索引
     * @return 文档单元实体
     */
    Optional<DocumentUnit> findByFileIdAndChunkIndex(Long fileId, Integer chunkIndex);
    
    /**
     * 查找未向量化的文档单元
     * @param limit 限制数量
     * @return 文档单元列表
     */
    List<DocumentUnit> findUnvectorized(int limit);
    
    /**
     * 查找指定文件未向量化的文档单元
     * @param fileId 文件ID
     * @return 文档单元列表
     */
    List<DocumentUnit> findUnvectorizedByFileId(Long fileId);
    
    /**
     * 更新文档单元
     * @param documentUnit 文档单元实体
     * @return 更新后的文档单元
     */
    DocumentUnit update(DocumentUnit documentUnit);
    
    /**
     * 删除文档单元（逻辑删除）
     * @param id 文档单元ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);
    
    /**
     * 根据文件ID删除所有文档单元
     * @param fileId 文件ID
     * @return 删除的数量
     */
    int deleteByFileId(Long fileId);
    
    /**
     * 标记为已向量化
     * @param id 文档单元ID
     */
    void markAsVectorized(Long id);
    
    /**
     * 批量标记为已向量化
     * @param ids 文档单元ID列表
     */
    void markAsVectorizedBatch(List<Long> ids);
    
    /**
     * 统计文件的文档单元数量
     * @param fileId 文件ID
     * @return 文档单元数量
     */
    int countByFileId(Long fileId);
    
    /**
     * 统计文件的总字符数
     * @param fileId 文件ID
     * @return 总字符数
     */
    Long sumCharCountByFileId(Long fileId);
    
    /**
     * 统计文件的总Token数
     * @param fileId 文件ID
     * @return 总Token数
     */
    Long sumTokenCountByFileId(Long fileId);
    
    /**
     * 统计文件已向量化的单元数量
     * @param fileId 文件ID
     * @return 已向量化数量
     */
    int countVectorizedByFileId(Long fileId);
    
    /**
     * 查找OCR处理的文档单元
     * @param fileId 文件ID
     * @return 文档单元列表
     */
    List<DocumentUnit> findOcrUnitsByFileId(Long fileId);
    
    /**
     * 根据语言查找文档单元
     * @param fileId 文件ID
     * @param language 语言代码
     * @return 文档单元列表
     */
    List<DocumentUnit> findByFileIdAndLanguage(Long fileId, String language);
    
    /**
     * 更新OCR信息
     * @param id 文档单元ID
     * @param confidence OCR置信度
     */
    void updateOcrInfo(Long id, BigDecimal confidence);
}
