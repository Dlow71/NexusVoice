package com.nexusvoice.domain.video.model;

/**
 * 视频生成结果领域模型
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
public class VideoGenerationResult {
    
    /**
     * 任务ID（用于异步查询）
     */
    private String taskId;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 任务状态（PROCESSING处理中、SUCCESS成功、FAIL失败）
     */
    private String taskStatus;
    
    /**
     * 生成的视频URL
     */
    private String videoUrl;
    
    /**
     * 视频封面图URL
     */
    private String coverImageUrl;
    
    /**
     * 使用的模型名称
     */
    private String modelName;
    
    /**
     * 视频生成耗时（毫秒）
     */
    private Long generationTime;
    
    /**
     * 创建时间（Unix时间戳，秒）
     */
    private Long createdAt;
    
    /**
     * Token使用情况
     */
    private TokenUsage tokenUsage;
    
    /**
     * 原始响应（用于调试）
     */
    private String rawResponse;
    
    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;
    
    public VideoGenerationResult() {
    }
    
    /**
     * 创建处理中的结果
     */
    public static VideoGenerationResult processing(String taskId, String requestId, String modelName) {
        VideoGenerationResult result = new VideoGenerationResult();
        result.setTaskId(taskId);
        result.setRequestId(requestId);
        result.setTaskStatus("PROCESSING");
        result.setModelName(modelName);
        result.setCreatedAt(System.currentTimeMillis() / 1000);
        return result;
    }
    
    /**
     * 创建成功的结果
     */
    public static VideoGenerationResult success(String taskId, String videoUrl, String coverImageUrl, 
                                                String modelName, Long generationTime) {
        VideoGenerationResult result = new VideoGenerationResult();
        result.setTaskId(taskId);
        result.setTaskStatus("SUCCESS");
        result.setVideoUrl(videoUrl);
        result.setCoverImageUrl(coverImageUrl);
        result.setModelName(modelName);
        result.setGenerationTime(generationTime);
        return result;
    }
    
    /**
     * 创建失败的结果
     */
    public static VideoGenerationResult failure(String taskId, String errorMessage) {
        VideoGenerationResult result = new VideoGenerationResult();
        result.setTaskId(taskId);
        result.setTaskStatus("FAIL");
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // Getters and Setters
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getTaskStatus() {
        return taskStatus;
    }
    
    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }
    
    public String getVideoUrl() {
        return videoUrl;
    }
    
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
    
    public String getCoverImageUrl() {
        return coverImageUrl;
    }
    
    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public Long getGenerationTime() {
        return generationTime;
    }
    
    public void setGenerationTime(Long generationTime) {
        this.generationTime = generationTime;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public TokenUsage getTokenUsage() {
        return tokenUsage;
    }
    
    public void setTokenUsage(TokenUsage tokenUsage) {
        this.tokenUsage = tokenUsage;
    }
    
    public String getRawResponse() {
        return rawResponse;
    }
    
    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * 是否处理中
     */
    public boolean isProcessing() {
        return "PROCESSING".equals(taskStatus);
    }
    
    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(taskStatus) && videoUrl != null && !videoUrl.isEmpty();
    }
    
    /**
     * 是否失败
     */
    public boolean isFailed() {
        return "FAIL".equals(taskStatus);
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
        
        public TokenUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }
        
        public Integer getPromptTokens() {
            return promptTokens;
        }
        
        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }
        
        public Integer getCompletionTokens() {
            return completionTokens;
        }
        
        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }
        
        public Integer getTotalTokens() {
            return totalTokens;
        }
        
        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}
