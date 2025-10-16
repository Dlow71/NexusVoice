package com.nexusvoice.infrastructure.ai.chain;

import com.nexusvoice.domain.ai.model.EnhancementContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Comparator;

/**
 * 聊天增强链管理器
 * 负责管理和执行增强器链
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Component
public class ChatEnhancementChain {
    
    @Autowired(required = false)
    private List<ChatEnhancer> enhancers;
    
    private ChatEnhancer firstEnhancer;
    
    /**
     * 初始化增强链
     */
    @PostConstruct
    public void initChain() {
        if (enhancers == null || enhancers.isEmpty()) {
            log.info("没有配置聊天增强器");
            return;
        }
        
        // 按优先级排序（如果增强器实现了Ordered接口）
        enhancers.sort(Comparator.comparing(e -> e.getClass().getSimpleName()));
        
        log.info("初始化聊天增强链，增强器数量：{}", enhancers.size());
        
        // 构建责任链
        for (int i = 0; i < enhancers.size() - 1; i++) {
            ChatEnhancer current = enhancers.get(i);
            ChatEnhancer next = enhancers.get(i + 1);
            current.setNext(next);
            log.info("链接增强器：{} -> {}", current.getName(), next.getName());
        }
        
        firstEnhancer = enhancers.get(0);
    }
    
    /**
     * 执行增强链
     * 
     * @param context 增强上下文
     * @return 增强后的上下文
     */
    public EnhancementContext enhance(EnhancementContext context) {
        if (firstEnhancer == null) {
            log.debug("增强链为空，返回原始上下文");
            return context;
        }
        
        log.info("开始执行聊天增强链");
        context = firstEnhancer.enhance(context);
        log.info("聊天增强链执行完成");
        
        return context;
    }
    
    /**
     * 获取增强器数量
     */
    public int getEnhancerCount() {
        return enhancers != null ? enhancers.size() : 0;
    }
    
    /**
     * 获取所有增强器名称
     */
    public List<String> getEnhancerNames() {
        if (enhancers == null) {
            return List.of();
        }
        return enhancers.stream()
                .map(ChatEnhancer::getName)
                .toList();
    }
}
