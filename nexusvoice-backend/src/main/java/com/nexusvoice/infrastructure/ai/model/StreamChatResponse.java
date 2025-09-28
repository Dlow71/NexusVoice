package com.nexusvoice.infrastructure.ai.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 流式聊天响应模型
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StreamChatResponse {

    /**
     * 响应ID
     */
    private String id;

    /**
     * 增量内容
     */
    private String delta;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 完成原因（仅在最后一个片段中存在）
     */
    private String finishReason;

    /**
     * 是否为流的结束
     */
    private Boolean isEnd;

    /**
     * 当前片段的索引
     */
    private Integer index;

    /**
     * 消息类型
     */
    private StreamMessageType type;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    /**
     * 对话ID（仅在END时返回）
     */
    private Long conversationId;

    /**
     * 消息ID（仅在END时返回）
     */
    private Long messageId;

    /**
     * AI回复语音地址（仅在END时返回且启用TTS时存在）
     */
    private String audioUrl;

    /**
     * 分段TTS的组ID（当以分段形式返回时）
     */
    private String ttsGroupId;

    /**
     * 是否为分段TTS消息
     */
    private Boolean ttsChunked;

    /**
     * 响应时间（毫秒，仅在END时可选返回）
     */
    private Long responseTimeMs;

    /**
     * 流消息类型
     */
    public enum StreamMessageType {
        /**
         * 内容片段
         */
        CONTENT,
        
        /**
         * 开始信号
         */
        START,
        
        /**
         * 结束信号
         */
        END,
        
        /**
         * 错误信号
         */
        ERROR,
        
        /**
         * 心跳信号
         */
        HEARTBEAT
        ,
        /**
         * 分段TTS消息（携带该段文本与对应音频地址）
         */
        TTS_SEGMENT,
        
        /**
         * 分段TTS补发（音频迟到时对已有文本段进行音频更新）
         */
        TTS_SEGMENT_UPDATE
    }

    /**
     * 创建内容片段响应
     */
    public static StreamChatResponse content(String delta, Integer index) {
        return StreamChatResponse.builder()
                .delta(delta)
                .index(index)
                .type(StreamMessageType.CONTENT)
                .isEnd(false)
                .build();
    }

    /**
     * 创建开始响应
     */
    public static StreamChatResponse start(String id, String model) {
        return StreamChatResponse.builder()
                .id(id)
                .model(model)
                .type(StreamMessageType.START)
                .isEnd(false)
                .index(0)
                .build();
    }

    /**
     * 创建结束响应
     */
    public static StreamChatResponse end(String finishReason) {
        return StreamChatResponse.builder()
                .finishReason(finishReason)
                .type(StreamMessageType.END)
                .isEnd(true)
                .build();
    }

    /**
     * 创建错误响应
     */
    public static StreamChatResponse error(String errorMessage) {
        return StreamChatResponse.builder()
                .errorMessage(errorMessage)
                .type(StreamMessageType.ERROR)
                .isEnd(true)
                .build();
    }

    /**
     * 创建心跳响应
     */
    public static StreamChatResponse heartbeat() {
        return StreamChatResponse.builder()
                .type(StreamMessageType.HEARTBEAT)
                .isEnd(false)
                .build();
    }

    /**
     * 创建分段TTS响应
     */
    public static StreamChatResponse ttsSegment(String groupId, int index, String text, String audioUrl, String model) {
        StreamChatResponse resp = StreamChatResponse.builder()
                .type(StreamMessageType.TTS_SEGMENT)
                .isEnd(false)
                .index(index)
                .delta(text)
                .audioUrl(audioUrl)
                .model(model)
                .ttsGroupId(groupId)
                .ttsChunked(true)
                .build();
        return resp;
    }

    /**
     * 创建分段TTS补发（迟到音频）
     */
    public static StreamChatResponse ttsSegmentUpdate(String groupId, int index, String audioUrl, String model) {
        StreamChatResponse resp = StreamChatResponse.builder()
                .type(StreamMessageType.TTS_SEGMENT_UPDATE)
                .isEnd(false)
                .index(index)
                .audioUrl(audioUrl)
                .model(model)
                .ttsGroupId(groupId)
                .ttsChunked(true)
                .build();
        return resp;
    }
}
