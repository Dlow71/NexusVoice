package com.nexusvoice.infrastructure.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 重排序响应
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RerankResponse {
    
    /**
     * 排序后的结果列表
     */
    private List<RerankResult> results;
    
    /**
     * 使用的模型标识
     */
    private String model;
    
    /**
     * token使用统计
     */
    private TokenUsage tokenUsage;
    
    /**
     * 处理耗时（毫秒）
     */
    private Long duration;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 重排序结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RerankResult {
        /**
         * 原始索引位置
         */
        private Integer index;
        
        /**
         * 文档内容
         */
        private String document;
        
        /**
         * 相关性分数（0-1之间，越大越相关）
         */
        private Double score;
        
        /**
         * 排序后的位置（从0开始）
         */
        private Integer rank;
    }
    
    /**
     * Token使用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        /**
         * 输入tokens
         */
        private Integer promptTokens;
        
        /**
         * 总tokens
         */
        private Integer totalTokens;
    }
    
    /**
     * 创建成功响应
     */
    public static RerankResponse success(List<RerankResult> results, String model, Integer tokens, Long duration) {
        return RerankResponse.builder()
                .results(results)
                .model(model)
                .tokenUsage(TokenUsage.builder()
                        .promptTokens(tokens)
                        .totalTokens(tokens)
                        .build())
                .duration(duration)
                .success(true)
                .build();
    }
    
    /**
     * 创建失败响应
     */
    public static RerankResponse error(String errorMessage) {
        return RerankResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
