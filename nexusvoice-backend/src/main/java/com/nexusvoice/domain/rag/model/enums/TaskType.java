package com.nexusvoice.domain.rag.model.enums;

/**
 * 任务类型枚举
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public enum TaskType {
    PARSE("文档解析"),
    OCR("OCR识别"),
    SPLIT("文档分割"),
    VECTORIZE("向量化");
    
    private final String description;
    
    TaskType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
