package com.nexusvoice.domain.rag.model.enums;

/**
 * 解析策略枚举
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public enum ParseStrategy {
    PDF_TEXT("PDF文本提取", FileType.PDF),
    PDF_OCR("PDF OCR识别", FileType.PDF),
    WORD_POI("Word POI解析", FileType.DOCX, FileType.DOC),
    EXCEL_POI("Excel POI解析", FileType.XLSX, FileType.XLS),
    PPT_POI("PPT POI解析", FileType.PPTX, FileType.PPT),
    TEXT_PLAIN("纯文本解析", FileType.TXT),
    MARKDOWN("Markdown解析", FileType.MD),
    HTML("HTML解析", FileType.HTML);
    
    private final String description;
    private final FileType[] supportedTypes;
    
    ParseStrategy(String description, FileType... supportedTypes) {
        this.description = description;
        this.supportedTypes = supportedTypes;
    }
    
    public String getDescription() {
        return description;
    }
    
    public FileType[] getSupportedTypes() {
        return supportedTypes;
    }
    
    /**
     * 判断是否支持指定文件类型
     */
    public boolean supports(FileType fileType) {
        if (fileType == null) {
            return false;
        }
        for (FileType type : supportedTypes) {
            if (type == fileType) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 根据文件类型获取默认解析策略
     */
    public static ParseStrategy getDefaultStrategy(FileType fileType) {
        if (fileType == null) {
            return null;
        }
        
        switch (fileType) {
            case PDF:
                return PDF_TEXT; // 默认使用文本提取
            case DOCX:
            case DOC:
                return WORD_POI;
            case XLSX:
            case XLS:
                return EXCEL_POI;
            case PPTX:
            case PPT:
                return PPT_POI;
            case TXT:
                return TEXT_PLAIN;
            case MD:
                return MARKDOWN;
            case HTML:
                return HTML;
            default:
                return null;
        }
    }
    
    /**
     * 是否需要OCR
     */
    public boolean requiresOcr() {
        return this == PDF_OCR;
    }
}
