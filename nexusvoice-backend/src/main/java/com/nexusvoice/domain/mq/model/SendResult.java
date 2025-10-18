package com.nexusvoice.domain.mq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息发送结果模型
 * 
 * @author Dlow
 * @date 2025/10/18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendResult {
    
    /**
     * 发送是否成功
     */
    private boolean success;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消息队列ID
     */
    private String msgId;
    
    /**
     * 队列ID
     */
    private Integer queueId;
    
    /**
     * 队列偏移量
     */
    private Long queueOffset;
    
    /**
     * 事务ID（事务消息）
     */
    private String transactionId;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 发送时间戳
     */
    private Long timestamp;
    
    /**
     * 创建成功结果
     */
    public static SendResult success(String messageId, String msgId) {
        return SendResult.builder()
                .success(true)
                .messageId(messageId)
                .msgId(msgId)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static SendResult failure(String messageId, String errorMessage) {
        return SendResult.builder()
                .success(false)
                .messageId(messageId)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
