package com.nexusvoice.application.developer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 开发者API密钥统计信息DTO
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Data
@Schema(description = "开发者API密钥统计信息")
public class ApiKeyStatisticsDTO {
    
    @Schema(description = "总密钥数量", example = "5")
    private Long totalKeys;
    
    @Schema(description = "正常状态密钥数量", example = "4")
    private Long activeKeys;
    
    @Schema(description = "禁用状态密钥数量", example = "1")
    private Long disabledKeys;
    
    @Schema(description = "已过期密钥数量", example = "0")
    private Long expiredKeys;
    
    @Schema(description = "总请求次数", example = "100000")
    private String totalRequests;
    
    @Schema(description = "今日总请求次数", example = "5000")
    private Long todayRequests;
    
    @Schema(description = "总Token使用量", example = "10000000")
    private String totalTokens;
    
    @Schema(description = "今日Token使用量", example = "50000")
    private String todayTokens;
    
    @Schema(description = "总费用（元）", example = "1234.56")
    private BigDecimal totalCost;
    
    @Schema(description = "今日费用（元）", example = "12.34")
    private BigDecimal todayCost;
}
