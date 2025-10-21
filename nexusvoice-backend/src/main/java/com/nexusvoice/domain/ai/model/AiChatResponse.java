package com.nexusvoice.domain.ai.model;

/**
 * AI聊天响应领域模型
 * 纯净的领域对象，不包含任何技术框架注解
 * 
 * @author NexusVoice
 * @since 2025-10-21
 */
public class AiChatResponse {

    /**
     * 响应内容
     */
    private String content;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误消息（当失败时）
     */
    private String errorMessage;

    /**
     * Token使用情况
     */
    private TokenUsage usage;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTimeMs;

    /**
     * 请求ID（用于追踪）
     */
    private String requestId;

    /**
     * 构造函数
     */
    public AiChatResponse() {
        this.success = true;
    }

    /**
     * 创建成功响应
     */
    public static AiChatResponse success(String content, String model) {
        AiChatResponse response = new AiChatResponse();
        response.content = content;
        response.model = model;
        response.success = true;
        return response;
    }

    /**
     * 创建失败响应
     */
    public static AiChatResponse failure(String errorMessage) {
        AiChatResponse response = new AiChatResponse();
        response.success = false;
        response.errorMessage = errorMessage;
        return response;
    }

    /**
     * Token使用情况
     */
    public static class TokenUsage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;

        public TokenUsage() {
        }

        public TokenUsage(Integer promptTokens, Integer completionTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = promptTokens + completionTokens;
        }

        // Getter and Setter methods
        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
            updateTotalTokens();
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
            updateTotalTokens();
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }

        private void updateTotalTokens() {
            if (promptTokens != null && completionTokens != null) {
                this.totalTokens = promptTokens + completionTokens;
            }
        }
    }

    // Getter and Setter methods
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public TokenUsage getUsage() {
        return usage;
    }

    public void setUsage(TokenUsage usage) {
        this.usage = usage;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
