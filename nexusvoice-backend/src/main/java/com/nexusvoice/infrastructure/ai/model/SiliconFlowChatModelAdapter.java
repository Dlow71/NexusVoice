package com.nexusvoice.infrastructure.ai.model;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 硅基流动聊天模型适配器
 * 处理硅基流动API的模型创建，使用OpenAI兼容的API格式
 * 支持DeepSeek-OCR等多种模型
 * 
 * 硅基流动特性：
 * - OpenAI兼容API
 * - 支持视觉理解和OCR
 * - 提供多种开源模型
 * - 部分模型限免
 * 
 * @author NexusVoice
 * @since 2025-10-26
 */
@Slf4j
@Component
public class SiliconFlowChatModelAdapter extends AbstractOpenAiCompatibleModel {
    
    // 硅基流动API端点
    private static final String DEFAULT_BASE_URL = "https://api.siliconflow.cn/v1";
    
    @Override
    protected String getBaseUrl(AiApiKey apiKey) {
        // 优先使用API密钥配置的URL，其次使用默认的硅基流动API端点
        String baseUrl = apiKey.getBaseUrl() != null && !apiKey.getBaseUrl().isEmpty() 
                ? apiKey.getBaseUrl() 
                : DEFAULT_BASE_URL;
        
        log.debug("硅基流动模型使用API端点：{}", baseUrl);
        return baseUrl;
    }
    
    @Override
    protected String getProviderName() {
        return "硅基流动";
    }
    
    /**
     * 转换模型名称
     * 硅基流动的模型名称通常带厂商前缀，如 deepseek-ai/DeepSeek-OCR
     * 直接使用原始名称，不需要转换
     * 
     * @param originalModelCode 原始模型代码（如：deepseek-ai/DeepSeek-OCR）
     * @param baseUrl API基础URL
     * @return 转换后的模型名称
     */
    @Override
    protected String transformModelName(String originalModelCode, String baseUrl) {
        log.debug("硅基流动模型名称：{}，API端点：{}", originalModelCode, baseUrl);
        // 硅基流动API使用带前缀的模型名称，直接返回
        return originalModelCode;
    }
    
    @Override
    protected void beforeModelCreation(AiModel model, AiApiKey apiKey) {
        // 硅基流动特有的前置处理
        log.info("准备创建硅基流动模型：{}，类型：{}", model.getModelCode(), model.getModelType());
        
        // 记录模型能力
        if (model.supportsOcr()) {
            log.info("模型支持OCR能力");
        }
        if (model.supportsVision()) {
            log.info("模型支持视觉理解能力");
        }
        if (model.supportsImageInput()) {
            log.info("模型支持图像输入");
        }
    }
    
    @Override
    protected void afterModelCreation(AiModel model, AiApiKey apiKey) {
        // 硅基流动特有的后置处理
        log.debug("硅基流动模型创建完成，模型：{}", model.getModelCode());
        
        // 提示可用的高级特性
        if (model.isMultimodal()) {
            log.info("硅基流动多模态模型就绪，支持：{}", String.join(", ", model.getCapabilitiesList()));
        }
    }
    
    @Override
    protected int getDefaultMaxTokens() {
        // 硅基流动默认使用4000 tokens
        return 4000;
    }
    
    @Override
    protected int getDefaultTimeout() {
        // 硅基流动默认60秒超时
        return 60;
    }
}
