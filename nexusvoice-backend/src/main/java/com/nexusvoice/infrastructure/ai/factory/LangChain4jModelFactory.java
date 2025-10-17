package com.nexusvoice.infrastructure.ai.factory;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LangChain4j模型工厂
 * 负责根据配置动态创建LangChain4j的模型实例
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Component
public class LangChain4jModelFactory {
    
    /**
     * 模型实例缓存
     * key: modelKey + ":" + apiKeyId
     */
    private final Map<String, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();
    private final Map<String, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();
    
    /**
     * 创建聊天模型
     * 
     * @param model AI模型配置
     * @param apiKey API密钥配置
     * @return LangChain4j聊天模型
     */
    public ChatLanguageModel createChatModel(AiModel model, AiApiKey apiKey) {
        String cacheKey = model.getModelKey() + ":" + apiKey.getId();
        
        // 检查缓存
        ChatLanguageModel cachedModel = modelCache.get(cacheKey);
        if (cachedModel != null) {
            return cachedModel;
        }
        
        // 根据提供商创建不同的模型实例
        ChatLanguageModel chatModel;
        switch (model.getProviderCode().toLowerCase()) {
            case "openai":
                chatModel = createOpenAiChatModel(model, apiKey);
                break;
            case "claude":
                chatModel = createClaudeChatModel(model, apiKey);
                break;
            case "qwen":
                chatModel = createQwenChatModel(model, apiKey);
                break;
            case "wenxin":
                chatModel = createWenxinChatModel(model, apiKey);
                break;
            case "zhipu":
                chatModel = createZhipuChatModel(model, apiKey);
                break;
            default:
                throw new BizException(ErrorCodeEnum.PARAM_ERROR, 
                    "不支持的AI提供商：" + model.getProviderCode());
        }
        
        // 缓存模型实例
        modelCache.put(cacheKey, chatModel);
        log.info("创建聊天模型实例，模型：{}，密钥ID：{}", model.getModelKey(), apiKey.getId());
        
        return chatModel;
    }
    
    /**
     * 创建流式聊天模型
     */
    public StreamingChatLanguageModel createStreamingChatModel(AiModel model, AiApiKey apiKey) {
        String cacheKey = model.getModelKey() + ":" + apiKey.getId();
        
        // 检查缓存
        StreamingChatLanguageModel cachedModel = streamingModelCache.get(cacheKey);
        if (cachedModel != null) {
            return cachedModel;
        }
        
        // 根据提供商创建不同的模型实例
        StreamingChatLanguageModel streamingModel;
        switch (model.getProviderCode().toLowerCase()) {
            case "openai":
                streamingModel = createOpenAiStreamingChatModel(model, apiKey);
                break;
            case "claude":
                streamingModel = createClaudeStreamingChatModel(model, apiKey);
                break;
            case "qwen":
                streamingModel = createQwenStreamingChatModel(model, apiKey);
                break;
            case "wenxin":
                streamingModel = createWenxinStreamingChatModel(model, apiKey);
                break;
            case "zhipu":
                streamingModel = createZhipuStreamingChatModel(model, apiKey);
                break;
            default:
                throw new BizException(ErrorCodeEnum.PARAM_ERROR, 
                    "不支持的AI提供商：" + model.getProviderCode());
        }
        
        // 缓存模型实例
        streamingModelCache.put(cacheKey, streamingModel);
        log.info("创建流式聊天模型实例，模型：{}，密钥ID：{}", model.getModelKey(), apiKey.getId());
        
        return streamingModel;
    }
    
    /**
     * 创建OpenAI聊天模型
     */
    private ChatLanguageModel createOpenAiChatModel(AiModel model, AiApiKey apiKey) {
        var builder = OpenAiChatModel.builder()
                .apiKey(apiKey.getApiKey())
                .modelName(model.getModelCode())
                .temperature(model.getDefaultTemperature() != null ? 
                           model.getDefaultTemperature().doubleValue() : 0.7)
                .maxTokens(model.getDefaultMaxTokens() != null ? 
                          model.getDefaultMaxTokens() : 2000)
                .timeout(Duration.ofSeconds(model.getDefaultTimeoutSeconds() != null ? 
                        model.getDefaultTimeoutSeconds() : 60));
        
        // 设置自定义端点
        String baseUrl = apiKey.getBaseUrl() != null ? apiKey.getBaseUrl() : model.getDefaultBaseUrl();
        if (baseUrl != null && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
        }
        
        // 设置代理
        if (apiKey.getProxyUrl() != null && !apiKey.getProxyUrl().isEmpty()) {
            // TODO: 配置代理，需要根据LangChain4j的具体API调整
            log.info("使用代理：{}", apiKey.getProxyUrl());
        }
        
        return builder.build();
    }
    
