package com.nexusvoice.domain.rag.model.enums;

/**
 * 知识库状态枚举
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public enum KnowledgeBaseStatus {
    ACTIVE("活跃"),
    ARCHIVED("已归档"),
    PROCESSING("处理中");
    
    private final String description;
    
    KnowledgeBaseStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 是否可用
     */
    public boolean isAvailable() {
        return this == ACTIVE;
    }
}
