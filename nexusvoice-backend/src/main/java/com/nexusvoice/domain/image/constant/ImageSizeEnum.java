package com.nexusvoice.domain.image.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 图像尺寸枚举
 * 
 * @author NexusVoice Team
 * @since 2025-09-28
 */
public enum ImageSizeEnum {
    
    // Kolors模型推荐尺寸
    KOLORS_1024x1024("1024x1024", "1:1", 1024, 1024, "正方形"),
    KOLORS_960x1280("960x1280", "3:4", 960, 1280, "竖屏3:4"),
    KOLORS_768x1024("768x1024", "3:4", 768, 1024, "竖屏3:4小尺寸"),
    KOLORS_720x1440("720x1440", "1:2", 720, 1440, "长竖屏1:2"),
    KOLORS_720x1280("720x1280", "9:16", 720, 1280, "手机屏幕9:16"),
    
    // Qwen-Image模型推荐尺寸
    QWEN_1328x1328("1328x1328", "1:1", 1328, 1328, "正方形大尺寸"),
    QWEN_1664x928("1664x928", "16:9", 1664, 928, "宽屏16:9"),
    QWEN_928x1664("928x1664", "9:16", 928, 1664, "竖屏9:16"),
    QWEN_1472x1140("1472x1140", "4:3", 1472, 1140, "标准4:3"),
    QWEN_1140x1472("1140x1472", "3:4", 1140, 1472, "竖屏3:4"),
    QWEN_1584x1056("1584x1056", "3:2", 1584, 1056, "相机比例3:2"),
    QWEN_1056x1584("1056x1584", "2:3", 1056, 1584, "竖屏相机比例2:3");
    
    /**
     * 尺寸字符串（宽x高格式）
     */
    private final String size;
    
    /**
     * 宽高比例
     */
    private final String aspectRatio;
    
    /**
     * 宽度像素
     */
    private final Integer width;
    
    /**
     * 高度像素
     */
    private final Integer height;
    
    /**
     * 描述
     */
    private final String description;
    
    ImageSizeEnum(String size, String aspectRatio, Integer width, Integer height, String description) {
        this.size = size;
        this.aspectRatio = aspectRatio;
        this.width = width;
        this.height = height;
        this.description = description;
    }
    
    public String getSize() {
        return size;
    }
    
    public String getAspectRatio() {
        return aspectRatio;
    }
    
    public Integer getWidth() {
        return width;
    }
    
    public Integer getHeight() {
        return height;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据尺寸字符串获取枚举
     * 
     * @param size 尺寸字符串
     * @return 尺寸枚举
     */
    public static ImageSizeEnum getBySize(String size) {
        for (ImageSizeEnum imageSize : values()) {
            if (imageSize.getSize().equals(size)) {
                return imageSize;
            }
        }
        throw new IllegalArgumentException("不支持的图像尺寸: " + size);
    }
    
    /**
     * 获取Kolors模型支持的尺寸
     * 
     * @return Kolors支持的尺寸列表
     */
    public static List<ImageSizeEnum> getKolorsSupportedSizes() {
        return Arrays.asList(
            KOLORS_1024x1024,
            KOLORS_960x1280,
            KOLORS_768x1024,
            KOLORS_720x1440,
            KOLORS_720x1280
        );
    }
    
    /**
     * 获取Qwen模型支持的尺寸
     * 
     * @return Qwen支持的尺寸列表
     */
    public static List<ImageSizeEnum> getQwenSupportedSizes() {
        return Arrays.asList(
            QWEN_1328x1328,
            QWEN_1664x928,
            QWEN_928x1664,
            QWEN_1472x1140,
            QWEN_1140x1472,
            QWEN_1584x1056,
            QWEN_1056x1584
        );
    }
    
    /**
     * 检查尺寸是否适用于指定模型
     * 
     * @param model 图像模型
     * @return true如果适用
     */
    public boolean isSupportedByModel(ImageModelEnum model) {
        if (model.isKolorsModel()) {
            return getKolorsSupportedSizes().contains(this);
        } else if (model.isQwenModel()) {
            return getQwenSupportedSizes().contains(this);
        }
        return false;
    }
    
    /**
     * 获取模型的默认尺寸
     * 
     * @param model 图像模型
     * @return 默认尺寸
     */
    public static ImageSizeEnum getDefaultSizeForModel(ImageModelEnum model) {
        if (model.isKolorsModel()) {
            return KOLORS_1024x1024;
        } else if (model.isQwenModel()) {
            return QWEN_1328x1328;
        }
        return KOLORS_1024x1024; // 默认尺寸
    }
}
