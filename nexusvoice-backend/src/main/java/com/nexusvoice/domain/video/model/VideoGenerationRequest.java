package com.nexusvoice.domain.video.model;

import com.nexusvoice.domain.video.constant.VideoQualityEnum;
import com.nexusvoice.domain.video.constant.VideoSizeEnum;

import java.util.List;

/**
 * 视频生成请求领域模型
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
public class VideoGenerationRequest {
    
    /**
     * 模型键（格式：provider:model，如 zhipu:cogvideox-flash）
     */
    private String modelKey;
    
    /**
     * 视频的文本描述
     */
    private String prompt;
    
    /**
     * 输出模式（quality质量优先、speed速度优先）
     */
    private VideoQualityEnum quality;
    
    /**
     * 是否生成AI音效
     */
    private Boolean withAudio;
    
    /**
     * 是否添加水印
     */
    private Boolean watermarkEnabled;
    
    /**
     * 基础图像URL（图生视频）
     */
    private String imageUrl;
    
    /**
     * 基础图像Base64（图生视频）
     */
    private String imageBase64;
    
    /**
     * 首尾帧图像URL列表（首尾帧生成模式）
     */
    private List<String> imageUrls;
    
    /**
     * 视频尺寸
     */
    private VideoSizeEnum size;
    
    /**
     * 视频帧率（FPS），30或60
     */
    private Integer fps;
    
    /**
     * 视频持续时长（秒），5或10
     */
    private Integer duration;
    
    /**
     * 请求ID（由客户端提供）
     */
    private String requestId;
    
    /**
     * 用户ID（用于费用统计和调用日志）
     */
    private Long userId;
    
    /**
     * 会话ID（用于关联调用日志）
     */
    private Long conversationId;
    
    public VideoGenerationRequest() {
        // 设置默认值
        this.quality = VideoQualityEnum.SPEED;
        this.withAudio = false;
        this.watermarkEnabled = true;
        this.fps = 30;
        this.duration = 5;
    }
    
    public VideoGenerationRequest(String modelKey, String prompt) {
        this();
        this.modelKey = modelKey;
        this.prompt = prompt;
    }
    
    // Getters and Setters
    
    public String getModelKey() {
        return modelKey;
    }
    
    public void setModelKey(String modelKey) {
        this.modelKey = modelKey;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public VideoQualityEnum getQuality() {
        return quality;
    }
    
    public void setQuality(VideoQualityEnum quality) {
        this.quality = quality;
    }
    
    public Boolean getWithAudio() {
        return withAudio;
    }
    
    public void setWithAudio(Boolean withAudio) {
        this.withAudio = withAudio;
    }
    
    public Boolean getWatermarkEnabled() {
        return watermarkEnabled;
    }
    
    public void setWatermarkEnabled(Boolean watermarkEnabled) {
        this.watermarkEnabled = watermarkEnabled;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getImageBase64() {
        return imageBase64;
    }
    
    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }
    
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    
    public VideoSizeEnum getSize() {
        return size;
    }
    
    public void setSize(VideoSizeEnum size) {
        this.size = size;
    }
    
    public Integer getFps() {
        return fps;
    }
    
    public void setFps(Integer fps) {
        this.fps = fps;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
    
    /**
     * 验证请求参数的有效性
     * 
     * @return 验证结果消息，null表示验证通过
     */
    public String validate() {
        if (modelKey == null || modelKey.trim().isEmpty()) {
            return "视频生成模型不能为空";
        }
        
        // prompt和imageUrl/imageBase64至少有一个
        if ((prompt == null || prompt.trim().isEmpty()) && 
            (imageUrl == null || imageUrl.trim().isEmpty()) &&
            (imageBase64 == null || imageBase64.trim().isEmpty()) &&
            (imageUrls == null || imageUrls.isEmpty())) {
            return "文本描述或图像至少需要提供一个";
        }
        
        // 首尾帧模式只支持speed质量
        if (imageUrls != null && imageUrls.size() == 2) {
            if (quality != VideoQualityEnum.SPEED) {
                return "首尾帧模式只支持speed质量模式";
            }
        }
        
        // FPS验证
        if (fps != null && fps != 30 && fps != 60) {
            return "FPS必须为30或60";
        }
        
        // 持续时长验证
        if (duration != null && duration != 5 && duration != 10) {
            return "持续时长必须为5或10秒";
        }
        
        return null; // 验证通过
    }
    
    /**
     * 是否为文生视频模式
     */
    public boolean isTextToVideo() {
        return (prompt != null && !prompt.trim().isEmpty()) &&
               (imageUrl == null || imageUrl.trim().isEmpty()) &&
               (imageBase64 == null || imageBase64.trim().isEmpty()) &&
               (imageUrls == null || imageUrls.isEmpty());
    }
    
    /**
     * 是否为图生视频模式
     */
    public boolean isImageToVideo() {
        return (imageUrl != null && !imageUrl.trim().isEmpty()) ||
               (imageBase64 != null && !imageBase64.trim().isEmpty());
    }
    
    /**
     * 是否为首尾帧模式
     */
    public boolean isFirstLastFrameMode() {
        return imageUrls != null && imageUrls.size() == 2;
    }
}
