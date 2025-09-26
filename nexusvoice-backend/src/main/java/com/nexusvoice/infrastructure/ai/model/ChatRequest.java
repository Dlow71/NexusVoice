package com.nexusvoice.infrastructure.ai.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI聊天请求模型
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {

    /**
     * 消息列表
     */
    private List<ChatMessage> messages;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 温度参数 (0-2)
     * 控制回复的随机性，数值越高越随机
     */
    private Double temperature;

    /**
     * 最大令牌数
     */
    private Integer maxTokens;

    /**
     * Top-p采样参数
     */
    private Double topP;

    /**
     * 频率惩罚参数
     */
    private Double frequencyPenalty;

    /**
     * 存在惩罚参数
     */
    private Double presencePenalty;

    /**
     * 停止词列表
     */
    private List<String> stop;

    /**
     * 是否流式输出
     */
    private Boolean stream;

    /**
     * 用户ID（用于追踪和限流）
     */
    private Long userId;

    /**
     * 对话ID（用于上下文管理）
     */
    private Long conversationId;

    /**
     * 创建默认配置的请求
     */
    public static ChatRequest defaultRequest(List<ChatMessage> messages) {
        return ChatRequest.builder()
                .messages(messages)
                .model("gpt-4o-mini")
                .temperature(0.7)
                .maxTokens(2000)
                .topP(1.0)
                .frequencyPenalty(0.0)
                .presencePenalty(0.0)
                .stream(false)
                .build();
    }

    /**
     * 创建流式请求
     */
    public static ChatRequest streamRequest(List<ChatMessage> messages) {
        return ChatRequest.builder()
                .messages(messages)
                .model("gpt-4o-mini")
                .temperature(0.7)
                .maxTokens(2000)
                .topP(1.0)
                .frequencyPenalty(0.0)
                .presencePenalty(0.0)
                .stream(true)
                .build();
    }
}
