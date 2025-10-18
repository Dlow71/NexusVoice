package com.nexusvoice.application.mq.service;

import com.nexusvoice.domain.mq.enums.DelayLevelEnum;
import com.nexusvoice.domain.mq.model.Message;
import com.nexusvoice.domain.mq.model.SendResult;
import com.nexusvoice.domain.mq.repository.MessageRepository;
import com.nexusvoice.infrastructure.mq.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 消息服务
 * 提供消息发送的业务接口
 * 
 * @author Dlow
 * @date 2025/10/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class MessageService {
    
    private final MessageRepository messageRepository;
    
    /**
     * 发送普通消息
     */
    public SendResult sendMessage(String topic, Object payload) {
        Message<Object> message = MessageUtils.buildMessage(topic, payload);
        log.info("发送普通消息 - Topic: {}, MessageId: {}", topic, message.getMessageId());
        return messageRepository.send(message);
    }
    
    /**
     * 发送带标签的消息
     */
    public SendResult sendMessage(String topic, String tag, Object payload) {
        Message<Object> message = MessageUtils.buildMessage(topic, tag, payload);
        log.info("发送带标签消息 - Topic: {}, Tag: {}, MessageId: {}", 
            topic, tag, message.getMessageId());
        return messageRepository.send(message);
    }
    
    /**
     * 发送带属性的消息
     */
    public SendResult sendMessageWithProperties(String topic, Object payload, Map<String, String> properties) {
        Message<Object> message = MessageUtils.buildMessageWithProperties(topic, payload, properties);
        log.info("发送带属性消息 - Topic: {}, Properties: {}, MessageId: {}", 
            topic, properties, message.getMessageId());
        return messageRepository.send(message);
    }
    
    /**
     * 发送异步消息
     */
    public CompletableFuture<SendResult> sendAsyncMessage(String topic, Object payload) {
        Message<Object> message = MessageUtils.buildMessage(topic, payload);
        log.info("发送异步消息 - Topic: {}, MessageId: {}", topic, message.getMessageId());
        return messageRepository.sendAsync(message);
    }
    
    /**
     * 发送单向消息（不关心结果）
     */
    public void sendOnewayMessage(String topic, Object payload) {
        Message<Object> message = MessageUtils.buildMessage(topic, payload);
        log.info("发送单向消息 - Topic: {}, MessageId: {}", topic, message.getMessageId());
        messageRepository.sendOneway(message);
    }
    
    /**
     * 发送延迟消息（指定延迟级别）
     */
    public SendResult sendDelayMessage(String topic, Object payload, DelayLevelEnum delayLevel) {
        Message<Object> message = MessageUtils.buildDelayMessage(topic, payload, delayLevel);
        log.info("发送延迟消息 - Topic: {}, DelayLevel: {}, MessageId: {}", 
            topic, delayLevel.getDesc(), message.getMessageId());
        return messageRepository.sendDelay(message, delayLevel.getLevel());
    }
    
    /**
     * 发送延迟消息（指定延迟秒数）
     */
    public SendResult sendDelayMessageBySeconds(String topic, Object payload, int seconds) {
        Message<Object> message = MessageUtils.buildDelayMessageBySeconds(topic, payload, seconds);
        log.info("发送延迟消息 - Topic: {}, 延迟{}秒, MessageId: {}", 
            topic, seconds, message.getMessageId());
        return messageRepository.sendDelay(message, message.getDelayLevel());
    }
    
    /**
     * 发送延迟消息（指定延迟分钟数）
     */
    public SendResult sendDelayMessageByMinutes(String topic, Object payload, int minutes) {
        Message<Object> message = MessageUtils.buildDelayMessageByMinutes(topic, payload, minutes);
        log.info("发送延迟消息 - Topic: {}, 延迟{}分钟, MessageId: {}", 
            topic, minutes, message.getMessageId());
        return messageRepository.sendDelay(message, message.getDelayLevel());
    }
    
    /**
     * 发送顺序消息
     */
    public SendResult sendOrderlyMessage(String topic, Object payload, String orderKey) {
        Message<Object> message = MessageUtils.buildOrderlyMessage(topic, payload, orderKey);
        log.info("发送顺序消息 - Topic: {}, OrderKey: {}, MessageId: {}", 
            topic, orderKey, message.getMessageId());
        return messageRepository.sendOrderly(message, orderKey);
    }
    
    /**
     * 批量发送消息
     */
    public SendResult sendBatchMessages(String topic, List<Object> payloads) {
        List<Message<?>> messages = new java.util.ArrayList<>();
        for (Object payload : payloads) {
            messages.add(MessageUtils.buildMessage(topic, payload));
        }
        log.info("批量发送消息 - Topic: {}, 数量: {}", topic, messages.size());
        return messageRepository.sendBatch(messages);
    }
    
    /**
     * 发送事务消息
     */
    public SendResult sendTransactionMessage(String topic, Object payload, Object arg) {
        Message<Object> message = MessageUtils.buildMessage(topic, payload);
        log.info("发送事务消息 - Topic: {}, MessageId: {}", topic, message.getMessageId());
        return messageRepository.sendTransaction(message, arg);
    }
}
