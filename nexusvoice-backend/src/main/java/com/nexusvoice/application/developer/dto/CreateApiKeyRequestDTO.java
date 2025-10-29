package com.nexusvoice.application.developer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建开发者API密钥请求DTO
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Data
@Schema(description = "创建开发者API密钥请求")
public class CreateApiKeyRequestDTO {
    
    @Schema(description = "密钥名称（便于识别）", 
            example = "我的聊天应用",
            required = true)
    @NotBlank(message = "密钥名称不能为空")
    @Size(max = 100, message = "密钥名称长度不能超过100字符")
    private String keyName;
    
    @Schema(description = "应用名称（可选）", 
            example = "智能客服系统")
    @Size(max = 200, message = "应用名称长度不能超过200字符")
    private String appName;
    
    @Schema(description = "权限范围列表（chat-聊天, image-图像, tts-语音合成, asr-语音识别, embedding-向量化, rerank-重排序, video-视频, file-文件, all-全部）",
            example = "[\"chat\", \"image\", \"tts\"]")
    @NotEmpty(message = "权限范围不能为空")
    @Size(max = 10, message = "权限范围数量不能超过10个")
    private List<String> scopes;
    
    @Schema(description = "允许访问的模型列表（如[\"openai:gpt-4\", \"doubao:seed-1.6\"]，为空表示不限制）",
            example = "[\"openai:gpt-oss-20b\", \"doubao:doubao-seed-1.6\"]")
    @Size(max = 50, message = "允许的模型数量不能超过50个")
    private List<String> allowedModels;
    
    @Schema(description = "IP白名单（CIDR格式，如[\"192.168.1.1\", \"10.0.0.0/24\"]，为空表示不限制）",
            example = "[\"192.168.1.100\"]")
    @Size(max = 100, message = "IP白名单数量不能超过100个")
    private List<String> ipWhitelist;
    
    @Schema(description = "每分钟请求次数限制（null表示不限制）",
            example = "60",
            minimum = "1",
            maximum = "10000")
    @Min(value = 1, message = "每分钟请求次数限制不能小于1")
    @Max(value = 10000, message = "每分钟请求次数限制不能大于10000")
    private Integer rateLimitPerMinute;
    
    @Schema(description = "每日请求次数限制（null表示不限制）",
            example = "10000",
            minimum = "1",
            maximum = "1000000")
    @Min(value = 1, message = "每日请求次数限制不能小于1")
    @Max(value = 1000000, message = "每日请求次数限制不能大于1000000")
    private Integer rateLimitPerDay;
    
    @Schema(description = "每日费用限额（元，null表示不限制）",
            example = "100.00",
            minimum = "0.01",
            maximum = "100000")
    @DecimalMin(value = "0.01", message = "每日费用限额不能小于0.01元")
    @DecimalMax(value = "100000.00", message = "每日费用限额不能大于100000元")
    private BigDecimal dailyCostLimit;
    
    @Schema(description = "每月费用限额（元，null表示不限制）",
            example = "1000.00",
            minimum = "0.01",
            maximum = "1000000")
    @DecimalMin(value = "0.01", message = "每月费用限额不能小于0.01元")
    @DecimalMax(value = "1000000.00", message = "每月费用限额不能大于1000000元")
    private BigDecimal monthlyCostLimit;
    
    @Schema(description = "每日Token限额（null表示不限制）",
            example = "1000000",
            minimum = "1000",
            maximum = "100000000")
    @Min(value = 1000, message = "每日Token限额不能小于1000")
    @Max(value = 100000000, message = "每日Token限额不能大于100000000")
    private Long dailyTokenLimit;
    
    @Schema(description = "每月Token限额（null表示不限制）",
            example = "10000000",
            minimum = "1000",
            maximum = "1000000000")
    @Min(value = 1000, message = "每月Token限额不能小于1000")
    @Max(value = 1000000000, message = "每月Token限额不能大于1000000000")
    private Long monthlyTokenLimit;
    
    @Schema(description = "过期时间（ISO 8601格式，null表示永不过期）",
            example = "2025-12-31T23:59:59")
    private LocalDateTime expireAt;
}
