package com.nexusvoice.infrastructure.ai.factory;

import com.nexusvoice.domain.ai.model.AiModelInfo;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.strategy.ChatStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Comparator;

/**
 * 聊天策略工厂
 * 根据请求和模型信息选择合适的策略
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Component
public class ChatStrategyFactory {
    
    @Autowired(required = false)
    private List<ChatStrategy> strategies;
    
    private ChatStrategy defaultStrategy;
    
    @PostConstruct
    public void init() {
        if (strategies == null || strategies.isEmpty()) {
            log.warn("没有配置聊天策略");
            return;
        }
        
        // 按优先级排序
        strategies.sort(Comparator.comparingInt(ChatStrategy::getPriority));
        
        // 找到默认策略（优先级最低的）
        defaultStrategy = strategies.get(strategies.size() - 1);
        
        log.info("初始化聊天策略工厂，策略数量：{}，默认策略：{}", 
                strategies.size(), defaultStrategy.getName());
        
        for (ChatStrategy strategy : strategies) {
            log.info("注册策略：{}，优先级：{}", strategy.getName(), strategy.getPriority());
        }
    }
    
    /**
     * 创建策略
     * 
     * @param request 聊天请求
     * @param modelInfo 模型信息
     * @return 选中的策略
     */
    public ChatStrategy createStrategy(ChatRequest request, AiModelInfo modelInfo) {
        if (strategies == null || strategies.isEmpty()) {
            throw new IllegalStateException("没有可用的聊天策略");
        }
        
        // 遍历策略，找到第一个支持的
        for (ChatStrategy strategy : strategies) {
            if (strategy.supports(request, modelInfo)) {
                log.debug("选择策略：{}", strategy.getName());
                return strategy;
            }
        }
        
        // 返回默认策略
        log.debug("使用默认策略：{}", defaultStrategy.getName());
        return defaultStrategy;
    }
    
    /**
     * 获取所有策略名称
     */
    public List<String> getStrategyNames() {
        if (strategies == null) {
            return List.of();
        }
        return strategies.stream()
                .map(ChatStrategy::getName)
                .toList();
    }
    
    /**
     * 获取策略数量
     */
    public int getStrategyCount() {
        return strategies != null ? strategies.size() : 0;
    }
}
