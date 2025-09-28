package com.nexusvoice.domain.image.constant;

/**
 * 图像生成模型枚举
 * 
 * @author NexusVoice Team
 * @since 2025-09-28
 */
public enum ImageModelEnum {
    
    /**
     * Qwen图像编辑模型 2509版本
     */
    QWEN_IMAGE_EDIT_2509("Qwen/Qwen-Image-Edit-2509", "Qwen图像编辑模型2509"),
    
    /**
     * Qwen图像编辑模型
     */
    QWEN_IMAGE_EDIT("Qwen/Qwen-Image-Edit", "Qwen图像编辑模型"),
    
    /**
     * Qwen图像生成模型
     */
    QWEN_IMAGE("Qwen/Qwen-Image", "Qwen图像生成模型"),
    
    /**
     * Kolors图像生成模型
     */
    KWAI_KOLORS("Kwai-Kolors/Kolors", "Kolors图像生成模型");
    
    /**
     * 模型标识符
     */
    private final String modelName;
    
    /**
     * 模型描述
     */
    private final String description;
    
    ImageModelEnum(String modelName, String description) {
        this.modelName = modelName;
        this.description = description;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据模型名称获取枚举
     * 
     * @param modelName 模型名称
     * @return 模型枚举
     */
    public static ImageModelEnum getByModelName(String modelName) {
        for (ImageModelEnum model : values()) {
            if (model.getModelName().equals(modelName)) {
                return model;
            }
        }
        throw new IllegalArgumentException("不支持的图像生成模型: " + modelName);
    }
    
    /**
     * 检查是否为Qwen图像模型
     * 
     * @return true如果是Qwen模型
     */
    public boolean isQwenModel() {
        return this.modelName.startsWith("Qwen/");
    }
    
    /**
     * 检查是否为Kolors模型
     * 
     * @return true如果是Kolors模型
     */
    public boolean isKolorsModel() {
        return this.modelName.startsWith("Kwai-Kolors/");
    }
    
    /**
     * 检查是否支持批量生成
     * 
     * @return true如果支持批量生成
     */
    public boolean supportsBatchGeneration() {
        return this.isKolorsModel();
    }
    
    /**
     * 检查是否支持引导比例参数
     * 
     * @return true如果支持引导比例参数
     */
    public boolean supportsGuidanceScale() {
        return this.isKolorsModel();
    }
    
    /**
     * 检查是否支持CFG参数
     * 
     * @return true如果支持CFG参数
     */
    public boolean supportsCFG() {
        return this.isQwenModel() && !this.equals(QWEN_IMAGE_EDIT_2509) && !this.equals(QWEN_IMAGE_EDIT);
    }
}
