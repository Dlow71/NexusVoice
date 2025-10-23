package com.nexusvoice.domain.rag.service;

import com.nexusvoice.domain.rag.model.entity.DocumentUnit;
import com.nexusvoice.domain.rag.model.entity.FileDetail;
import com.nexusvoice.domain.rag.model.enums.FileType;

import java.util.List;

/**
 * 文档解析领域服务接口
 * 定义文档解析的核心业务逻辑
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface DocumentParserService {
    
    /**
     * 解析文档
     * @param fileDetail 文件详情
     * @param fileContent 文件内容（字节数组）
     * @return 解析出的文档单元列表
     * @throws DocumentParseException 解析异常
     */
    List<DocumentUnit> parseDocument(FileDetail fileDetail, byte[] fileContent) throws DocumentParseException;
    
    /**
     * 判断是否支持该文件类型
     * @param fileType 文件类型
     * @return 是否支持
     */
    boolean supports(FileType fileType);
    
    /**
     * 估算文档页数
     * @param fileDetail 文件详情
     * @param fileContent 文件内容
     * @return 估算的页数
     */
    int estimatePageCount(FileDetail fileDetail, byte[] fileContent);
    
    /**
     * 文档解析异常
     */
    class DocumentParseException extends Exception {
        private String errorCode;
        
        public DocumentParseException(String message) {
            super(message);
        }
        
        public DocumentParseException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public DocumentParseException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        
        public DocumentParseException(String errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
}
