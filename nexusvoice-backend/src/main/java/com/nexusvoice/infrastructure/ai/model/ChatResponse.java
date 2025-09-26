package com.nexusvoice.infrastructure.ai.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI聊天响应模型
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {

    /**
     * 响应ID
     */
    private String id;

    /**
     * 响应内容
     */
    private String content;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 完成原因
     * stop: 正常完成
     * length: 达到最大长度限制
     * content_filter: 内容被过滤
     */
    private String finishReason;

    /**
     * 令牌使用统计
     */
    private TokenUsage usage;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTimeMs;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 额外的元数据
     */
    private Map<String, Object> metadata;

    /**
     * 令牌使用统计
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenUsage {
        /**
         * 输入令牌数
         */
        private Integer promptTokens;

        /**
         * 输出令牌数
         */
        private Integer completionTokens;

        /**
         * 总令牌数
         */
        private Integer totalTokens;
    }

    /**
     * 创建成功响应
     */
    public static ChatResponse success(String content, String model, TokenUsage usage, Long responseTime) {
        return ChatResponse.builder()
                .content(content)
                .model(model)
                .finishReason("stop")
                .usage(usage)
                .responseTimeMs(responseTime)
                .success(true)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static ChatResponse error(String errorMessage) {
        return ChatResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
