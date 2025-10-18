package com.nexusvoice.infrastructure.mq.utils;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.nexusvoice.domain.mq.enums.DelayLevelEnum;
import com.nexusvoice.domain.mq.model.Message;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息工具类
 * 
 * @author Dlow
 * @date 2025/10/18
 */
public class MessageUtils {
    
    private static final String TRACE_ID_KEY = "traceId";
    
    /**
     * 构建普通消息
     */
    public static <T> Message<T> buildMessage(String topic, T payload) {
        return buildMessage(topic, null, payload);
    }
    
    /**
     * 构建带标签的消息
     */
    public static <T> Message<T> buildMessage(String topic, String tag, T payload) {
        return Message.<T>builder()
                .messageId(generateMessageId())
                .topic(topic)
                .tag(tag)
                .payload(payload)
                .traceId(getOrCreateTraceId())
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 构建延迟消息
     */
    public static <T> Message<T> buildDelayMessage(String topic, T payload, DelayLevelEnum delayLevel) {
        return buildDelayMessage(topic, null, payload, delayLevel.getLevel());
    }
    
    /**
     * 构建延迟消息（指定延迟级别）
     */
    public static <T> Message<T> buildDelayMessage(String topic, String tag, T payload, Integer delayLevel) {
        Message<T> message = buildMessage(topic, tag, payload);
        message.setDelayLevel(delayLevel);
        return message;
    }
    
    /**
     * 构建延迟消息（指定延迟秒数）
     */
    public static <T> Message<T> buildDelayMessageBySeconds(String topic, T payload, int seconds) {
        DelayLevelEnum delayLevel = DelayLevelEnum.getBySeconds(seconds);
        return buildDelayMessage(topic, payload, delayLevel);
    }
    
    /**
     * 构建延迟消息（指定延迟分钟数）
     */
    public static <T> Message<T> buildDelayMessageByMinutes(String topic, T payload, int minutes) {
        DelayLevelEnum delayLevel = DelayLevelEnum.getByMinutes(minutes);
        return buildDelayMessage(topic, payload, delayLevel);
    }
    
    /**
     * 构建顺序消息
     */
    public static <T> Message<T> buildOrderlyMessage(String topic, T payload, String orderKey) {
        Message<T> message = buildMessage(topic, payload);
        message.setKey(orderKey);
        return message;
    }
    
    /**
     * 构建带属性的消息
     */
    public static <T> Message<T> buildMessageWithProperties(String topic, T payload, Map<String, String> properties) {
        Message<T> message = buildMessage(topic, payload);
        message.setProperties(properties);
        return message;
    }
    
    /**
     * 添加消息属性
     */
    public static <T> void addProperty(Message<T> message, String key, String value) {
        if (message.getProperties() == null) {
            message.setProperties(new HashMap<>());
        }
        message.getProperties().put(key, value);
    }
    
    /**
     * 生成消息ID
     */
    public static String generateMessageId() {
        return IdUtil.fastSimpleUUID();
    }
    
    /**
     * 获取或创建追踪ID
     */
    public static String getOrCreateTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
        }
        return traceId;
    }
    
    /**
     * 计算重试延迟级别
     * 根据重试次数返回递增的延迟级别
     */
    public static Integer calculateRetryDelayLevel(int retryTimes) {
        // 第1次重试：10秒
        // 第2次重试：30秒
        // 第3次重试：1分钟
        // 第4次重试：2分钟
        // 第5次及以上：5分钟
        return switch (retryTimes) {
            case 1 -> DelayLevelEnum.DELAY_10S.getLevel();
            case 2 -> DelayLevelEnum.DELAY_30S.getLevel();
            case 3 -> DelayLevelEnum.DELAY_1M.getLevel();
            case 4 -> DelayLevelEnum.DELAY_2M.getLevel();
            default -> DelayLevelEnum.DELAY_5M.getLevel();
        };
    }
    
    /**
     * 验证消息是否有效
     */
    public static <T> boolean isValidMessage(Message<T> message) {
        return message != null 
            && StrUtil.isNotBlank(message.getTopic())
            && message.getPayload() != null;
    }
    
    /**
     * 复制消息（用于重试）
     */
    public static <T> Message<T> copyMessage(Message<T> original) {
        return Message.<T>builder()
                .messageId(generateMessageId())
                .topic(original.getTopic())
                .tag(original.getTag())
                .key(original.getKey())
                .payload(original.getPayload())
                .delayLevel(original.getDelayLevel())
                .properties(original.getProperties() != null ? new HashMap<>(original.getProperties()) : null)
                .createTime(LocalDateTime.now())
                .retryTimes(original.getRetryTimes())
                .traceId(original.getTraceId())
                .source(original.getSource())
                .build();
    }
}
