package com.nexusvoice.domain.mq.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息领域模型
 * 
 * @author Dlow
 * @date 2025/10/18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message<T> implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消息主题
     */
    private String topic;
    
    /**
     * 消息标签
     */
    private String tag;
    
    /**
     * 消息键（用于顺序消息）
     */
    private String key;
    
    /**
     * 消息体
     */
    private T payload;
    
    /**
     * 延迟级别（1-18级，对应不同延迟时间）
     */
    private Integer delayLevel;
    
    /**
     * 消息属性
     */
    private Map<String, String> properties;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 重试次数
     */
    private Integer retryTimes;
    
    /**
     * 消息追踪ID（用于链路追踪）
     */
    private String traceId;
    
    /**
     * 消息来源
     */
    private String source;
    
    /**
     * 创建即时消息
     */
    public static <T> Message<T> createNormalMessage(String topic, T payload) {
        return Message.<T>builder()
                .topic(topic)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建延迟消息
     */
    public static <T> Message<T> createDelayMessage(String topic, T payload, Integer delayLevel) {
        return Message.<T>builder()
                .topic(topic)
                .payload(payload)
                .delayLevel(delayLevel)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建顺序消息
     */
    public static <T> Message<T> createOrderlyMessage(String topic, T payload, String key) {
        return Message.<T>builder()
                .topic(topic)
                .payload(payload)
                .key(key)
                .createTime(LocalDateTime.now())
                .build();
    }
}