    /**
     * 创建OpenAI流式聊天模型
     */
    private StreamingChatLanguageModel createOpenAiStreamingChatModel(AiModel model, AiApiKey apiKey) {
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey.getApiKey())
                .modelName(model.getModelCode())
                .temperature(model.getDefaultTemperature() != null ? 
                           model.getDefaultTemperature().doubleValue() : 0.7)
                .maxTokens(model.getDefaultMaxTokens() != null ? 
                          model.getDefaultMaxTokens() : 2000)
                .timeout(Duration.ofSeconds(model.getDefaultTimeoutSeconds() != null ? 
                        model.getDefaultTimeoutSeconds() : 60));
        
        // 设置自定义端点
        String baseUrl = apiKey.getBaseUrl() != null ? apiKey.getBaseUrl() : model.getDefaultBaseUrl();
        if (baseUrl != null && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
        }
        
        return builder.build();
    }
    
    /**
     * 创建Claude聊天模型
     */
    private ChatLanguageModel createClaudeChatModel(AiModel model, AiApiKey apiKey) {
        // TODO: 实现Claude模型创建
        // 需要添加langchain4j-claude依赖并实现
        log.warn("Claude模型支持尚未实现，使用OpenAI兼容模式");
        return createOpenAiChatModel(model, apiKey);
    }
    
    /**
     * 创建Claude流式聊天模型
     */
    private StreamingChatLanguageModel createClaudeStreamingChatModel(AiModel model, AiApiKey apiKey) {
        // TODO: 实现Claude流式模型创建
        log.warn("Claude流式模型支持尚未实现，使用OpenAI兼容模式");
        return createOpenAiStreamingChatModel(model, apiKey);
    }
    
    /**
     * 创建通义千问聊天模型
     */
    private ChatLanguageModel createQwenChatModel(AiModel model, AiApiKey apiKey) {
        // TODO: 实现通义千问模型创建
        // 需要添加langchain4j-dashscope依赖并实现
        log.warn("通义千问模型支持尚未实现，使用OpenAI兼容模式");
        
        // 通义千问可能使用OpenAI兼容接口
        String baseUrl = apiKey.getBaseUrl() != null ? apiKey.getBaseUrl() : 
                        "https://dashscope.aliyuncs.com/compatible-mode/v1";
        
        AiApiKey compatibleKey = new AiApiKey();
        compatibleKey.setApiKey(apiKey.getApiKey());
        compatibleKey.setBaseUrl(baseUrl);
        
        AiModel compatibleModel = new AiModel();
        compatibleModel.setModelCode(model.getModelCode());
        compatibleModel.setDefaultTemperature(model.getDefaultTemperature());
        compatibleModel.setDefaultMaxTokens(model.getDefaultMaxTokens());
        compatibleModel.setDefaultTimeoutSeconds(model.getDefaultTimeoutSeconds());
        
        return createOpenAiChatModel(compatibleModel, compatibleKey);
    }
    
    /**
     * 创建通义千问流式聊天模型
     */
    private StreamingChatLanguageModel createQwenStreamingChatModel(AiModel model, AiApiKey apiKey) {
        // TODO: 实现通义千问流式模型创建
        log.warn("通义千问流式模型支持尚未实现，使用OpenAI兼容模式");
        
        String baseUrl = apiKey.getBaseUrl() != null ? apiKey.getBaseUrl() : 
                        "https://dashscope.aliyuncs.com/compatible-mode/v1";
        
        AiApiKey compatibleKey = new AiApiKey();
        compatibleKey.setApiKey(apiKey.getApiKey());
        compatibleKey.setBaseUrl(baseUrl);
        
        AiModel compatibleModel = new AiModel();
        compatibleModel.setModelCode(model.getModelCode());
        compatibleModel.setDefaultTemperature(model.getDefaultTemperature());
        compatibleModel.setDefaultMaxTokens(model.getDefaultMaxTokens());
        compatibleModel.setDefaultTimeoutSeconds(model.getDefaultTimeoutSeconds());
        
        return createOpenAiStreamingChatModel(compatibleModel, compatibleKey);
    }
    
    /**
     * 创建文心一言聊天模型
     */
    private ChatLanguageModel createWenxinChatModel(AiModel model, AiApiKey apiKey) {
        // TODO: 实现文心一言模型创建
        // 需要添加langchain4j-qianfan依赖并实现
        log.warn("文心一言模型支持尚未实现");
        throw new BizException(ErrorCodeEnum.FUNCTION_NOT_IMPLEMENTED, "文心一言模型支持尚未实现");
    }
    
    /**
     * 创建文心一言流式聊天模型
     */
    private StreamingChatLanguageModel createWenxinStreamingChatModel(AiModel model, AiApiKey apiKey) {
        // TODO: 实现文心一言流式模型创建
        log.warn("文心一言流式模型支持尚未实现");
        throw new BizException(ErrorCodeEnum.FUNCTION_NOT_IMPLEMENTED, "文心一言流式模型支持尚未实现");
    }
    
    /**
     * 创建智谱AI聊天模型
     */
    private ChatLanguageModel createZhipuChatModel(AiModel model, AiApiKey apiKey) {
        // TODO: 实现智谱AI模型创建
        // 需要添加langchain4j-zhipu依赖并实现
        log.warn("智谱AI模型支持尚未实现，使用OpenAI兼容模式");
        
        // 智谱AI使用OpenAI兼容接口
        String baseUrl = apiKey.getBaseUrl() != null ? apiKey.getBaseUrl() : 
                        "https://open.bigmodel.cn/api/paas/v4";
        
        AiApiKey compatibleKey = new AiApiKey();
        compatibleKey.setApiKey(apiKey.getApiKey());
        compatibleKey.setBaseUrl(baseUrl);
        
        AiModel compatibleModel = new AiModel();
        compatibleModel.setModelCode(model.getModelCode());
        compatibleModel.setDefaultTemperature(model.getDefaultTemperature());
        compatibleModel.setDefaultMaxTokens(model.getDefaultMaxTokens());
        compatibleModel.setDefaultTimeoutSeconds(model.getDefaultTimeoutSeconds());
        
        return createOpenAiChatModel(compatibleModel, compatibleKey);
    }
    
    /**
     * 创建智谱AI流式聊天模型
     */
    private StreamingChatLanguageModel createZhipuStreamingChatModel(AiModel model, AiApiKey apiKey) {
        // TODO: 实现智谱AI流式模型创建
        log.warn("智谱AI流式模型支持尚未实现，使用OpenAI兼容模式");
        
        String baseUrl = apiKey.getBaseUrl() != null ? apiKey.getBaseUrl() : 
                        "https://open.bigmodel.cn/api/paas/v4";
        
        AiApiKey compatibleKey = new AiApiKey();
        compatibleKey.setApiKey(apiKey.getApiKey());
        compatibleKey.setBaseUrl(baseUrl);
        
        AiModel compatibleModel = new AiModel();
        compatibleModel.setModelCode(model.getModelCode());
        compatibleModel.setDefaultTemperature(model.getDefaultTemperature());
        compatibleModel.setDefaultMaxTokens(model.getDefaultMaxTokens());
        compatibleModel.setDefaultTimeoutSeconds(model.getDefaultTimeoutSeconds());
        
        return createOpenAiStreamingChatModel(compatibleModel, compatibleKey);
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        modelCache.clear();
        streamingModelCache.clear();
        log.info("清除模型实例缓存");
    }
    
    /**
     * 清除特定模型的缓存
     */
    public void clearModelCache(String modelKey) {
        modelCache.entrySet().removeIf(entry -> entry.getKey().startsWith(modelKey + ":"));
        streamingModelCache.entrySet().removeIf(entry -> entry.getKey().startsWith(modelKey + ":"));
        log.info("清除模型{}的实例缓存", modelKey);
    }
}
