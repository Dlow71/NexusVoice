package com.nexusvoice.infrastructure.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.mq.model.Message;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 消息监听器抽象基类
 * 提供消息处理的通用模板方法
 * 
 * @author Dlow
 * @date 2025/10/18
 */
@Slf4j
public abstract class AbstractMessageListener<T> implements RocketMQListener<String> {
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    /**
     * 获取消息主题
     */
    protected abstract String getTopic();
    
    /**
     * 获取消费者组
     */
    protected abstract String getConsumerGroup();
    
    /**
     * 获取消息标签（可选）
     */
    protected String getSelectorExpression() {
        return "*";
    }
    
    /**
     * 获取消费模式（默认并发消费）
     */
    protected ConsumeMode getConsumeMode() {
        return ConsumeMode.CONCURRENTLY;
    }
    
    /**
     * 获取消息模式（默认集群模式）
     */
    protected MessageModel getMessageModel() {
        return MessageModel.CLUSTERING;
    }
    
    /**
     * 获取最大重试次数
     */
    protected int getMaxReconsumeTimes() {
        return 3;
    }
    
    /**
     * 获取消息体类型
     */
    protected abstract TypeReference<Message<T>> getMessageType();
    
    /**
     * 处理业务逻辑
     */
    protected abstract void handleBusiness(Message<T> message) throws Exception;
    
    /**
     * 处理消息失败时的逻辑
     */
    protected void handleFailure(Message<T> message, Exception e) {
        log.error("消息处理失败 - Topic: {}, MessageId: {}, 错误信息: {}", 
            getTopic(), message.getMessageId(), e.getMessage(), e);
    }
    
    /**
     * 处理消息成功时的逻辑
     */
    protected void handleSuccess(Message<T> message) {
        log.info("消息处理成功 - Topic: {}, MessageId: {}", 
            getTopic(), message.getMessageId());
    }
    
    @Override
    public void onMessage(String messageJson) {
        Message<T> message = null;
        String traceId = null;
        
        try {
            // 解析消息
            message = objectMapper.readValue(messageJson, getMessageType());
            
            // 验证消息不为空
            if (message == null || message.getPayload() == null) {
                log.error("消息内容为空 - MessageJson: {}", messageJson);
                return; // 不重试空消息
            }
            
            // 设置追踪ID到MDC
            traceId = message.getTraceId();
            if (StrUtil.isNotBlank(traceId)) {
                MDC.put("traceId", traceId);
            }
            
            log.info("开始处理消息 - Topic: {}, MessageId: {}, TraceId: {}", 
                getTopic(), message.getMessageId(), traceId);
            
            // 处理业务逻辑
            handleBusiness(message);
            
            // 处理成功回调
            handleSuccess(message);
            
        } catch (Exception e) {
            if (message != null) {
                // 处理失败回调
                handleFailure(message, e);
                
                // 判断是否是业务异常（不需要重试）
                if (e instanceof BizException) {
                    log.error("业务异常，不触发重试 - Topic: {}, MessageId: {}, 错误: {}", 
                        getTopic(), message.getMessageId(), e.getMessage());
                    // 业务异常不重试，直接记录
                    sendToDeadLetterQueue(message, e);
                    return;
                }
                
                // 系统异常，抛出让RocketMQ重试
                // 注：RocketMQ会自动管理重试次数，超过maxReconsumeTimes后会进入死信队列
                log.error("系统异常，触发重试 - Topic: {}, MessageId: {}, 错误: {}", 
                    getTopic(), message.getMessageId(), e.getMessage(), e);
                throw new RuntimeException("消息处理失败，触发重试", e);
            } else {
                log.error("消息解析失败 - MessageJson: {}", messageJson, e);
                // 解析失败的消息不重试，避免无限重试
                return;
            }
        } finally {
            // 清理MDC
            if (StrUtil.isNotBlank(traceId)) {
                MDC.remove("traceId");
            }
        }
    }
    
    /**
     * 发送消息到死信队列
     */
    protected void sendToDeadLetterQueue(Message<T> message, Exception e) {
        // 默认实现：记录日志
        log.error("消息进入死信队列 - Topic: {}, MessageId: {}, 错误信息: {}", 
            getTopic(), message.getMessageId(), e.getMessage());
        // 子类可以重写此方法，将消息发送到死信队列或保存到数据库
    }
}
