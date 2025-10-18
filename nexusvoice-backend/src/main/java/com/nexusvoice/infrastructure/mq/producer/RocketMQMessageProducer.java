package com.nexusvoice.infrastructure.mq.producer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.domain.mq.model.Message;
import com.nexusvoice.domain.mq.model.SendResult;
import com.nexusvoice.domain.mq.repository.MessageRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * RocketMQ消息生产者实现
 * 
 * @author Dlow
 * @date 2025/10/18
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class RocketMQMessageProducer implements MessageRepository {
    
    private final RocketMQTemplate rocketMQTemplate;
    
    @Override
    public SendResult send(Message<?> message) {
        try {
            validateMessage(message);
            String messageId = generateMessageId(message);
            message.setMessageId(messageId);
            
            String destination = buildDestination(message);
            org.springframework.messaging.Message<?> springMessage = buildSpringMessage(message);
            
            org.apache.rocketmq.client.producer.SendResult result = 
                rocketMQTemplate.syncSend(destination, springMessage);
            
            log.info("同步消息发送成功 - MessageId: {}, Topic: {}, MsgId: {}", 
                messageId, message.getTopic(), result.getMsgId());
            
            return SendResult.success(messageId, result.getMsgId());
        } catch (Exception e) {
            log.error("同步消息发送失败 - Topic: {}, 错误信息: {}", 
                message.getTopic(), e.getMessage(), e);
            return SendResult.failure(message.getMessageId(), e.getMessage());
        }
    }
    
    @Override
    public CompletableFuture<SendResult> sendAsync(Message<?> message) {
        CompletableFuture<SendResult> future = new CompletableFuture<>();
        
        try {
            validateMessage(message);
            String messageId = generateMessageId(message);
            message.setMessageId(messageId);
            
            String destination = buildDestination(message);
            org.springframework.messaging.Message<?> springMessage = buildSpringMessage(message);
            
            rocketMQTemplate.asyncSend(destination, springMessage, new SendCallback() {
                @Override
                public void onSuccess(org.apache.rocketmq.client.producer.SendResult result) {
                    log.info("异步消息发送成功 - MessageId: {}, Topic: {}, MsgId: {}", 
                        messageId, message.getTopic(), result.getMsgId());
                    future.complete(SendResult.success(messageId, result.getMsgId()));
                }
                
                @Override
                public void onException(Throwable e) {
                    log.error("异步消息发送失败 - MessageId: {}, Topic: {}, 错误信息: {}", 
                        messageId, message.getTopic(), e.getMessage(), e);
                    future.complete(SendResult.failure(messageId, e.getMessage()));
                }
            });
        } catch (Exception e) {
            log.error("异步消息发送异常 - Topic: {}, 错误信息: {}", 
                message.getTopic(), e.getMessage(), e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    @Override
    public void sendOneway(Message<?> message) {
        try {
            validateMessage(message);
            String messageId = generateMessageId(message);
            message.setMessageId(messageId);
            
            String destination = buildDestination(message);
            org.springframework.messaging.Message<?> springMessage = buildSpringMessage(message);
            
            rocketMQTemplate.sendOneWay(destination, springMessage);
            
            log.info("单向消息发送完成 - MessageId: {}, Topic: {}", 
                messageId, message.getTopic());
        } catch (Exception e) {
            log.error("单向消息发送失败 - Topic: {}, 错误信息: {}", 
                message.getTopic(), e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.MQ_SEND_ERROR, "单向消息发送失败");
        }
    }
    
    @Override
    public SendResult sendDelay(Message<?> message, Integer delayLevel) {
        try {
            validateMessage(message);
            String messageId = generateMessageId(message);
            message.setMessageId(messageId);
            message.setDelayLevel(delayLevel);
            
            String destination = buildDestination(message);
            org.springframework.messaging.Message<?> springMessage = buildSpringMessage(message);
            
            org.apache.rocketmq.client.producer.SendResult result = 
                rocketMQTemplate.syncSend(destination, springMessage, 3000, delayLevel);
            
            log.info("延迟消息发送成功 - MessageId: {}, Topic: {}, DelayLevel: {}, MsgId: {}", 
                messageId, message.getTopic(), delayLevel, result.getMsgId());
            
            return SendResult.success(messageId, result.getMsgId());
        } catch (Exception e) {
            log.error("延迟消息发送失败 - Topic: {}, DelayLevel: {}, 错误信息: {}", 
                message.getTopic(), delayLevel, e.getMessage(), e);
            return SendResult.failure(message.getMessageId(), e.getMessage());
        }
    }
    
    @Override
    public SendResult sendOrderly(Message<?> message, String hashKey) {
        try {
            validateMessage(message);
            String messageId = generateMessageId(message);
            message.setMessageId(messageId);
            message.setKey(hashKey);
            
            String destination = buildDestination(message);
            org.springframework.messaging.Message<?> springMessage = buildSpringMessage(message);
            
            org.apache.rocketmq.client.producer.SendResult result = 
                rocketMQTemplate.syncSendOrderly(destination, springMessage, hashKey);
            
            log.info("顺序消息发送成功 - MessageId: {}, Topic: {}, HashKey: {}, MsgId: {}", 
                messageId, message.getTopic(), hashKey, result.getMsgId());
            
            return SendResult.success(messageId, result.getMsgId());
        } catch (Exception e) {
            log.error("顺序消息发送失败 - Topic: {}, HashKey: {}, 错误信息: {}", 
                message.getTopic(), hashKey, e.getMessage(), e);
            return SendResult.failure(message.getMessageId(), e.getMessage());
        }
    }
    
    @Override
    public SendResult sendBatch(List<Message<?>> messages) {
        try {
            if (messages == null || messages.isEmpty()) {
                throw new BizException(ErrorCodeEnum.PARAM_ERROR, "批量消息不能为空");
            }
            
            // 验证所有消息的Topic必须相同
            String topic = messages.get(0).getTopic();
            if (!messages.stream().allMatch(m -> topic.equals(m.getTopic()))) {
                throw new BizException(ErrorCodeEnum.PARAM_ERROR, "批量消息的Topic必须相同");
            }
            
            List<org.springframework.messaging.Message<?>> springMessages = messages.stream()
                .map(message -> {
                    String messageId = generateMessageId(message);
                    message.setMessageId(messageId);
                    return buildSpringMessage(message);
                })
                .collect(Collectors.toList());
            
            String destination = buildDestination(messages.get(0));
            org.apache.rocketmq.client.producer.SendResult result = 
                rocketMQTemplate.syncSend(destination, springMessages);
            
            log.info("批量消息发送成功 - Topic: {}, 数量: {}, MsgId: {}", 
                topic, messages.size(), result.getMsgId());
            
            return SendResult.success(null, result.getMsgId());
        } catch (Exception e) {
            log.error("批量消息发送失败 - 错误信息: {}", e.getMessage(), e);
            return SendResult.failure(null, e.getMessage());
        }
    }
    
    @Override
    public SendResult sendTransaction(Message<?> message, Object arg) {
        // 事务消息需要单独实现事务监听器，这里暂不实现
        throw new UnsupportedOperationException("事务消息暂未实现");
    }
    
    /**
     * 构建目标地址
     */
    private String buildDestination(Message<?> message) {
        if (StrUtil.isNotBlank(message.getTag())) {
            return message.getTopic() + ":" + message.getTag();
        }
        return message.getTopic();
    }
    
    /**
     * 构建Spring消息
     */
    private org.springframework.messaging.Message<?> buildSpringMessage(Message<?> message) {
        MessageBuilder<?> builder = MessageBuilder.withPayload(message.getPayload());
        
        // 设置消息键
        if (StrUtil.isNotBlank(message.getKey())) {
            builder.setHeader(RocketMQHeaders.KEYS, message.getKey());
        }
        
        // 设置追踪ID
        if (StrUtil.isNotBlank(message.getTraceId())) {
            builder.setHeader("traceId", message.getTraceId());
        }
        
        // 设置自定义属性
        if (message.getProperties() != null) {
            message.getProperties().forEach(builder::setHeader);
        }
        
        return builder.build();
    }
    
    /**
     * 验证消息
     */
    private void validateMessage(Message<?> message) {
        if (message == null) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "消息不能为空");
        }
        if (StrUtil.isBlank(message.getTopic())) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "消息主题不能为空");
        }
        if (message.getPayload() == null) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "消息内容不能为空");
        }
    }
    
    /**
     * 生成消息ID
     */
    private String generateMessageId(Message<?> message) {
        if (StrUtil.isNotBlank(message.getMessageId())) {
            return message.getMessageId();
        }
        return IdUtil.fastSimpleUUID();
    }
}
