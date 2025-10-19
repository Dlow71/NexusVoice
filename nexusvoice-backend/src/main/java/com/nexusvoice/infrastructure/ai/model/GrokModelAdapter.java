package com.nexusvoice.infrastructure.ai.model;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Grok (xAI) 模型适配器
 * 处理Grok API的模型创建，使用OpenAI兼容的API格式
 * 支持官方API和七牛云等代理服务
 * 
 * @author NexusVoice
 * @since 2025-10-19
 */
@Slf4j
@Component
public class GrokModelAdapter extends AbstractOpenAiCompatibleModel {
    
    // 七牛云API特征标识（支持多种域名格式）
    private static final String QINIU_API_INDICATOR_1 = "qiniu.com";
    private static final String QINIU_API_INDICATOR_2 = "qiniuapi.com";
    
    private static final String DEFAULT_BASE_URL = "https://api.x.ai/v1";
    
    @Override
    protected String getBaseUrl(AiApiKey apiKey) {
        // 优先使用API密钥配置的URL，其次使用默认的xAI API端点
        String baseUrl = apiKey.getBaseUrl() != null && !apiKey.getBaseUrl().isEmpty() 
                ? apiKey.getBaseUrl() 
                : DEFAULT_BASE_URL;
        
        log.debug("Grok模型使用API端点：{}", baseUrl);
        return baseUrl;
    }
    
    @Override
    protected String getProviderName() {
        return "Grok (xAI)";
    }
    
    /**
     * 转换模型名称以适配不同的API提供商
     * 
     * @param originalModelCode 原始模型代码（如：grok-4-fast）
     * @param baseUrl API基础URL
     * @return 转换后的模型名称
     */
    @Override
    protected String transformModelName(String originalModelCode, String baseUrl) {
        // 如果是七牛云API，需要添加 "x-ai/" 前缀
        if (baseUrl != null && (baseUrl.contains(QINIU_API_INDICATOR_1) || baseUrl.contains(QINIU_API_INDICATOR_2))) {
            String transformedName = "x-ai/" + originalModelCode;
            log.info("检测到七牛云API（{}），转换模型名称：{} -> {}", baseUrl, originalModelCode, transformedName);
            return transformedName;
        }
        
        // 官方API或其他代理，使用原始名称
        log.debug("使用原始模型名称：{}，API端点：{}", originalModelCode, baseUrl);
        return originalModelCode;
    }
    
    @Override
    protected void beforeModelCreation(AiModel model, AiApiKey apiKey) {
        // Grok特有的前置处理
        log.info("准备创建Grok模型，确认API密钥格式...");
        
        // Grok API密钥通常以"xai-"开头，这里可以添加验证
        if (apiKey.getApiKey() != null && !apiKey.getApiKey().startsWith("xai-")) {
            log.warn("Grok API密钥格式可能不正确，期望以'xai-'开头");
        }
    }
    
    @Override
    protected void afterModelCreation(AiModel model, AiApiKey apiKey) {
        // Grok特有的后置处理
        log.debug("Grok模型创建完成，模型：{}", model.getModelCode());
    }
    
    @Override
    protected int getDefaultMaxTokens() {
        // Grok默认使用4000 tokens（更大的上下文窗口）
        return 4000;
    }
    
    @Override
    protected int getDefaultTimeout() {
        // Grok默认90秒超时（处理复杂推理可能需要更长时间）
        return 90;
    }
}
