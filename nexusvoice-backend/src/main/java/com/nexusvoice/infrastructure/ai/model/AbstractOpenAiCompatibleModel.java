package com.nexusvoice.infrastructure.ai.model;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * OpenAI兼容API模型的抽象父类
 * 适用于所有使用OpenAI API格式的模型提供商（如OpenAI、Grok、Moonshot等）
 * 
 * @author NexusVoice
 * @since 2025-10-19
 */
@Slf4j
public abstract class AbstractOpenAiCompatibleModel {
    
    /**
     * 获取API基础URL
     * 子类必须实现此方法，返回对应服务商的API端点
     * 
     * @param apiKey API密钥配置
     * @return API基础URL
     */
    protected abstract String getBaseUrl(AiApiKey apiKey);
    
    /**
     * 获取提供商名称（用于日志）
     * 
     * @return 提供商名称
     */
    protected abstract String getProviderName();
    
    /**
     * 模型创建前的钩子方法
     * 子类可以重写此方法进行特殊配置
     * 
     * @param model AI模型配置
     * @param apiKey API密钥配置
     */
    protected void beforeModelCreation(AiModel model, AiApiKey apiKey) {
        // 默认空实现，子类可重写
    }
    
    /**
     * 模型创建后的钩子方法
     * 子类可以重写此方法进行后处理
     * 
     * @param model AI模型配置
     * @param apiKey API密钥配置
     */
    protected void afterModelCreation(AiModel model, AiApiKey apiKey) {
        // 默认空实现，子类可重写
    }
    
    /**
     * 转换模型名称以适配不同的API提供商
     * 子类可以重写此方法来处理特定提供商的模型名称格式
     * 
     * @param originalModelCode 原始模型代码
     * @param baseUrl API基础URL
     * @return 转换后的模型名称
     */
    protected String transformModelName(String originalModelCode, String baseUrl) {
        // 默认不转换，直接返回原始名称
        return originalModelCode;
    }
    
    /**
     * 创建聊天模型
     * 
     * @param model AI模型配置
     * @param apiKey API密钥配置
     * @return 聊天模型实例
     */
    public ChatLanguageModel createChatModel(AiModel model, AiApiKey apiKey) {
        log.info("创建{}聊天模型，模型：{}", getProviderName(), model.getModelCode());
        
        // 调用前置钩子
        beforeModelCreation(model, apiKey);
        
        // 获取基础URL
        String baseUrl = getBaseUrl(apiKey);
        
        // 转换模型名称（适配不同的API提供商）
        String modelName = transformModelName(model.getModelCode(), baseUrl);
        
        // 构建模型
        var builder = OpenAiChatModel.builder()
                .apiKey(apiKey.getApiKey())
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(model.getDefaultTemperature() != null ? 
                           model.getDefaultTemperature().doubleValue() : 0.7)
                .maxTokens(model.getDefaultMaxTokens() != null ? 
                          model.getDefaultMaxTokens() : getDefaultMaxTokens())
                .timeout(Duration.ofSeconds(model.getDefaultTimeoutSeconds() != null ? 
                        model.getDefaultTimeoutSeconds() : getDefaultTimeout()));
        
        // 设置代理（如果有）
        if (apiKey.getProxyUrl() != null && !apiKey.getProxyUrl().isEmpty()) {
            log.info("{}使用代理：{}", getProviderName(), apiKey.getProxyUrl());
            // TODO: LangChain4j的代理设置需要根据具体版本API调整
        }
        
        ChatLanguageModel chatModel = builder.build();
        
        // 调用后置钩子
        afterModelCreation(model, apiKey);
        
        return chatModel;
    }
    
    /**
     * 创建流式聊天模型
     * 
     * @param model AI模型配置
     * @param apiKey API密钥配置
     * @return 流式聊天模型实例
     */
    public StreamingChatLanguageModel createStreamingChatModel(AiModel model, AiApiKey apiKey) {
        log.info("创建{}流式聊天模型，模型：{}", getProviderName(), model.getModelCode());
        
        // 调用前置钩子
        beforeModelCreation(model, apiKey);
        
        // 获取基础URL
        String baseUrl = getBaseUrl(apiKey);
        
        // 转换模型名称（适配不同的API提供商）
        String modelName = transformModelName(model.getModelCode(), baseUrl);
        
        // 构建流式模型
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey.getApiKey())
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(model.getDefaultTemperature() != null ? 
                           model.getDefaultTemperature().doubleValue() : 0.7)
                .maxTokens(model.getDefaultMaxTokens() != null ? 
                          model.getDefaultMaxTokens() : getDefaultMaxTokens())
                .timeout(Duration.ofSeconds(model.getDefaultTimeoutSeconds() != null ? 
                        model.getDefaultTimeoutSeconds() : getDefaultTimeout()));
        
        StreamingChatLanguageModel streamingModel = builder.build();
        
        // 调用后置钩子
        afterModelCreation(model, apiKey);
        
        return streamingModel;
    }
    
    /**
     * 获取默认的最大token数
     * 子类可以重写此方法提供不同的默认值
     * 
     * @return 默认最大token数
     */
    protected int getDefaultMaxTokens() {
        return 4000;
    }
    
    /**
     * 获取默认的超时时间（秒）
     * 子类可以重写此方法提供不同的默认值
     * 
     * @return 默认超时时间（秒）
     */
    protected int getDefaultTimeout() {
        return 90;
    }
}
