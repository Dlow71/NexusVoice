package com.nexusvoice.infrastructure.mq.interceptor;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.client.hook.SendMessageHook;
import org.slf4j.MDC;

/**
 * 消息发送追踪拦截器
 * 用于记录消息发送的链路追踪信息
 * 
 * @author Dlow
 * @date 2025/10/18
 */
@Slf4j
public class MessageTraceInterceptor implements SendMessageHook {
    
    private static final String TRACE_ID_KEY = "traceId";
    
    @Override
    public String hookName() {
        return "MessageTraceHook";
    }
    
    @Override
    public void sendMessageBefore(SendMessageContext context) {
        // 获取或生成traceId
        String traceId = MDC.get(TRACE_ID_KEY);
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
        }
        
        // 设置消息属性
        if (context != null && context.getMessage() != null) {
            context.getMessage().putUserProperty(TRACE_ID_KEY, traceId);
            
            log.info("准备发送消息 - Topic: {}, Tags: {}, Keys: {}, TraceId: {}", 
                context.getMessage().getTopic(),
                context.getMessage().getTags(),
                context.getMessage().getKeys(),
                traceId);
        }
    }
    
    @Override
    public void sendMessageAfter(SendMessageContext context) {
        if (context != null && context.getSendResult() != null) {
            String traceId = context.getMessage().getUserProperty(TRACE_ID_KEY);
            
            if (context.getException() != null) {
                log.error("消息发送失败 - Topic: {}, MsgId: {}, TraceId: {}, 错误信息: {}", 
                    context.getMessage().getTopic(),
                    context.getSendResult().getMsgId(),
                    traceId,
                    context.getException().getMessage(),
                    context.getException());
            } else {
                log.info("消息发送成功 - Topic: {}, MsgId: {}, QueueId: {}, TraceId: {}", 
                    context.getMessage().getTopic(),
                    context.getSendResult().getMsgId(),
                    context.getSendResult().getMessageQueue().getQueueId(),
                    traceId);
            }
        }
    }
}
