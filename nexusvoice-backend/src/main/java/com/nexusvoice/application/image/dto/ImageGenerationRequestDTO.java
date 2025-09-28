package com.nexusvoice.application.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 图像生成请求DTO
 * 
 * @author NexusVoice Team
 * @since 2025-09-28
 */
@Data
@Schema(description = "AI图像生成请求")
public class ImageGenerationRequestDTO {

    @Schema(description = "图像生成模型", 
            example = "Qwen/Qwen-Image", 
            allowableValues = {"Qwen/Qwen-Image-Edit-2509", "Qwen/Qwen-Image-Edit", "Qwen/Qwen-Image", "Kwai-Kolors/Kolors"},
            required = true)
    @NotBlank(message = "图像生成模型不能为空")
    private String model;

    @Schema(description = "图像描述提示词", 
            example = "一只可爱的小猫坐在阳光明媚的窗台上，背景是绿色的植物",
            required = true)
    @NotBlank(message = "图像描述提示词不能为空")
    @Size(max = 2000, message = "提示词长度不能超过2000字符")
    private String prompt;

    @Schema(description = "负向提示词（描述不希望出现的内容）", 
            example = "模糊、低质量、变形")
    @Size(max = 1000, message = "负向提示词长度不能超过1000字符")
    private String negativePrompt;

    @Schema(description = "图像尺寸", 
            example = "1024x1024",
            allowableValues = {"1024x1024", "960x1280", "768x1024", "720x1440", "720x1280", 
                             "1328x1328", "1664x928", "928x1664", "1472x1140", "1140x1472", 
                             "1584x1056", "1056x1584"})
    private String imageSize;

    @Schema(description = "批量生成数量（仅Kolors模型支持）", 
            example = "1", 
            minimum = "1", 
            maximum = "4")
    @Min(value = 1, message = "批量生成数量不能小于1")
    @Max(value = 4, message = "批量生成数量不能大于4")
    private Integer batchSize;

    @Schema(description = "随机种子，用于可重现的生成结果", 
            example = "12345",
            minimum = "0",
            maximum = "9999999999")
    @Min(value = 0, message = "随机种子不能小于0")
    @Max(value = 9999999999L, message = "随机种子不能大于9999999999")
    private Long seed;

    @Schema(description = "推理步数，影响图像质量和生成时间", 
            example = "20",
            minimum = "1",
            maximum = "100")
    @Min(value = 1, message = "推理步数不能小于1")
    @Max(value = 100, message = "推理步数不能大于100")
    private Integer numInferenceSteps;

    @Schema(description = "引导比例，控制生成图像与提示词的匹配程度（仅Kolors模型支持）", 
            example = "7.5",
            minimum = "0",
            maximum = "20")
    @DecimalMin(value = "0.0", message = "引导比例不能小于0")
    @DecimalMax(value = "20.0", message = "引导比例不能大于20")
    private Double guidanceScale;

    @Schema(description = "CFG参数，调节生成精度和创意性（仅Qwen-Image模型支持）", 
            example = "4.0",
            minimum = "0.1",
            maximum = "20")
    @DecimalMin(value = "0.1", message = "CFG参数不能小于0.1")
    @DecimalMax(value = "20.0", message = "CFG参数不能大于20")
    private Double cfg;

    @Schema(description = "输入图像（用于图像编辑，可以是base64格式或URL）",
            example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...")
    private String inputImage;

    @Schema(description = "第二张输入图像（仅Qwen-Image-Edit-2509支持）")
    private String inputImage2;

    @Schema(description = "第三张输入图像（仅Qwen-Image-Edit-2509支持）")
    private String inputImage3;
}
