package com.nexusvoice.domain.rag.service;

import com.nexusvoice.domain.rag.model.entity.DocumentUnit;
import com.nexusvoice.domain.rag.model.entity.VectorStore;

import java.util.List;

/**
 * 文档向量化领域服务接口
 * 负责将文档内容转换为向量表示
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface DocumentVectorizerService {
    
    /**
     * 向量化单个文档单元
     * @param documentUnit 文档单元
     * @return 向量实体
     * @throws VectorizationException 向量化异常
     */
    VectorStore vectorize(DocumentUnit documentUnit) throws VectorizationException;
    
    /**
     * 批量向量化文档单元
     * @param documentUnits 文档单元列表
     * @return 向量实体列表
     * @throws VectorizationException 向量化异常
     */
    List<VectorStore> vectorizeBatch(List<DocumentUnit> documentUnits) throws VectorizationException;
    
    /**
     * 向量化文本
     * @param text 文本内容
     * @return 向量数组
     * @throws VectorizationException 向量化异常
     */
    List<Float> vectorizeText(String text) throws VectorizationException;
    
    /**
     * 批量向量化文本
     * @param texts 文本列表
     * @return 向量数组列表
     * @throws VectorizationException 向量化异常
     */
    List<List<Float>> vectorizeTexts(List<String> texts) throws VectorizationException;
    
    /**
     * 获取当前使用的向量模型名称
     * @return 模型名称
     */
    String getEmbeddingModel();
    
    /**
     * 获取向量维度
     * @return 向量维度
     */
    int getEmbeddingDimension();
    
    /**
     * 检查文本是否超过模型的最大长度限制
     * @param text 文本内容
     * @return 是否超过限制
     */
    boolean exceedsMaxLength(String text);
    
    /**
     * 截断文本到模型允许的最大长度
     * @param text 文本内容
     * @return 截断后的文本
     */
    String truncateToMaxLength(String text);
    
    /**
     * 向量化异常
     */
    class VectorizationException extends Exception {
        private String errorCode;
        
        public VectorizationException(String message) {
            super(message);
        }
        
        public VectorizationException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public VectorizationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        
        public VectorizationException(String errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
}
