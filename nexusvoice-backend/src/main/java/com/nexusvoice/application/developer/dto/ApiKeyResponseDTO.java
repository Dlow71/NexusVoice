package com.nexusvoice.application.developer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 开发者API密钥响应DTO
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Data
@Schema(description = "开发者API密钥响应")
public class ApiKeyResponseDTO {
    
    @Schema(description = "密钥ID", example = "1234567890")
    private String id;
    
    @Schema(description = "密钥名称", example = "我的聊天应用")
    private String keyName;
    
    @Schema(description = "API Key前缀（显示用）", example = "sk-nv-abc123...")
    private String keyPrefix;
    
    @Schema(description = "应用名称", example = "智能客服系统")
    private String appName;
    
    @Schema(description = "权限范围列表", example = "[\"chat\", \"image\", \"tts\"]")
    private List<String> scopes;
    
    @Schema(description = "允许访问的模型列表", example = "[\"openai:gpt-oss-20b\"]")
    private List<String> allowedModels;
    
    @Schema(description = "IP白名单", example = "[\"192.168.1.100\"]")
    private List<String> ipWhitelist;
    
    @Schema(description = "每分钟请求次数限制", example = "60")
    private Integer rateLimitPerMinute;
    
    @Schema(description = "每日请求次数限制", example = "10000")
    private Integer rateLimitPerDay;
    
    @Schema(description = "总请求次数", example = "12345")
    private String totalRequestCount;
    
    @Schema(description = "今日请求次数", example = "567")
    private Integer todayRequestCount;
    
    @Schema(description = "最后请求时间", example = "2025-10-29 14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRequestAt;
    
    @Schema(description = "总输入Token数", example = "1000000")
    private String totalInputTokens;
    
    @Schema(description = "总输出Token数", example = "500000")
    private String totalOutputTokens;
    
    @Schema(description = "总Token数", example = "1500000")
    private String totalTokens;
    
    @Schema(description = "今日Token数", example = "5000")
    private String todayTokens;
    
    @Schema(description = "每日费用限额（元）", example = "100.00")
    private BigDecimal dailyCostLimit;
    
    @Schema(description = "每月费用限额（元）", example = "1000.00")
    private BigDecimal monthlyCostLimit;
    
    @Schema(description = "总费用（元）", example = "256.78")
    private BigDecimal totalCost;
    
    @Schema(description = "每日Token限额", example = "1000000")
    private String dailyTokenLimit;
    
    @Schema(description = "每月Token限额", example = "10000000")
    private String monthlyTokenLimit;
    
    @Schema(description = "状态（ACTIVE-正常, DISABLED-禁用, EXPIRED-过期）", example = "ACTIVE")
    private String status;
    
    @Schema(description = "过期时间", example = "2025-12-31 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireAt;
    
    @Schema(description = "最后使用的IP地址", example = "192.168.1.100")
    private String lastUsedIp;
    
    @Schema(description = "创建时间", example = "2025-10-29 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新时间", example = "2025-10-29 14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
