package com.nexusvoice.infrastructure.ai.chain;

import com.nexusvoice.domain.ai.model.EnhancementContext;

/**
 * 聊天增强器接口
 * 责任链模式的处理器接口
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
public interface ChatEnhancer {
    
    /**
     * 增强请求
     * 
     * @param context 增强上下文
     * @return 增强后的上下文
     */
    EnhancementContext enhance(EnhancementContext context);
    
    /**
     * 设置下一个增强器
     * 
     * @param next 下一个增强器
     * @return 下一个增强器（便于链式调用）
     */
    ChatEnhancer setNext(ChatEnhancer next);
    
    /**
     * 判断是否应该处理
     * 
     * @param context 增强上下文
     * @return 是否处理
     */
    boolean shouldProcess(EnhancementContext context);
    
    /**
     * 获取增强器名称
     * 
     * @return 名称
     */
    String getName();
}
