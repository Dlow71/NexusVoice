package com.nexusvoice.domain.ai.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.HashMap;

/**
 * 增强上下文领域模型
 * 在请求增强链中传递的上下文信息
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
@Data
@Builder
public class EnhancementContext {
    
    /**
     * 原始请求
     */
    private com.nexusvoice.infrastructure.ai.model.ChatRequest originalRequest;
    
    /**
     * 增强后的请求（可被链中的增强器修改）
     */
    private com.nexusvoice.infrastructure.ai.model.ChatRequest enhancedRequest;
    
    /**
     * 是否启用联网搜索
     */
    private Boolean enableWebSearch;
    
    /**
     * 是否启用RAG
     */
    private Boolean enableRag;
    
    /**
     * 是否启用多模态
     */
    private Boolean enableMultiModal;
    
    /**
     * 搜索结果（如果执行了搜索）
     */
    private String searchResults;
    
    /**
     * RAG检索结果（如果执行了RAG）
     */
    private String ragResults;
    
    /**
     * 扩展属性（用于未来扩展）
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
    
    /**
     * 添加属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * 获取属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
    
    /**
     * 是否有增强
     */
    public boolean hasEnhancements() {
        return (enableWebSearch != null && enableWebSearch) ||
               (enableRag != null && enableRag) ||
               (enableMultiModal != null && enableMultiModal);
    }
}
