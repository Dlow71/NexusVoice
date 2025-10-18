package com.nexusvoice.domain.mq.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息状态枚举
 * 
 * @author Dlow
 * @date 2025/10/18
 */
@Getter
@AllArgsConstructor
public enum MessageStatusEnum {
    
    /**
     * 待发送
     */
    PENDING("PENDING", "待发送"),
    
    /**
     * 发送中
     */
    SENDING("SENDING", "发送中"),
    
    /**
     * 发送成功
     */
    SUCCESS("SUCCESS", "发送成功"),
    
    /**
     * 发送失败
     */
    FAILED("FAILED", "发送失败"),
    
    /**
     * 已消费
     */
    CONSUMED("CONSUMED", "已消费"),
    
    /**
     * 消费失败
     */
    CONSUME_FAILED("CONSUME_FAILED", "消费失败"),
    
    /**
     * 已重试
     */
    RETRIED("RETRIED", "已重试"),
    
    /**
     * 已死信
     */
    DLQ("DLQ", "已死信");
    
    private final String code;
    private final String desc;
    
    public static MessageStatusEnum getByCode(String code) {
        for (MessageStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
