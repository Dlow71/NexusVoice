package com.nexusvoice.infrastructure.ai.factory;

import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.model.AiProvider;
import com.nexusvoice.domain.ai.repository.AiProviderRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.adapter.ProviderAdapter;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LangChain4j模型工厂（重构版）
 * 使用Provider + Protocol驱动模型创建，支持动态扩展
 * 
 * 核心改进：
 * 1. 通过providerId关联Provider配置
 * 2. 根据Protocol自动选择对应的ProviderAdapter
 * 3. 支持向后兼容（providerCode降级）
 * 4. 三级BaseURL优先级：API密钥 > 模型 > 服务商
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Component
public class LangChain4jModelFactory {
    
    @Autowired
    private AiProviderRepository providerRepository;
    
    @Autowired
    private Map<String, ProviderAdapter> adapterMap;
    
    /**
     * 模型实例缓存
     * key: modelKey + ":" + apiKeyId
     */
    private final Map<String, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();
    private final Map<String, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();
    private final Map<String, EmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();
    
    /**
     * 创建聊天模型（重构版）
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
        
        // 新方案：通过Provider + Protocol创建模型
        ChatLanguageModel chatModel = createChatModelByProvider(model, apiKey);
        
        // 缓存模型实例
        modelCache.put(cacheKey, chatModel);
        log.info("创建聊天模型实例，模型：{}，密钥ID：{}", model.getModelKey(), apiKey.getId());
        
        return chatModel;
    }
    
    /**
     * 创建流式聊天模型（重构版）
     */
    public StreamingChatLanguageModel createStreamingChatModel(AiModel model, AiApiKey apiKey) {
        String cacheKey = model.getModelKey() + ":" + apiKey.getId();
        
        // 检查缓存
        StreamingChatLanguageModel cachedModel = streamingModelCache.get(cacheKey);
        if (cachedModel != null) {
            return cachedModel;
        }
        
        // 新方案：通过Provider + Protocol创建模型
        StreamingChatLanguageModel streamingModel = createStreamingChatModelByProvider(model, apiKey);
        
        // 缓存模型实例
        streamingModelCache.put(cacheKey, streamingModel);
        log.info("创建流式聊天模型实例，模型：{}，密钥ID：{}", model.getModelKey(), apiKey.getId());
        
        return streamingModel;
    }
    
    /**
     * 通过Provider创建聊天模型（核心方法）
     */
    private ChatLanguageModel createChatModelByProvider(AiModel model, AiApiKey apiKey) {
        // 1. 获取Provider配置
        AiProvider provider = getProvider(model);
        
        // 2. 根据Protocol获取适配器
        ProviderAdapter adapter = getAdapter(provider.getProtocol());
        
        // 3. 使用适配器创建模型
        return adapter.createChatModel(provider, model, apiKey);
    }
    
    /**
     * 通过Provider创建流式聊天模型（核心方法）
     */
    private StreamingChatLanguageModel createStreamingChatModelByProvider(AiModel model, AiApiKey apiKey) {
        // 1. 获取Provider配置
        AiProvider provider = getProvider(model);
        
        // 2. 根据Protocol获取适配器
        ProviderAdapter adapter = getAdapter(provider.getProtocol());
        
        // 3. 使用适配器创建模型
        return adapter.createStreamingChatModel(provider, model, apiKey);
    }
    
    /**
     * 获取Provider（支持降级）
     */
    private AiProvider getProvider(AiModel model) {
        // 优先使用providerId
        if (model.getProviderId() != null) {
            return providerRepository.findById(model.getProviderId())
                .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, 
                    "服务商不存在，ID：" + model.getProviderId()));
        }
        
        // 降级：使用providerCode（向后兼容）
        if (model.getProviderCode() != null && !model.getProviderCode().trim().isEmpty()) {
            log.warn("模型{}未配置providerId，降级使用providerCode：{}", 
                    model.getModelKey(), model.getProviderCode());
            
            return providerRepository.findByCode(model.getProviderCode())
                .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, 
                    "服务商不存在，代码：" + model.getProviderCode()));
        }
        
        throw new BizException(ErrorCodeEnum.PARAM_ERROR, 
            "模型既没有providerId也没有providerCode");
    }
    
    /**
     * 根据协议获取适配器
     */
    private ProviderAdapter getAdapter(String protocol) {
        // 适配器bean名称格式：协议代码 + "Adapter"，对协议做规范化处理
        String normalized = com.nexusvoice.domain.ai.model.ProviderProtocol.fromCode(protocol).getCode();
        String adapterBeanName = normalized + "Adapter";
        ProviderAdapter adapter = adapterMap.get(adapterBeanName);
        
        if (adapter == null) {
            throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                "不支持的协议类型：" + protocol + "，请检查是否有对应的ProviderAdapter实现（期望Bean：" + adapterBeanName + ")");
        }
        
        return adapter;
    }
    
    /**
     * 创建向量模型（重构版）
     * 
     * @param model AI模型配置
     * @param apiKey API密钥配置
     * @return LangChain4j向量模型
     */
    public EmbeddingModel createEmbeddingModel(AiModel model, AiApiKey apiKey) {
        String cacheKey = model.getModelKey() + ":" + apiKey.getId();
        
        // 检查缓存
        EmbeddingModel cachedModel = embeddingModelCache.get(cacheKey);
        if (cachedModel != null) {
            return cachedModel;
        }
        
        // 新方案：通过Provider + Protocol创建模型
        EmbeddingModel embeddingModel = createEmbeddingModelByProvider(model, apiKey);
        
        // 缓存模型实例
        embeddingModelCache.put(cacheKey, embeddingModel);
        log.info("创建向量模型实例，模型：{}，密钥ID：{}", model.getModelKey(), apiKey.getId());
        
        return embeddingModel;
    }
    
    /**
     * 通过Provider创建向量模型（核心方法）
     */
    private EmbeddingModel createEmbeddingModelByProvider(AiModel model, AiApiKey apiKey) {
        // 1. 获取Provider配置
        AiProvider provider = getProvider(model);
        
        // 2. 根据Protocol获取适配器
        ProviderAdapter adapter = getAdapter(provider.getProtocol());
        
        // 3. 使用适配器创建模型
        return adapter.createEmbeddingModel(provider, model, apiKey);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearCache() {
        modelCache.clear();
        streamingModelCache.clear();
        embeddingModelCache.clear();
        log.info("清除所有模型实例缓存");
    }
    
    /**
     * 清除特定模型的缓存
     */
    public void clearModelCache(String modelKey) {
        modelCache.entrySet().removeIf(entry -> entry.getKey().startsWith(modelKey + ":"));
        streamingModelCache.entrySet().removeIf(entry -> entry.getKey().startsWith(modelKey + ":"));
        embeddingModelCache.entrySet().removeIf(entry -> entry.getKey().startsWith(modelKey + ":"));
        log.info("清除模型{}的实例缓存", modelKey);
    }
}
