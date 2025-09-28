package com.nexusvoice.application.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 图像生成响应DTO
 * 
 * @author NexusVoice Team
 * @since 2025-09-28
 */
@Data
@Schema(description = "AI图像生成响应")
public class ImageGenerationResponseDTO {

    @Schema(description = "生成的图像URL列表（七牛云CDN地址）", 
            example = "[\"https://cdn.nexusvoice.com/images/generated_image_1695552000000.png\"]")
    private List<String> imageUrls;

    @Schema(description = "原始提示词", 
            example = "一只可爱的小猫坐在阳光明媚的窗台上，背景是绿色的植物")
    private String prompt;

    @Schema(description = "负向提示词", 
            example = "模糊、低质量、变形")
    private String negativePrompt;

    @Schema(description = "使用的模型", 
            example = "Qwen/Qwen-Image")
    private String model;

    @Schema(description = "图像尺寸", 
            example = "1024x1024")
    private String imageSize;

    @Schema(description = "生成的图像数量", 
            example = "1")
    private Integer imageCount;

    @Schema(description = "使用的随机种子", 
            example = "12345")
    private Long usedSeed;

    @Schema(description = "推理步数", 
            example = "20")
    private Integer numInferenceSteps;

    @Schema(description = "引导比例（如果适用）", 
            example = "7.5")
    private Double guidanceScale;

    @Schema(description = "CFG参数（如果适用）", 
            example = "4.0")
    private Double cfg;

    @Schema(description = "图像生成耗时（毫秒）", 
            example = "3500")
    private Long generationTime;

    @Schema(description = "图像详细信息列表")
    private List<ImageInfo> imageInfos;

    @Data
    @Schema(description = "单个图像信息")
    public static class ImageInfo {
        
        @Schema(description = "图像URL（七牛云CDN地址）", 
                example = "https://cdn.nexusvoice.com/images/generated_image_1695552000000.png")
        private String url;

        @Schema(description = "图像文件大小（字节）", 
                example = "245760")
        private Long fileSize;

        @Schema(description = "图像文件名", 
                example = "generated_image_1695552000000.png")
        private String fileName;

        @Schema(description = "图像宽度（像素）", 
                example = "1024")
        private Integer width;

        @Schema(description = "图像高度（像素）", 
                example = "1024")
        private Integer height;

        @Schema(description = "图像格式", 
                example = "PNG")
        private String format;

        @Schema(description = "该图像的生成序号（批量生成时）", 
                example = "0")
        private Integer index;
    }

    /**
     * 获取第一张图像的URL
     * 
     * @return 第一张图像URL，如果没有图像返回null
     */
    public String getFirstImageUrl() {
        return imageUrls != null && !imageUrls.isEmpty() ? imageUrls.get(0) : null;
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
     * 获取生成结果的摘要信息
     * 
     * @return 摘要字符串
     */
    public String getSummary() {
        return String.format("使用模型%s生成了%d张%s尺寸的图像，耗时%dms", 
            model, imageCount, imageSize, generationTime);
    }
}
