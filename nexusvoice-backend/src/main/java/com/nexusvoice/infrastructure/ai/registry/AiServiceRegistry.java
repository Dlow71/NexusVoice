package com.nexusvoice.infrastructure.ai.registry;

import com.nexusvoice.domain.ai.enums.AiProviderEnum;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI服务注册器
 * 管理不同提供商的AI服务实例
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Component
public class AiServiceRegistry {
    
    @Autowired(required = false)
    private List<AiChatService> aiServices;
    
    /**
     * 提供商到服务的映射
     */
    private final Map<AiProviderEnum, AiChatService> serviceMap = new ConcurrentHashMap<>();
    
    /**
     * 默认服务
     */
    private AiChatService defaultService;
    
    @PostConstruct
    public void init() {
        if (aiServices == null || aiServices.isEmpty()) {
            log.warn("没有注册任何AI服务");
            return;
        }
        
        log.info("开始注册AI服务，服务数量：{}", aiServices.size());
        
        for (AiChatService service : aiServices) {
            // 跳过模板类（Abstract开头的）
            if (service.getClass().getSimpleName().startsWith("Abstract")) {
                continue;
            }
            
            try {
                // 获取服务类型（需要服务实现getProviderType方法）
                if (service instanceof com.nexusvoice.infrastructure.ai.template.AbstractAiChatService) {
                    com.nexusvoice.infrastructure.ai.template.AbstractAiChatService abstractService = 
                            (com.nexusvoice.infrastructure.ai.template.AbstractAiChatService) service;
                    AiProviderEnum provider = abstractService.getProviderType();
                    
                    if (provider != null) {
                        serviceMap.put(provider, service);
                        log.info("注册AI服务：{} -> {}", provider.getName(), service.getClass().getSimpleName());
                        
                        // 设置默认服务（第一个或OpenAI）
                        if (defaultService == null || provider == AiProviderEnum.OPENAI) {
                            defaultService = service;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("注册AI服务失败：{}", service.getClass().getSimpleName(), e);
            }
        }
        
        if (defaultService != null) {
            log.info("默认AI服务：{}", defaultService.getClass().getSimpleName());
        }
    }
    
    /**
     * 获取指定提供商的服务
     * 
     * @param provider 提供商
     * @return AI服务实例
     */
    public AiChatService getService(AiProviderEnum provider) {
        if (provider == null) {
            return getDefaultService();
        }
        
        AiChatService service = serviceMap.get(provider);
        if (service == null) {
            log.warn("未找到提供商{}的服务，使用默认服务", provider.getName());
            return getDefaultService();
        }
        
        return service;
    }
    
    /**
     * 获取默认服务
     */
    public AiChatService getDefaultService() {
        if (defaultService == null) {
            throw new IllegalStateException("没有可用的默认AI服务");
        }
        return defaultService;
    }
    
    /**
     * 检查提供商是否可用
     */
    public boolean isProviderAvailable(AiProviderEnum provider) {
        return serviceMap.containsKey(provider);
    }
    
    /**
     * 获取所有可用的提供商
     */
    public List<AiProviderEnum> getAvailableProviders() {
        return serviceMap.keySet().stream().toList();
    }
    
    /**
     * 获取注册的服务数量
     */
    public int getServiceCount() {
        return serviceMap.size();
    }
}
