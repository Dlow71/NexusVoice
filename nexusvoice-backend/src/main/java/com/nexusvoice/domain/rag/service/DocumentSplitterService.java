package com.nexusvoice.domain.rag.service;

import com.nexusvoice.domain.rag.model.entity.DocumentUnit;

import java.util.List;

/**
 * 文档分割领域服务接口
 * 负责将文档内容分割成合适的块
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface DocumentSplitterService {
    
    /**
     * 分割文档单元
     * @param documentUnit 原始文档单元
     * @param maxChunkSize 最大块大小（字符数）
     * @param overlapSize 重叠大小（字符数）
     * @return 分割后的文档单元列表
     */
    List<DocumentUnit> splitDocument(DocumentUnit documentUnit, int maxChunkSize, int overlapSize);
    
    /**
     * 批量分割文档单元
     * @param documentUnits 原始文档单元列表
     * @param maxChunkSize 最大块大小
     * @param overlapSize 重叠大小
     * @return 分割后的文档单元列表
     */
    List<DocumentUnit> splitDocuments(List<DocumentUnit> documentUnits, int maxChunkSize, int overlapSize);
    
    /**
     * 按句子分割
     * @param content 文本内容
     * @param maxChunkSize 最大块大小
     * @param overlapSize 重叠大小
     * @return 分割后的文本列表
     */
    List<String> splitBySentence(String content, int maxChunkSize, int overlapSize);
    
    /**
     * 按段落分割
     * @param content 文本内容
     * @param maxChunkSize 最大块大小
     * @param overlapSize 重叠大小
     * @return 分割后的文本列表
     */
    List<String> splitByParagraph(String content, int maxChunkSize, int overlapSize);
    
    /**
     * 按固定大小分割
     * @param content 文本内容
     * @param chunkSize 块大小
     * @param overlapSize 重叠大小
     * @return 分割后的文本列表
     */
    List<String> splitByFixedSize(String content, int chunkSize, int overlapSize);
    
    /**
     * 智能分割（根据内容类型自动选择分割策略）
     * @param content 文本内容
     * @param language 语言代码
     * @param maxChunkSize 最大块大小
     * @param overlapSize 重叠大小
     * @return 分割后的文本列表
     */
    List<String> smartSplit(String content, String language, int maxChunkSize, int overlapSize);
    
    /**
     * 估算分割后的块数
     * @param contentLength 内容长度
     * @param maxChunkSize 最大块大小
     * @param overlapSize 重叠大小
     * @return 估算的块数
     */
    int estimateChunkCount(int contentLength, int maxChunkSize, int overlapSize);
}
