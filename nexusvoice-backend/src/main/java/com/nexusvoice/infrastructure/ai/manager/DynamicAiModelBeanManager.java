package com.nexusvoice.infrastructure.ai.manager;

import com.nexusvoice.domain.ai.model.AiApiCallLog;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.model.EnhancementContext;
import com.nexusvoice.domain.ai.repository.AiApiCallLogRepository;
import com.nexusvoice.domain.ai.repository.AiModelRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.chain.ChatEnhancementChain;
import com.nexusvoice.infrastructure.ai.converter.AiModelConverter;
import com.nexusvoice.infrastructure.ai.factory.LangChain4jModelFactory;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;
import com.nexusvoice.infrastructure.ai.pool.ApiKeyPoolManager;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 动态AI模型Bean管理器
 * 统一管理所有AI模型服务实例，支持动态加载和热更新
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Component
public class DynamicAiModelBeanManager {
    
    @Autowired
    private AiModelRepository modelRepository;
    
    @Autowired
    private AiApiCallLogRepository callLogRepository;
    
    @Autowired
    private ApiKeyPoolManager apiKeyPoolManager;
    
    @Autowired
    private LangChain4jModelFactory modelFactory;
    
    @Autowired(required = false)
    private ChatEnhancementChain enhancementChain;
    
    /**
     * 模型服务映射表
     * key: provider:model, value: 该模型的服务实例
     */
    private final Map<String, DynamicAiChatService> modelServiceMap = new ConcurrentHashMap<>();
    
    /**
     * 初始化所有模型服务
     */
    @PostConstruct
    public void init() {
        log.info("初始化动态AI模型Bean管理器...");
        loadAllModelServices();
    }
    
    /**
     * 加载所有模型服务
     */
    public void loadAllModelServices() {
        try {
            List<AiModel> enabledModels = modelRepository.findAllEnabled();
            
            for (AiModel model : enabledModels) {
                String modelKey = model.getModelKey();
                DynamicAiChatService service = new DynamicAiChatService(model);
                modelServiceMap.put(modelKey, service);
                log.info("加载模型服务：{}，名称：{}", modelKey, model.getModelName());
            }
            
            log.info("成功加载{}个模型服务", modelServiceMap.size());
            
        } catch (Exception e) {
            log.error("加载模型服务失败", e);
        }
    }
    
    /**
     * 获取模型服务
     * 
     * @param providerCode 厂商代码
     * @param modelCode 模型代码
     * @return AI聊天服务
     */
    public AiChatService getService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        DynamicAiChatService service = modelServiceMap.get(modelKey);
        
        if (service == null) {
            // 尝试动态加载
            Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
            if (modelOpt.isEmpty()) {
                throw new BizException(ErrorCodeEnum.DATA_NOT_FOUND, 
                    String.format("模型%s不存在", modelKey));
            }
            
            AiModel model = modelOpt.get();
            if (!model.isEnabled()) {
                throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                    String.format("模型%s已禁用", modelKey));
            }
            
