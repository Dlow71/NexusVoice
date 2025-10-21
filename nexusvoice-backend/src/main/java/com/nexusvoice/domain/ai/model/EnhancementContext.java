package com.nexusvoice.domain.ai.model;

import java.util.Map;
import java.util.HashMap;

/**
 * 增强上下文领域模型
 * 在请求增强链中传递的上下文信息
 * 纯净的领域对象，不依赖infrastructure层
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
public class EnhancementContext {
    
    /**
     * 原始请求
     */
    private AiChatRequest originalRequest;
    
    /**
     * 增强后的请求（可被链中的增强器修改）
     */
    private AiChatRequest enhancedRequest;
    
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
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * 构造函数
     */
    public EnhancementContext() {
        this.attributes = new HashMap<>();
    }

    /**
     * 构建器
     */
    public static Builder builder() {
        return new Builder();
    }
    
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
        return Boolean.TRUE.equals(enableWebSearch) ||
               Boolean.TRUE.equals(enableRag) ||
               Boolean.TRUE.equals(enableMultiModal);
    }

    /**
     * 构建器模式
     */
    public static class Builder {
        private EnhancementContext context = new EnhancementContext();

        public Builder originalRequest(AiChatRequest originalRequest) {
            context.originalRequest = originalRequest;
            return this;
        }

        public Builder enhancedRequest(AiChatRequest enhancedRequest) {
            context.enhancedRequest = enhancedRequest;
            return this;
        }

        public Builder enableWebSearch(Boolean enableWebSearch) {
            context.enableWebSearch = enableWebSearch;
            return this;
        }

        public Builder enableRag(Boolean enableRag) {
            context.enableRag = enableRag;
            return this;
        }

        public Builder enableMultiModal(Boolean enableMultiModal) {
            context.enableMultiModal = enableMultiModal;
            return this;
        }

        public Builder searchResults(String searchResults) {
            context.searchResults = searchResults;
            return this;
        }

        public Builder ragResults(String ragResults) {
            context.ragResults = ragResults;
            return this;
        }

        public Builder attribute(String key, Object value) {
            context.attributes.put(key, value);
            return this;
        }

        public EnhancementContext build() {
            return context;
        }
    }

    // Getter and Setter methods
    public AiChatRequest getOriginalRequest() {
        return originalRequest;
    }

    public void setOriginalRequest(AiChatRequest originalRequest) {
        this.originalRequest = originalRequest;
    }

    public AiChatRequest getEnhancedRequest() {
        return enhancedRequest;
    }

    public void setEnhancedRequest(AiChatRequest enhancedRequest) {
        this.enhancedRequest = enhancedRequest;
    }

    public Boolean getEnableWebSearch() {
        return enableWebSearch;
    }

    public void setEnableWebSearch(Boolean enableWebSearch) {
        this.enableWebSearch = enableWebSearch;
    }

    public Boolean getEnableRag() {
        return enableRag;
    }

    public void setEnableRag(Boolean enableRag) {
        this.enableRag = enableRag;
    }

    public Boolean getEnableMultiModal() {
        return enableMultiModal;
    }

    public void setEnableMultiModal(Boolean enableMultiModal) {
        this.enableMultiModal = enableMultiModal;
    }

    public String getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(String searchResults) {
        this.searchResults = searchResults;
    }

    public String getRagResults() {
        return ragResults;
    }

    public void setRagResults(String ragResults) {
        this.ragResults = ragResults;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
