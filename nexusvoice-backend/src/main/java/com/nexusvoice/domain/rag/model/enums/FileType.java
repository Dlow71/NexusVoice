package com.nexusvoice.domain.rag.model.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 文件类型枚举
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public enum FileType {
    PDF("PDF", "application/pdf", Arrays.asList(".pdf")),
    DOCX("DOCX", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", Arrays.asList(".docx")),
    DOC("DOC", "application/msword", Arrays.asList(".doc")),
    TXT("TXT", "text/plain", Arrays.asList(".txt")),
    MD("MD", "text/markdown", Arrays.asList(".md", ".markdown")),
    MARKDOWN("MARKDOWN", "text/markdown", Arrays.asList(".md", ".markdown")),
    PPTX("PPTX", "application/vnd.openxmlformats-officedocument.presentationml.presentation", Arrays.asList(".pptx")),
    PPT("PPT", "application/vnd.ms-powerpoint", Arrays.asList(".ppt")),
    XLSX("XLSX", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Arrays.asList(".xlsx")),
    XLS("XLS", "application/vnd.ms-excel", Arrays.asList(".xls")),
    HTML("HTML", "text/html", Arrays.asList(".html", ".htm"));
    
    private final String code;
    private final String mimeType;
    private final List<String> extensions;
    
    FileType(String code, String mimeType, List<String> extensions) {
        this.code = code;
        this.mimeType = mimeType;
        this.extensions = extensions;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public List<String> getExtensions() {
        return extensions;
    }
    
    /**
     * 根据文件扩展名获取文件类型
     */
    public static FileType fromExtension(String extension) {
        if (extension == null) {
            return null;
        }
        String ext = extension.toLowerCase();
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }
        for (FileType type : FileType.values()) {
            if (type.extensions.contains(ext)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 根据MIME类型获取文件类型
     */
    public static FileType fromMimeType(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        for (FileType type : FileType.values()) {
            if (type.mimeType.equals(mimeType)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 判断是否为Office文档
     */
    public boolean isOfficeDocument() {
        return this == DOCX || this == DOC || this == PPTX || this == PPT || this == XLSX || this == XLS;
    }
    
    /**
     * 判断是否为文本文件
     */
    public boolean isTextFile() {
        return this == TXT || this == MD || this == HTML;
    }
    
    /**
     * 判断是否需要OCR处理
     */
    public boolean needsOcr() {
        return this == PDF;
    }
}