            service = new DynamicAiChatService(model);
            modelServiceMap.put(modelKey, service);
            log.info("动态加载模型服务：{}", modelKey);
        }
        
        return service;
    }
    
    /**
     * 根据模型键获取服务
     */
    public AiChatService getServiceByModelKey(String modelKey) {
        if (modelKey == null || !modelKey.contains(":")) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "无效的模型键：" + modelKey);
        }
        
        String[] parts = modelKey.split(":", 2);
        return getService(parts[0], parts[1]);
    }
    
    /**
     * 刷新模型服务（热更新）
     */
    public void refreshModelService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        
        Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
        if (modelOpt.isEmpty()) {
            // 模型被删除，移除服务
            modelServiceMap.remove(modelKey);
            modelFactory.clearModelCache(modelKey);
            log.info("移除模型服务：{}", modelKey);
            return;
        }
        
        AiModel model = modelOpt.get();
        if (!model.isEnabled()) {
            // 模型被禁用，移除服务
            modelServiceMap.remove(modelKey);
            modelFactory.clearModelCache(modelKey);
            log.info("禁用模型服务：{}", modelKey);
            return;
        }
        
        // 创建新的服务实例替换旧的
        DynamicAiChatService newService = new DynamicAiChatService(model);
        modelServiceMap.put(modelKey, newService);
        modelFactory.clearModelCache(modelKey);
        apiKeyPoolManager.refreshModelPool(providerCode, modelCode);
        
        log.info("刷新模型服务：{}，名称：{}", modelKey, model.getModelName());
    }
    
    /**
     * 定时刷新任务（每30分钟执行）
     */
    @Scheduled(fixedDelay = 1800000)
    public void scheduledRefresh() {
        log.info("执行定时模型服务刷新任务");
        loadAllModelServices();
    }
    
    /**
     * 获取所有可用的模型键列表
     */
    public List<String> getAvailableModelKeys() {
        return new ArrayList<>(modelServiceMap.keySet());
    }
    
    /**
     * 获取所有可用的模型信息
     */
    public List<AiModel> getAvailableModels() {
        return modelServiceMap.values().stream()
                .map(service -> service.model)
                .collect(Collectors.toList());
    }
    
    /**
     * 动态AI聊天服务内部类
     * 每个模型对应一个服务实例
     */
    private class DynamicAiChatService implements AiChatService {
        private final AiModel model;
        
        public DynamicAiChatService(AiModel model) {
            this.model = model;
        }
        
        @Override
        public ChatResponse chat(ChatRequest request) {
            long startTime = System.currentTimeMillis();
            LocalDateTime requestTime = LocalDateTime.now();
            String requestId = UUID.randomUUID().toString();
            
            AiApiKey apiKey = null;
            AiApiCallLog callLog = null;
            
            try {
                // 1. 获取API密钥
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());
                
                // 2. 创建模型实例
                ChatLanguageModel chatModel = modelFactory.createChatModel(model, apiKey);
                
                // 3. 转换消息格式
                List<ChatMessage> langchainMessages = convertMessages(request);
                
                // 4. 调用模型
                Response<AiMessage> response = chatModel.generate(langchainMessages);
                
                // 5. 计算token和费用
                int promptTokens = estimateTokenCount(request.getMessages().stream()
                        .map(m -> m.getContent())
                        .collect(Collectors.joining("\n")));
                int completionTokens = estimateTokenCount(response.content().text());
                BigDecimal cost = model.calculateCost(promptTokens, completionTokens);
                
                // 6. 更新密钥使用统计
                apiKeyPoolManager.markSuccess(apiKey.getId(), promptTokens + completionTokens, cost);
                
                // 7. 记录调用日志
                callLog = AiApiCallLog.success(
                        apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                        request.getUserId(), request.getConversationId(),
                        requestId, requestTime,
                        (int)(System.currentTimeMillis() - startTime),
                        promptTokens, completionTokens, cost
                );
                callLogRepository.save(callLog);
                
                // 8. 构建响应
                return ChatResponse.success(
                        response.content().text(),
                        model.getModelKey(),
                        ChatResponse.TokenUsage.builder()
                                .promptTokens(promptTokens)
                                .completionTokens(completionTokens)
                                .totalTokens(promptTokens + completionTokens)
                                .build(),
                        System.currentTimeMillis() - startTime
                );
                
            } catch (Exception e) {
                log.error("AI聊天请求失败，模型：{}，错误：{}", model.getModelKey(), e.getMessage(), e);
                
                // 标记密钥失败
                if (apiKey != null) {
                    apiKeyPoolManager.markFailed(apiKey.getId());
                }
                
                // 记录失败日志
                if (apiKey != null) {
                    callLog = AiApiCallLog.failure(
                            apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                            request.getUserId(), request.getConversationId(),
                            requestId, requestTime, e.getMessage()
                    );
                    callLogRepository.save(callLog);
                }
                
                return ChatResponse.error("AI聊天请求失败：" + e.getMessage());
            }
        }
        
        @Override
        public void streamChat(ChatRequest request, 
                              Consumer<StreamChatResponse> onNext,
                              Consumer<Throwable> onError, 
                              Runnable onComplete) {
            
            long startTime = System.currentTimeMillis();
            LocalDateTime requestTime = LocalDateTime.now();
            String requestId = UUID.randomUUID().toString();
            
            AiApiKey apiKey = null;
            
            try {
                // 1. 执行请求增强链（联网搜索、RAG等）
                // 将基础设施请求转换为领域请求
                com.nexusvoice.domain.ai.model.AiChatRequest domainRequest = AiModelConverter.toDomainRequest(request);
                com.nexusvoice.domain.ai.model.AiChatRequest enhancedDomainRequest = domainRequest;
                
                if (enhancementChain != null) {
                    EnhancementContext context = EnhancementContext.builder()
                            .originalRequest(domainRequest)
                            .enhancedRequest(domainRequest)
                            .enableWebSearch(domainRequest.getEnableWebSearch())
                            .enableRag(domainRequest.getEnableRag())
                            .enableMultiModal(domainRequest.getEnableMultiModal())
                            .build();
                    
                    if (context.hasEnhancements()) {
                        log.info("开始执行流式聊天增强链，模型：{}，联网搜索：{}", 
                                model.getModelKey(), domainRequest.getEnableWebSearch());
                        context = enhancementChain.enhance(context);
                        enhancedDomainRequest = context.getEnhancedRequest();
                        log.info("流式聊天增强链执行完成，模型：{}", model.getModelKey());
                    }
                }
                
                // 将增强后的领域请求转换回基础设施请求
                ChatRequest enhancedRequest = AiModelConverter.toInfrastructureRequest(enhancedDomainRequest);
                final ChatRequest finalRequest = enhancedRequest;
                
                // 2. 获取API密钥
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());
                
                // 3. 创建流式模型实例
                StreamingChatLanguageModel streamingModel = modelFactory.createStreamingChatModel(model, apiKey);
                
                // 4. 转换消息格式
                List<ChatMessage> langchainMessages = convertMessages(finalRequest);
                
                // 4. 创建响应处理器
                AtomicInteger index = new AtomicInteger(0);
                AtomicReference<String> responseId = new AtomicReference<>("stream_" + System.currentTimeMillis());
                StringBuilder fullResponse = new StringBuilder();
                AiApiKey finalApiKey = apiKey;
                
                StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        fullResponse.append(token);
                        StreamChatResponse response = StreamChatResponse.content(token, index.getAndIncrement());
                        response.setId(responseId.get());
                        response.setModel(model.getModelKey());
                        onNext.accept(response);
                    }
                    
                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        // 计算费用并记录
                        int promptTokens = estimateTokenCount(finalRequest.getMessages().stream()
                                .map(m -> m.getContent())
                                .collect(Collectors.joining("\n")));
                        int completionTokens = estimateTokenCount(fullResponse.toString());
                        BigDecimal cost = model.calculateCost(promptTokens, completionTokens);
                        
                        // 更新密钥统计
                        apiKeyPoolManager.markSuccess(finalApiKey.getId(), 
                                promptTokens + completionTokens, cost);
                        
                        // 记录调用日志
                        AiApiCallLog callLog = AiApiCallLog.success(
                                finalApiKey.getId(), model.getProviderCode(), model.getModelCode(),
                                finalRequest.getUserId(), finalRequest.getConversationId(),
                                requestId, requestTime,
                                (int)(System.currentTimeMillis() - startTime),
                                promptTokens, completionTokens, cost
                        );
                        callLogRepository.save(callLog);
                        
                        // 发送结束信号
                        StreamChatResponse endResponse = StreamChatResponse.end(
                                response.finishReason() != null ? response.finishReason().toString() : "stop"
                        );
                        onNext.accept(endResponse);
                        onComplete.run();
                    }
                    
                    @Override
                    public void onError(Throwable throwable) {
                        log.error("流式聊天请求失败，模型：{}", model.getModelKey(), throwable);
                        
                        // 标记密钥失败
                        apiKeyPoolManager.markFailed(finalApiKey.getId());
                        
                        // 记录失败日志
                        AiApiCallLog callLog = AiApiCallLog.failure(
                                finalApiKey.getId(), model.getProviderCode(), model.getModelCode(),
                                finalRequest.getUserId(), finalRequest.getConversationId(),
                                requestId, requestTime, throwable.getMessage()
                        );
                        callLogRepository.save(callLog);
                        
                        onError.accept(throwable);
                    }
                };
                
                // 5. 发送开始信号
                StreamChatResponse startResponse = StreamChatResponse.start(responseId.get(), model.getModelKey());
                onNext.accept(startResponse);
                
                // 6. 开始流式请求
                streamingModel.generate(langchainMessages, handler);
                
            } catch (Exception e) {
                log.error("启动流式聊天失败，模型：{}", model.getModelKey(), e);
                
                if (apiKey != null) {
                    apiKeyPoolManager.markFailed(apiKey.getId());
                }
                
                onError.accept(e);
            }
        }
        
        @Override
        public String getModelName() {
            return model.getModelName();
        }
        
        @Override
        public boolean isModelAvailable() {
            return model.isEnabled() && 
                   apiKeyPoolManager.getAvailableKeyCount(model.getProviderCode(), model.getModelCode()) > 0;
        }
        
        @Override
        public int estimateTokenCount(String text) {
            if (text == null || text.isEmpty()) {
                return 0;
            }
            // 简单的估算：平均3-4个字符一个token
            return (int) Math.ceil(text.length() / 3.5);
        }
        
        /**
         * 转换消息格式
         */
        private List<ChatMessage> convertMessages(ChatRequest request) {
            List<ChatMessage> messages = new ArrayList<>();
            
            for (com.nexusvoice.infrastructure.ai.model.ChatMessage msg : request.getMessages()) {
                switch (msg.getRole()) {
                    case SYSTEM:
                        messages.add(SystemMessage.from(msg.getContent()));
                        break;
                    case USER:
                        messages.add(UserMessage.from(msg.getContent()));
                        break;
                    case ASSISTANT:
                        messages.add(AiMessage.from(msg.getContent()));
                        break;
                    default:
                        log.warn("忽略不支持的消息角色：{}", msg.getRole());
                }
            }
            
            return messages;
        }
    }
}
