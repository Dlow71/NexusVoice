package com.nexusvoice.infrastructure.ai.model;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 硅基流动向量模型适配器
 * 硅基流动使用OpenAI兼容API
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Slf4j
@Component
public class SiliconFlowEmbeddingAdapter {
    
    /**
     * 创建向量模型
     */
    public EmbeddingModel createEmbeddingModel(AiModel model, AiApiKey apiKey) {
        String baseUrl = apiKey.getBaseUrl() != null ? apiKey.getBaseUrl() : model.getDefaultBaseUrl();
        Integer timeout = model.getDefaultTimeoutSeconds() != null ? model.getDefaultTimeoutSeconds() : 60;
        
        log.info("创建硅基流动向量模型，模型：{}，BaseURL：{}", model.getModelCode(), baseUrl);
        
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey.getApiKey())
                .baseUrl(baseUrl)
                .modelName(model.getModelCode())
                .timeout(Duration.ofSeconds(timeout))
                .logRequests(false)
                .logResponses(false);
        
        // Qwen模型支持自定义dimensions参数
        if (model.getModelCode().toLowerCase().contains("qwen")) {
            var config = model.getConfigMap();
            if (config != null && config.containsKey("dimensions")) {
                Object dims = config.get("dimensions");
                if (dims instanceof Integer) {
                    builder.dimensions((Integer) dims);
                    log.info("为Qwen模型设置维度：{}", dims);
                }
            }
        }
        
        return builder.build();
    }
}
