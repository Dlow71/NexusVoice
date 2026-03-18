package com.nexusvoice.infrastructure.ai.adapter;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.model.AiProvider;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * OpenAI兼容协议适配器
 * 适用于：OpenAI、DeepSeek、Grok、豆包、硅基流动等所有兼容OpenAI API的服务商
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Component("openai_compatibleAdapter")
public class OpenAiCompatibleAdapter implements ProviderAdapter {
    
    @Override
    public ChatLanguageModel createChatModel(AiProvider provider, AiModel model, AiApiKey apiKey) {
        return createChatModel(provider, model, apiKey, null);
    }

    @Override
    public ChatLanguageModel createChatModel(AiProvider provider, AiModel model, AiApiKey apiKey, ChatRequest request) {
        String baseUrl = resolveBaseUrl(provider, model, apiKey);
        
        log.debug("创建OpenAI兼容聊天模型，服务商：{}，模型：{}，端点：{}", 
                provider.getProviderName(), model.getModelCode(), baseUrl);
        
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey.getApiKey())
                .modelName(model.getModelCode())
                .timeout(Duration.ofSeconds(model.getDefaultTimeoutSeconds() != null ? 
                        model.getDefaultTimeoutSeconds() : 60))
                .logRequests(true)
                .logResponses(false);

        applyRequestOverrides(builder, model, request);
        return builder.build();
    }
    
    @Override
    public StreamingChatLanguageModel createStreamingChatModel(AiProvider provider, AiModel model, AiApiKey apiKey) {
        return createStreamingChatModel(provider, model, apiKey, null);
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(AiProvider provider, AiModel model, AiApiKey apiKey, ChatRequest request) {
        String baseUrl = resolveBaseUrl(provider, model, apiKey);
        
        log.debug("创建OpenAI兼容流式聊天模型，服务商：{}，模型：{}，端点：{}", 
                provider.getProviderName(), model.getModelCode(), baseUrl);
        
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey.getApiKey())
                .modelName(model.getModelCode())
                .timeout(Duration.ofSeconds(model.getDefaultTimeoutSeconds() != null ? 
                        model.getDefaultTimeoutSeconds() : 60))
                .logRequests(true)
                .logResponses(false);

        applyRequestOverrides(builder, model, request);
        return builder.build();
    }
    
    @Override
    public EmbeddingModel createEmbeddingModel(AiProvider provider, AiModel model, AiApiKey apiKey) {
        String baseUrl = resolveBaseUrl(provider, model, apiKey);
        
        log.debug("创建OpenAI兼容Embedding模型，服务商：{}，模型：{}，端点：{}", 
                provider.getProviderName(), model.getModelCode(), baseUrl);
        
        // 从config_json读取dimensions参数（如Qwen模型）
        Integer dimensions = extractDimensions(model);
        
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey.getApiKey())
                .modelName(model.getModelCode())
                .timeout(Duration.ofSeconds(model.getDefaultTimeoutSeconds() != null ? 
                        model.getDefaultTimeoutSeconds() : 60))
                .logRequests(true)
                .logResponses(false);
        
        // 如果配置了dimensions，则设置
        if (dimensions != null) {
            builder.dimensions(dimensions);
            log.debug("Embedding模型使用自定义维度：{}", dimensions);
        }
        
        return builder.build();
    }
    
    @Override
    public String getSupportedProtocol() {
        return "openai_compatible";
    }
    
    /**
     * 解析BaseURL（三级优先级）
     * 优先级：API密钥 > 模型 > 服务商
     */
    private String resolveBaseUrl(AiProvider provider, AiModel model, AiApiKey apiKey) {
        // 优先级1：API密钥级配置
        if (apiKey.getBaseUrl() != null && !apiKey.getBaseUrl().trim().isEmpty()) {
            log.debug("使用API密钥级别的BaseURL：{}", apiKey.getBaseUrl());
            return apiKey.getBaseUrl().trim();
        }
        
        // 优先级2：模型级配置
        if (model.getDefaultBaseUrl() != null && !model.getDefaultBaseUrl().trim().isEmpty()) {
            log.debug("使用模型级别的BaseURL：{}", model.getDefaultBaseUrl());
            return model.getDefaultBaseUrl().trim();
        }
        
        // 优先级3：服务商级配置
        if (provider.getDefaultBaseUrl() != null && !provider.getDefaultBaseUrl().trim().isEmpty()) {
            log.debug("使用服务商级别的BaseURL：{}", provider.getDefaultBaseUrl());
            return provider.getDefaultBaseUrl().trim();
        }
        
        throw new BizException(ErrorCodeEnum.AI_MODEL_CONFIG_ERROR, 
                String.format("服务商%s未配置API端点地址", provider.getProviderName()));
    }
    
    /**
     * 从模型配置中提取dimensions参数
     */
    private Integer extractDimensions(AiModel model) {
        try {
            Map<String, Object> configMap = model.getConfigMap();
            if (configMap != null && configMap.containsKey("dimensions")) {
                Object dimObj = configMap.get("dimensions");
                if (dimObj instanceof Number) {
                    return ((Number) dimObj).intValue();
                } else if (dimObj instanceof String) {
                    return Integer.parseInt((String) dimObj);
                }
            }
        } catch (Exception e) {
            log.warn("解析模型dimensions参数失败，模型：{}", model.getModelKey(), e);
        }
        return null;
    }

    private void applyRequestOverrides(OpenAiChatModel.OpenAiChatModelBuilder builder, AiModel model, ChatRequest request) {
        Double temperature = request != null && request.getTemperature() != null
                ? request.getTemperature()
                : (model.getDefaultTemperature() != null ? model.getDefaultTemperature().doubleValue() : 0.7);
        Integer maxTokens = request != null && request.getMaxTokens() != null
                ? request.getMaxTokens()
                : (model.getDefaultMaxTokens() != null ? model.getDefaultMaxTokens() : 2000);

        builder.temperature(temperature).maxTokens(maxTokens);
        if (request != null) {
            if (request.getTopP() != null) {
                builder.topP(request.getTopP());
            }
            if (request.getPresencePenalty() != null) {
                builder.presencePenalty(request.getPresencePenalty());
            }
            if (request.getFrequencyPenalty() != null) {
                builder.frequencyPenalty(request.getFrequencyPenalty());
            }
            if (request.getStop() != null && !request.getStop().isEmpty()) {
                builder.stop(request.getStop());
            }
        }
    }

    private void applyRequestOverrides(OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder, AiModel model, ChatRequest request) {
        Double temperature = request != null && request.getTemperature() != null
                ? request.getTemperature()
                : (model.getDefaultTemperature() != null ? model.getDefaultTemperature().doubleValue() : 0.7);
        Integer maxTokens = request != null && request.getMaxTokens() != null
                ? request.getMaxTokens()
                : (model.getDefaultMaxTokens() != null ? model.getDefaultMaxTokens() : 2000);

        builder.temperature(temperature).maxTokens(maxTokens);
        if (request != null) {
            if (request.getTopP() != null) {
                builder.topP(request.getTopP());
            }
            if (request.getPresencePenalty() != null) {
                builder.presencePenalty(request.getPresencePenalty());
            }
            if (request.getFrequencyPenalty() != null) {
                builder.frequencyPenalty(request.getFrequencyPenalty());
            }
            if (request.getStop() != null && !request.getStop().isEmpty()) {
                builder.stop(request.getStop());
            }
        }
    }
}
