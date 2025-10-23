package com.nexusvoice.domain.image.model;

import com.nexusvoice.domain.image.constant.ImageSizeEnum;

/**
 * 图像生成请求领域模型
 * 
 * @author NexusVoice Team
 * @since 2025-09-28
 */
public class ImageGenerationRequest {
    
    /**
     * 模型键（格式：provider:model，如 siliconflow:kolors）
     */
    private String modelKey;
    
    /**
     * 正向提示词
     */
    private String prompt;
    
    /**
     * 负向提示词
     */
    private String negativePrompt;
    
    /**
     * 图像尺寸
     */
    private ImageSizeEnum imageSize;
    
    /**
     * 批量大小（仅Kolors模型支持）
     */
    private Integer batchSize;
    
    /**
     * 随机种子
     */
    private Long seed;
    
    /**
     * 推理步数
     */
    private Integer numInferenceSteps;
    
    /**
     * 引导比例（仅Kolors模型支持）
     */
    private Double guidanceScale;
    
    /**
     * CFG参数（仅Qwen-Image模型支持）
     */
    private Double cfg;
    
    /**
     * 输入图像（用于图像编辑）
     */
    private String inputImage;
    
    /**
     * 第二张输入图像（仅Qwen-Image-Edit-2509支持）
     */
    private String inputImage2;
    
    /**
     * 第三张输入图像（仅Qwen-Image-Edit-2509支持）
     */
    private String inputImage3;
    
    /**
     * 用户ID（用于费用统计和调用日志）
     */
    private Long userId;
    
    public ImageGenerationRequest() {
    }
    
    public ImageGenerationRequest(String modelKey, String prompt) {
        this.modelKey = modelKey;
        this.prompt = prompt;
        // 设置默认值
        this.imageSize = ImageSizeEnum.KOLORS_1024x1024; // 默认尺寸
        this.batchSize = 1;
        this.numInferenceSteps = 20;
        this.guidanceScale = 7.5;
        this.cfg = 4.0;
    }
    
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
    
    public String getNegativePrompt() {
        return negativePrompt;
    }
    
    public void setNegativePrompt(String negativePrompt) {
        this.negativePrompt = negativePrompt;
    }
    
    public ImageSizeEnum getImageSize() {
        return imageSize;
    }
    
    public void setImageSize(ImageSizeEnum imageSize) {
        this.imageSize = imageSize;
    }
    
    public Integer getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }
    
    public Long getSeed() {
        return seed;
    }
    
    public void setSeed(Long seed) {
        this.seed = seed;
    }
    
    public Integer getNumInferenceSteps() {
        return numInferenceSteps;
    }
    
    public void setNumInferenceSteps(Integer numInferenceSteps) {
        this.numInferenceSteps = numInferenceSteps;
    }
    
    public Double getGuidanceScale() {
        return guidanceScale;
    }
    
    public void setGuidanceScale(Double guidanceScale) {
        this.guidanceScale = guidanceScale;
    }
    
    public Double getCfg() {
        return cfg;
    }
    
    public void setCfg(Double cfg) {
        this.cfg = cfg;
    }
    
    public String getInputImage() {
        return inputImage;
    }
    
    public void setInputImage(String inputImage) {
        this.inputImage = inputImage;
    }
    
    public String getInputImage2() {
        return inputImage2;
    }
    
    public void setInputImage2(String inputImage2) {
        this.inputImage2 = inputImage2;
    }
    
    public String getInputImage3() {
        return inputImage3;
    }
    
    public void setInputImage3(String inputImage3) {
        this.inputImage3 = inputImage3;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    /**
     * 验证请求参数的有效性
     * 
     * @return 验证结果消息，null表示验证通过
     */
    public String validate() {
        if (modelKey == null || modelKey.trim().isEmpty()) {
            return "图像生成模型不能为空";
        }
        
        if (prompt == null || prompt.trim().isEmpty()) {
            return "图像描述提示词不能为空";
        }
        
        if (imageSize == null) {
            return "图像尺寸不能为空";
        }
        
        // 批量大小验证（简化验证，不再依赖枚举）
        if (batchSize != null) {
            if (batchSize < 1 || batchSize > 4) {
                return "批量大小必须在1-4之间";
            }
        }
        
        // 种子值验证
        if (seed != null && (seed < 0 || seed > 9999999999L)) {
            return "随机种子必须在0-9999999999之间";
        }
        
        // 推理步数验证
        if (numInferenceSteps != null && (numInferenceSteps < 1 || numInferenceSteps > 100)) {
            return "推理步数必须在1-100之间";
        }
        
        // 引导比例验证（简化，不再检查模型是否支持）
        if (guidanceScale != null) {
            if (guidanceScale < 0 || guidanceScale > 20) {
                return "引导比例必须在0-20之间";
            }
        }
        
        // CFG参数验证（简化，不再检查模型是否支持）
        if (cfg != null) {
            if (cfg < 0.1 || cfg > 20) {
                return "CFG参数必须在0.1-20之间";
            }
        }
        
        return null; // 验证通过
    }
}
