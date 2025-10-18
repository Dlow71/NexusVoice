package com.nexusvoice.infrastructure.mq.listener.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nexusvoice.domain.mq.model.Message;
import com.nexusvoice.infrastructure.mq.consumer.AbstractMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 示例消息监听器
 * 演示如何实现消息消费者
 * 
 * @author Dlow
 * @date 2025/10/18
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "rocketmq.listeners.example",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
@RocketMQMessageListener(
    topic = "${rocketmq.listeners.example.topic:example_topic}",
    consumerGroup = "${rocketmq.listeners.example.consumer-group:example_consumer_group}",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadNumber = 5,
    maxReconsumeTimes = 3
)
public class ExampleMessageListener extends AbstractMessageListener<String> {
    
    @Override
    protected String getTopic() {
        return "example_topic";
    }
    
    @Override
    protected String getConsumerGroup() {
        return "example_consumer_group";
    }
    
    @Override
    protected TypeReference<Message<String>> getMessageType() {
        return new TypeReference<Message<String>>() {};
    }
    
    @Override
    protected void handleBusiness(Message<String> message) throws Exception {
        log.info("处理示例消息 - MessageId: {}, Payload: {}", 
            message.getMessageId(), message.getPayload());
        
        // 模拟业务处理
        String payload = message.getPayload();
        
        // 这里可以添加具体的业务逻辑
        // 例如：保存到数据库、调用其他服务等
        
        if (payload.contains("error")) {
            // 模拟处理失败
            throw new RuntimeException("模拟处理失败，触发重试");
        }
        
        log.info("示例消息处理完成");
    }
    
    @Override
    protected void handleFailure(Message<String> message, Exception e) {
        log.error("示例消息处理失败 - MessageId: {}, 错误: {}", 
            message.getMessageId(), e.getMessage());
        
        // 可以发送告警、记录失败日志等
        super.handleFailure(message, e);
    }
    
    @Override
    protected void handleSuccess(Message<String> message) {
        log.info("示例消息处理成功 - MessageId: {}", message.getMessageId());
        
        // 可以更新状态、发送通知等
        super.handleSuccess(message);
    }
}
