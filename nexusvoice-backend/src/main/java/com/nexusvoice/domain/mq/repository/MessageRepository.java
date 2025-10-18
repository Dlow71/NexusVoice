package com.nexusvoice.domain.mq.repository;

import com.nexusvoice.domain.mq.model.Message;
import com.nexusvoice.domain.mq.model.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * 消息仓储接口
 * 
 * @author Dlow
 * @date 2025/10/18
 */
public interface MessageRepository {
    
    /**
     * 发送同步消息
     * 
     * @param message 消息
     * @return 发送结果
     */
    SendResult send(Message<?> message);
    
    /**
     * 发送异步消息
     * 
     * @param message 消息
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendAsync(Message<?> message);
    
    /**
     * 发送单向消息（不关心发送结果）
     * 
     * @param message 消息
     */
    void sendOneway(Message<?> message);
    
    /**
     * 发送延迟消息
     * 
     * @param message 消息
     * @param delayLevel 延迟级别（1-18）
     * @return 发送结果
     */
    SendResult sendDelay(Message<?> message, Integer delayLevel);
    
    /**
     * 发送顺序消息
     * 
     * @param message 消息
     * @param hashKey 用于选择队列的键
     * @return 发送结果
     */
    SendResult sendOrderly(Message<?> message, String hashKey);
    
    /**
     * 批量发送消息
     * 
     * @param messages 消息列表
     * @return 发送结果
     */
    SendResult sendBatch(java.util.List<Message<?>> messages);
    
    /**
     * 发送事务消息
     * 
     * @param message 消息
     * @param arg 事务参数
     * @return 发送结果
     */
    SendResult sendTransaction(Message<?> message, Object arg);
}
