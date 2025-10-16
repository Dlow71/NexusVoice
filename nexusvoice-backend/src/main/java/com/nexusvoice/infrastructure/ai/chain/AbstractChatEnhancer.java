package com.nexusvoice.infrastructure.ai.chain;

import com.nexusvoice.domain.ai.model.EnhancementContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象聊天增强器
 * 实现责任链模式的基础逻辑
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
public abstract class AbstractChatEnhancer implements ChatEnhancer {
    
    protected ChatEnhancer nextEnhancer;
    
    @Override
    public EnhancementContext enhance(EnhancementContext context) {
        // 判断是否应该处理
        if (shouldProcess(context)) {
            log.info("执行增强器：{}", getName());
            // 执行具体的增强逻辑
            context = doEnhance(context);
        }
        
        // 传递给下一个增强器
        if (nextEnhancer != null) {
            context = nextEnhancer.enhance(context);
        }
        
        return context;
    }
    
    @Override
    public ChatEnhancer setNext(ChatEnhancer next) {
        this.nextEnhancer = next;
        return next;
    }
    
    /**
     * 执行具体的增强逻辑（由子类实现）
     * 
     * @param context 增强上下文
     * @return 增强后的上下文
     */
    protected abstract EnhancementContext doEnhance(EnhancementContext context);
}
