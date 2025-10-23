package com.nexusvoice.domain.rag.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 文档OCR领域服务接口
 * 负责从图像中识别文字
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public interface DocumentOcrService {
    
    /**
     * OCR识别单张图片
     * @param imageData 图片数据（字节数组）
     * @param imageFormat 图片格式（jpg、png等）
     * @return OCR识别结果
     * @throws OcrException OCR异常
     */
    OcrResult recognizeImage(byte[] imageData, String imageFormat) throws OcrException;
    
    /**
     * OCR识别Base64编码的图片
     * @param base64Image Base64编码的图片
     * @param imageFormat 图片格式
     * @return OCR识别结果
     * @throws OcrException OCR异常
     */
    OcrResult recognizeBase64Image(String base64Image, String imageFormat) throws OcrException;
    
    /**
     * 批量OCR识别
     * @param images 图片数据列表
     * @return OCR识别结果列表
     * @throws OcrException OCR异常
     */
    List<OcrResult> recognizeBatch(List<byte[]> images) throws OcrException;
    
    /**
     * PDF页面OCR识别
     * @param pdfData PDF数据
     * @param pageNumber 页码（从1开始）
     * @return OCR识别结果
     * @throws OcrException OCR异常
     */
    OcrResult recognizePdfPage(byte[] pdfData, int pageNumber) throws OcrException;
    
    /**
     * PDF全文OCR识别
     * @param pdfData PDF数据
     * @return 每页的OCR识别结果
     * @throws OcrException OCR异常
     */
    List<OcrResult> recognizePdfDocument(byte[] pdfData) throws OcrException;
    
    /**
     * 获取OCR模型名称
     * @return 模型名称
     */
    String getOcrModel();
    
    /**
     * 检查是否支持该图片格式
     * @param imageFormat 图片格式
     * @return 是否支持
     */
    boolean supportsFormat(String imageFormat);
    
    /**
     * OCR识别结果
     */
    class OcrResult {
        private String text;
        private BigDecimal confidence;
        private String language;
        private Integer pageNumber;
        private List<TextRegion> regions;
        
        public OcrResult() {}
        
        public OcrResult(String text, BigDecimal confidence) {
            this.text = text;
            this.confidence = confidence;
        }
        
        // Getters and Setters
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public BigDecimal getConfidence() {
            return confidence;
        }
        
        public void setConfidence(BigDecimal confidence) {
            this.confidence = confidence;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public void setLanguage(String language) {
            this.language = language;
        }
        
        public Integer getPageNumber() {
            return pageNumber;
        }
        
        public void setPageNumber(Integer pageNumber) {
            this.pageNumber = pageNumber;
        }
        
        public List<TextRegion> getRegions() {
            return regions;
        }
        
        public void setRegions(List<TextRegion> regions) {
            this.regions = regions;
        }
    }
    
    /**
     * 文本区域
     */
    class TextRegion {
        private String text;
        private BigDecimal confidence;
        private int x;
        private int y;
        private int width;
        private int height;
        
        // Getters and Setters
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public BigDecimal getConfidence() {
            return confidence;
        }
        
        public void setConfidence(BigDecimal confidence) {
            this.confidence = confidence;
        }
        
        public int getX() {
            return x;
        }
        
        public void setX(int x) {
            this.x = x;
        }
        
        public int getY() {
            return y;
        }
        
        public void setY(int y) {
            this.y = y;
        }
        
        public int getWidth() {
            return width;
        }
        
        public void setWidth(int width) {
            this.width = width;
        }
        
        public int getHeight() {
            return height;
        }
        
        public void setHeight(int height) {
            this.height = height;
        }
    }
    
    /**
     * OCR异常
     */
    class OcrException extends Exception {
        private String errorCode;
        
        public OcrException(String message) {
            super(message);
        }
        
        public OcrException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public OcrException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        
        public OcrException(String errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
}
