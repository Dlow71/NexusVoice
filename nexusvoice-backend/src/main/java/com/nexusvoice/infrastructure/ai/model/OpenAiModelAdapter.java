package com.nexusvoice.infrastructure.ai.model;

import com.nexusvoice.domain.ai.model.AiApiKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OpenAI模型适配器
 * 处理OpenAI官方API和兼容API的模型创建
 * 
 * @author NexusVoice
 * @since 2025-10-19
 */
@Slf4j
@Component
public class OpenAiModelAdapter extends AbstractOpenAiCompatibleModel {
    
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    
    @Override
    protected String getBaseUrl(AiApiKey apiKey) {
        // 优先使用API密钥配置的URL，其次使用默认URL
        String baseUrl = apiKey.getBaseUrl() != null && !apiKey.getBaseUrl().isEmpty() 
                ? apiKey.getBaseUrl() 
                : DEFAULT_BASE_URL;
        
        log.debug("OpenAI模型使用API端点：{}", baseUrl);
        return baseUrl;
    }
    
    @Override
    protected String getProviderName() {
        return "OpenAI";
    }
    
    @Override
    protected int getDefaultMaxTokens() {
        // OpenAI默认使用2000 tokens
        return 2000;
    }
    
    @Override
    protected int getDefaultTimeout() {
        // OpenAI默认60秒超时
        return 60;
    }
}
