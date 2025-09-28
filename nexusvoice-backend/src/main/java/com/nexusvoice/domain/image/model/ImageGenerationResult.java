package com.nexusvoice.domain.image.model;

import java.util.List;

/**
 * 图像生成结果领域模型
 * 
 * @author NexusVoice Team
 * @since 2025-09-28
 */
public class ImageGenerationResult {
    
    /**
     * 生成的图像URL列表
     */
    private List<String> imageUrls;
    
    /**
     * 使用的随机种子
     */
    private Long usedSeed;
    
    /**
     * 图像生成耗时（毫秒）
     */
    private Long generationTime;
    
    /**
     * 图像生成服务商提供的原始响应（用于调试）
     */
    private String rawResponse;
    
    /**
     * 生成的图像数量
     */
    private Integer imageCount;
    
    /**
     * 图像尺寸信息
     */
    private String imageSize;
    
    /**
     * 使用的模型名称
     */
    private String modelName;
    
    public ImageGenerationResult() {
    }
    
    public ImageGenerationResult(List<String> imageUrls) {
        this.imageUrls = imageUrls;
        this.imageCount = imageUrls != null ? imageUrls.size() : 0;
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }
    
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
        this.imageCount = imageUrls != null ? imageUrls.size() : 0;
    }
    
    public Long getUsedSeed() {
        return usedSeed;
    }
    
    public void setUsedSeed(Long usedSeed) {
        this.usedSeed = usedSeed;
    }
    
    public Long getGenerationTime() {
        return generationTime;
    }
    
    public void setGenerationTime(Long generationTime) {
        this.generationTime = generationTime;
    }
    
    public String getRawResponse() {
        return rawResponse;
    }
    
    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
    
    public Integer getImageCount() {
        return imageCount;
    }
    
    public void setImageCount(Integer imageCount) {
        this.imageCount = imageCount;
    }
    
    public String getImageSize() {
        return imageSize;
    }
    
    public void setImageSize(String imageSize) {
        this.imageSize = imageSize;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    /**
     * 检查是否生成成功
     * 
     * @return true如果有生成的图像
     */
    public boolean isSuccess() {
        return imageUrls != null && !imageUrls.isEmpty();
    }
    
    /**
     * 获取第一张图像的URL
     * 
     * @return 第一张图像URL，如果没有图像返回null
     */
    public String getFirstImageUrl() {
        return isSuccess() ? imageUrls.get(0) : null;
    }
    
    /**
     * 获取生成结果的摘要信息
     * 
     * @return 摘要字符串
     */
    public String getSummary() {
        return String.format("生成了%d张图像，耗时%dms，使用模型:%s，种子:%d", 
            imageCount, generationTime, modelName, usedSeed);
    }
}
