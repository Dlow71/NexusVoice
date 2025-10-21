package com.nexusvoice.infrastructure.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 向量化响应
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse {
    
    /**
     * 向量数据（单个）
     */
    private List<Float> vector;
    
    /**
     * 向量数据列表（批量）
     */
    private List<List<Float>> vectors;
    
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
     * 创建成功响应（单个向量）
     */
    public static EmbeddingResponse successSingle(List<Float> vector, String model, Integer tokens, Long duration) {
        return EmbeddingResponse.builder()
                .vector(vector)
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
     * 创建成功响应（批量向量）
     */
    public static EmbeddingResponse successBatch(List<List<Float>> vectors, String model, Integer tokens, Long duration) {
        return EmbeddingResponse.builder()
                .vectors(vectors)
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
    public static EmbeddingResponse error(String errorMessage) {
        return EmbeddingResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
