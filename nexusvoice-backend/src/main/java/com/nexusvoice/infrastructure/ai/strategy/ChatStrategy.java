package com.nexusvoice.infrastructure.ai.strategy;

import com.nexusvoice.domain.ai.model.AiModelInfo;
import com.nexusvoice.domain.ai.model.EnhancementContext;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;

/**
 * 聊天策略接口
 * 定义不同的聊天处理策略
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
public interface ChatStrategy {
    
    /**
     * 执行聊天策略
     * 
     * @param request 聊天请求
     * @param modelInfo 模型信息
     * @param context 增强上下文
     * @return 聊天响应
     */
    ChatResponse execute(ChatRequest request, AiModelInfo modelInfo, EnhancementContext context);
    
    /**
     * 判断是否支持该请求
     * 
     * @param request 聊天请求
     * @param modelInfo 模型信息
     * @return 是否支持
     */
    boolean supports(ChatRequest request, AiModelInfo modelInfo);
    
    /**
     * 获取策略名称
     * 
     * @return 策略名称
     */
    String getName();
    
    /**
     * 获取策略优先级（数字越小优先级越高）
     * 
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
}
