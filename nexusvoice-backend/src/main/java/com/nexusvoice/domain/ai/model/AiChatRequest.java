package com.nexusvoice.domain.ai.model;

import java.util.List;

/**
 * AI聊天请求领域模型
 * 纯净的领域对象，不包含任何技术框架注解
 * 
 * @author NexusVoice
 * @since 2025-10-21
 */
public class AiChatRequest {

    /**
     * 消息列表
     */
    private List<AiMessage> messages;

    /**
     * 模型名称（如：openai:gpt-4）
     */
    private String model;

    /**
     * 温度参数 (0-2)
     * 控制回复的随机性，数值越高越随机
     */
    private Double temperature;

    /**
     * 最大令牌数
     */
    private Integer maxTokens;

    /**
     * Top-p采样参数
     */
    private Double topP;

    /**
     * 频率惩罚参数
     */
    private Double frequencyPenalty;

    /**
     * 存在惩罚参数
     */
    private Double presencePenalty;

    /**
     * 停止词列表
     */
    private List<String> stop;

    /**
     * 是否流式输出
     */
    private Boolean stream;

    /**
     * 用户ID（用于追踪和限流）
     */
    private Long userId;

    /**
     * 对话ID（用于上下文管理）
     */
    private Long conversationId;

    /**
     * 是否启用联网搜索
     */
    private Boolean enableWebSearch;
    
    /**
     * 是否启用RAG（知识库检索增强）
     */
    private Boolean enableRag;
    
    /**
     * 是否启用多模态（图片、视频等）
     */
    private Boolean enableMultiModal;
    
    /**
     * RAG知识库ID列表
     */
    private List<Long> knowledgeBaseIds;
    
    /**
     * 图像URL列表（支持多图输入）
     */
    private List<String> imageUrls;
    
    /**
     * Base64编码的图像数据（单图输入）
     */
    private String imageBase64;

    /**
     * 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建基础请求
     */
    public static AiChatRequest createBasicRequest(List<AiMessage> messages, String model) {
        AiChatRequest request = new AiChatRequest();
        request.messages = messages;
        request.model = model;
        request.temperature = 0.7;
        request.maxTokens = 2000;
        request.topP = 1.0;
        request.frequencyPenalty = 0.0;
        request.presencePenalty = 0.0;
        request.stream = false;
        request.enableWebSearch = false;
        request.enableRag = false;
        request.enableMultiModal = false;
        return request;
    }

    /**
     * 是否有增强功能启用
     */
    public boolean hasEnhancements() {
        return Boolean.TRUE.equals(enableWebSearch) || 
               Boolean.TRUE.equals(enableRag) || 
               Boolean.TRUE.equals(enableMultiModal);
    }

    /**
     * 建造器模式
     */
    public static class Builder {
        private AiChatRequest request = new AiChatRequest();

        public Builder messages(List<AiMessage> messages) {
            request.messages = messages;
            return this;
        }

        public Builder model(String model) {
            request.model = model;
            return this;
        }

        public Builder temperature(Double temperature) {
            request.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            request.maxTokens = maxTokens;
            return this;
        }

        public Builder topP(Double topP) {
            request.topP = topP;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            request.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            request.presencePenalty = presencePenalty;
            return this;
        }

        public Builder stop(List<String> stop) {
            request.stop = stop;
            return this;
        }

        public Builder stream(Boolean stream) {
            request.stream = stream;
            return this;
        }

        public Builder userId(Long userId) {
            request.userId = userId;
            return this;
        }

        public Builder conversationId(Long conversationId) {
            request.conversationId = conversationId;
            return this;
        }

        public Builder enableWebSearch(Boolean enableWebSearch) {
            request.enableWebSearch = enableWebSearch;
            return this;
        }

        public Builder enableRag(Boolean enableRag) {
            request.enableRag = enableRag;
            return this;
        }

        public Builder enableMultiModal(Boolean enableMultiModal) {
            request.enableMultiModal = enableMultiModal;
            return this;
        }

        public Builder knowledgeBaseIds(List<Long> knowledgeBaseIds) {
            request.knowledgeBaseIds = knowledgeBaseIds;
            return this;
        }
        
        public Builder imageUrls(List<String> imageUrls) {
            request.imageUrls = imageUrls;
            return this;
        }
        
        public Builder imageBase64(String imageBase64) {
            request.imageBase64 = imageBase64;
            return this;
        }

        public AiChatRequest build() {
            return request;
        }
    }

    // Getter methods
    public List<AiMessage> getMessages() {
        return messages;
    }

    public String getModel() {
        return model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public Double getTopP() {
        return topP;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public List<String> getStop() {
        return stop;
    }

    public Boolean getStream() {
        return stream;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Boolean getEnableWebSearch() {
        return enableWebSearch;
    }

    public Boolean getEnableRag() {
        return enableRag;
    }

    public Boolean getEnableMultiModal() {
        return enableMultiModal;
    }

    public List<Long> getKnowledgeBaseIds() {
        return knowledgeBaseIds;
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }
    
    public String getImageBase64() {
        return imageBase64;
    }

    // Setter methods
    public void setMessages(List<AiMessage> messages) {
        this.messages = messages;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public void setEnableWebSearch(Boolean enableWebSearch) {
        this.enableWebSearch = enableWebSearch;
    }

    public void setEnableRag(Boolean enableRag) {
        this.enableRag = enableRag;
    }

    public void setEnableMultiModal(Boolean enableMultiModal) {
        this.enableMultiModal = enableMultiModal;
    }

    public void setKnowledgeBaseIds(List<Long> knowledgeBaseIds) {
        this.knowledgeBaseIds = knowledgeBaseIds;
    }
    
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    
    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
