package com.nexusvoice.infrastructure.ai.manager;

import com.nexusvoice.domain.ai.model.AiApiCallLog;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.model.AiProvider;
import com.nexusvoice.domain.ai.model.EnhancementContext;
import com.nexusvoice.domain.ai.repository.AiApiCallLogRepository;
import com.nexusvoice.domain.ai.repository.AiModelRepository;
import com.nexusvoice.domain.ai.repository.AiProviderRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.chain.ChatEnhancementChain;
import com.nexusvoice.infrastructure.ai.client.NativeThinkingChatClient;
import com.nexusvoice.infrastructure.ai.converter.AiModelConverter;
import com.nexusvoice.infrastructure.ai.factory.LangChain4jModelFactory;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;
import com.nexusvoice.infrastructure.ai.model.EmbeddingRequest;
import com.nexusvoice.infrastructure.ai.model.EmbeddingResponse;
import com.nexusvoice.infrastructure.ai.model.RerankRequest;
import com.nexusvoice.infrastructure.ai.model.RerankResponse;
import com.nexusvoice.infrastructure.ai.pool.ApiKeyPoolManager;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.infrastructure.ai.service.AiEmbeddingService;
import com.nexusvoice.infrastructure.ai.service.AiRerankService;
import com.nexusvoice.infrastructure.rag.service.RagCitationService;
import com.nexusvoice.infrastructure.rag.service.StrictRagAnswerPostProcessor;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private AiProviderRepository providerRepository;
    
    @Autowired
    private ApiKeyPoolManager apiKeyPoolManager;
    
    @Autowired
    private LangChain4jModelFactory modelFactory;
    
    @Autowired(required = false)
    private ChatEnhancementChain enhancementChain;
    
    @Autowired(required = false)
    private com.nexusvoice.infrastructure.ai.model.SiliconFlowImageAdapter siliconFlowImageAdapter;
    
    @Autowired(required = false)
    private com.nexusvoice.infrastructure.ai.model.SiliconFlowAsrAdapter siliconFlowAsrAdapter;
    
    @Autowired(required = false)
    private com.nexusvoice.infrastructure.ai.model.SiliconFlowEmbeddingAdapter siliconFlowEmbeddingAdapter;
    
    @Autowired(required = false)
    private com.nexusvoice.infrastructure.ai.model.SiliconFlowRerankAdapter siliconFlowRerankAdapter;
    
    @Autowired(required = false)
    private com.nexusvoice.infrastructure.tts.adapter.QiniuTtsAdapter qiniuTtsAdapter;
    
    @Autowired(required = false)
    private com.nexusvoice.infrastructure.video.adapter.ZhipuVideoAdapter zhipuVideoAdapter;

    @Autowired(required = false)
    private StrictRagAnswerPostProcessor strictRagAnswerPostProcessor;

    @Autowired(required = false)
    private RagCitationService ragCitationService;

    @Autowired
    private NativeThinkingChatClient nativeThinkingChatClient;
    
    /**
     * 模型服务映射表
     * key: provider:model, value: 该模型的服务实例
     */
    private final Map<String, DynamicAiChatService> modelServiceMap = new ConcurrentHashMap<>();
    
    /**
     * 图像生成服务映射表
     * key: provider:model, value: 该模型的图像生成服务实例
     */
    private final Map<String, DynamicAiImageService> imageServiceMap = new ConcurrentHashMap<>();
    
    /**
     * ASR语音识别服务映射表
     * key: provider:model, value: 该模型的ASR服务实例
     */
    private final Map<String, DynamicAiAsrService> asrServiceMap = new ConcurrentHashMap<>();
    
    /**
     * TTS语音合成服务映射表
     * key: provider:model, value: 该模型的TTS服务实例
     */
    private final Map<String, DynamicAiTtsService> ttsServiceMap = new ConcurrentHashMap<>();
    
    /**
     * 视频生成服务映射表
     * key: provider:model, value: 该模型的视频生成服务实例
     */
    private final Map<String, DynamicAiVideoService> videoServiceMap = new ConcurrentHashMap<>();
    
    /**
     * Embedding向量化服务映射表
     * key: provider:model, value: 该模型的Embedding服务实例
     */
    private final Map<String, DynamicAiEmbeddingService> embeddingServiceMap = new ConcurrentHashMap<>();
    
    /**
     * Rerank重排序服务映射表
     * key: provider:model, value: 该模型的Rerank服务实例
     */
    private final Map<String, DynamicAiRerankService> rerankServiceMap = new ConcurrentHashMap<>();
    
    /**
     * 加载所有类型的AI模型服务
     * 由AiModelInitializer统一调度，避免重复查询数据库
     * 
     * @param chatModels 对话模型列表
     * @param imageModels 图像模型列表
     * @param asrModels ASR模型列表
     * @param ttsModels TTS模型列表
     * @param videoModels 视频模型列表
     * @param embeddingModels Embedding模型列表
     * @param rerankModels Rerank模型列表
     */
    public void loadModels(List<AiModel> chatModels, List<AiModel> imageModels, List<AiModel> asrModels, 
                          List<AiModel> ttsModels, List<AiModel> videoModels,
                          List<AiModel> embeddingModels, List<AiModel> rerankModels) {
        try {
            log.info("开始加载所有AI模型服务...");
            
            // 清空现有服务（用于刷新场景）
            modelServiceMap.clear();
            imageServiceMap.clear();
            asrServiceMap.clear();
            ttsServiceMap.clear();
            videoServiceMap.clear();
            embeddingServiceMap.clear();
            rerankServiceMap.clear();
            
            // 加载对话模型
            for (AiModel model : chatModels) {
                String modelKey = model.getModelKey();
                DynamicAiChatService service = new DynamicAiChatService(model);
                modelServiceMap.put(modelKey, service);
                log.debug("加载对话模型：{}，名称：{}", modelKey, model.getModelName());
            }
            
            // 加载图像模型
            for (AiModel model : imageModels) {
                String modelKey = model.getModelKey();
                DynamicAiImageService imageService = new DynamicAiImageService(model);
                imageServiceMap.put(modelKey, imageService);
                log.debug("加载图像模型：{}，名称：{}", modelKey, model.getModelName());
            }
            
            // 加载ASR模型
            for (AiModel model : asrModels) {
                String modelKey = model.getModelKey();
                DynamicAiAsrService asrService = new DynamicAiAsrService(model);
                asrServiceMap.put(modelKey, asrService);
                log.debug("加载ASR模型：{}，名称：{}", modelKey, model.getModelName());
            }
            
            // 加载TTS模型
            for (AiModel model : ttsModels) {
                String modelKey = model.getModelKey();
                DynamicAiTtsService ttsService = new DynamicAiTtsService(model);
                ttsServiceMap.put(modelKey, ttsService);
                log.debug("加载TTS模型：{}，名称：{}", modelKey, model.getModelName());
            }
            
            // 加载视频模型
            for (AiModel model : videoModels) {
                String modelKey = model.getModelKey();
                DynamicAiVideoService videoService = new DynamicAiVideoService(model);
                videoServiceMap.put(modelKey, videoService);
                log.debug("加载视频模型：{}，名称：{}", modelKey, model.getModelName());
            }
            
            // 加载Embedding模型
            for (AiModel model : embeddingModels) {
                String modelKey = model.getModelKey();
                DynamicAiEmbeddingService embeddingService = new DynamicAiEmbeddingService(model);
                embeddingServiceMap.put(modelKey, embeddingService);
                log.debug("加载Embedding模型：{}，名称：{}", modelKey, model.getModelName());
            }
            
            // 加载Rerank模型
            for (AiModel model : rerankModels) {
                String modelKey = model.getModelKey();
                DynamicAiRerankService rerankService = new DynamicAiRerankService(model);
                rerankServiceMap.put(modelKey, rerankService);
                log.debug("加载Rerank模型：{}，名称：{}", modelKey, model.getModelName());
            }
            
            log.info("成功加载{}个对话模型，{}个图像模型，{}个ASR模型，{}个TTS模型，{}个视频模型，{}个Embedding模型，{}个Rerank模型", 
                    modelServiceMap.size(), imageServiceMap.size(), asrServiceMap.size(), 
                    ttsServiceMap.size(), videoServiceMap.size(), embeddingServiceMap.size(), rerankServiceMap.size());
            
        } catch (Exception e) {
            log.error("加载AI模型服务失败", e);
            throw new RuntimeException("加载AI模型服务失败", e);
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
     * 刷新单个模型服务（热更新）
     * 用于单个模型配置变更时的即时刷新
     */
    public void refreshModelService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        
        Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
        if (modelOpt.isEmpty()) {
            // 模型被删除，移除服务
            modelServiceMap.remove(modelKey);
            imageServiceMap.remove(modelKey);
            asrServiceMap.remove(modelKey);
            modelFactory.clearModelCache(modelKey);
            log.info("移除模型服务：{}", modelKey);
            return;
        }
        
        AiModel model = modelOpt.get();
        if (!model.isEnabled()) {
            // 模型被禁用，移除服务
            modelServiceMap.remove(modelKey);
            imageServiceMap.remove(modelKey);
            asrServiceMap.remove(modelKey);
            modelFactory.clearModelCache(modelKey);
            log.info("禁用模型服务：{}", modelKey);
            return;
        }
        
        // 根据类型创建新服务
        if (model.isChatModel()) {
            DynamicAiChatService newService = new DynamicAiChatService(model);
            modelServiceMap.put(modelKey, newService);
            log.info("刷新对话模型服务：{}，名称：{}", modelKey, model.getModelName());
        } else if (model.isImageModel()) {
            DynamicAiImageService newService = new DynamicAiImageService(model);
            imageServiceMap.put(modelKey, newService);
            log.info("刷新图像模型服务：{}，名称：{}", modelKey, model.getModelName());
        } else if (model.isAsrModel()) {
            DynamicAiAsrService newService = new DynamicAiAsrService(model);
            asrServiceMap.put(modelKey, newService);
            log.info("刷新ASR模型服务：{}，名称：{}", modelKey, model.getModelName());
        }
        
        modelFactory.clearModelCache(modelKey);
        apiKeyPoolManager.refreshModelPool(providerCode, modelCode);
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

    public AiModel getModelByKey(String modelKey) {
        if (modelKey == null || !modelKey.contains(":")) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "无效的模型键：" + modelKey);
        }
        String[] parts = modelKey.split(":", 2);
        return modelRepository.findByProviderAndModel(parts[0], parts[1])
                .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "模型不存在：" + modelKey));
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
                ChatRequest finalRequest = enhanceRequestIfNeeded(request);

                // 1. 获取API密钥
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());

                if (shouldUseNativeThinkingClient()) {
                    ChatResponse nativeResponse = chatWithNativeThinkingClient(finalRequest, apiKey, startTime, requestTime, requestId);
                    return nativeResponse;
                }
                
                // 2. 创建模型实例
                ChatLanguageModel chatModel = modelFactory.createChatModel(model, apiKey, finalRequest);
                
                // 3. 转换消息格式
                List<ChatMessage> langchainMessages = convertMessages(finalRequest);
                
                // 4. 调用模型
                Response<AiMessage> response = chatModel.generate(langchainMessages);
                String finalContent = postProcessIfNeeded(finalRequest, response.content().text());
                RagCitationService.RagAnswerPackage answerPackage = buildAnswerPackage(finalRequest, finalContent);
                finalContent = answerPackage.content();
                
                // 5. 计算token和费用
                int promptTokens = estimateTokenCount(finalRequest.getMessages().stream()
                        .map(m -> m.getContent())
                        .collect(Collectors.joining("\n")));
                int completionTokens = estimateTokenCount(finalContent);
                BigDecimal cost = model.calculateCost(promptTokens, completionTokens);
                
                // 6. 更新密钥使用统计
                apiKeyPoolManager.markSuccess(apiKey.getId(), promptTokens + completionTokens, cost);
                
                // 7. 记录调用日志
                callLog = AiApiCallLog.success(
                        apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                        finalRequest.getUserId(), finalRequest.getConversationId(),
                        requestId, requestTime,
                        (int)(System.currentTimeMillis() - startTime),
                        promptTokens, completionTokens, cost
                );
                com.nexusvoice.infrastructure.ai.util.CallLogContextEnricher.enrich(callLog);
                callLogRepository.save(callLog);
                
                // 8. 构建响应
                ChatResponse chatResponse = ChatResponse.success(
                        finalContent,
                        model.getModelKey(),
                        ChatResponse.TokenUsage.builder()
                                .promptTokens(promptTokens)
                                .completionTokens(completionTokens)
                                .totalTokens(promptTokens + completionTokens)
                                .build(),
                        System.currentTimeMillis() - startTime
                );
                chatResponse.setCitations(answerPackage.citations());
                return chatResponse;
                
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
                    com.nexusvoice.infrastructure.ai.util.CallLogContextEnricher.enrich(callLog);
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
            
            log.info("开始流式聊天，模型：{}，包含图片：{}", 
                model.getModelKey(), 
                request.getImageUrls() != null ? request.getImageUrls().size() : 0);
            
            AiApiKey apiKey = null;
            
            try {
                final ChatRequest finalRequest = enhanceRequestIfNeeded(request);
                final boolean strictBufferedMode = isStrictBufferedMode(finalRequest);
                
                // 2. 获取API密钥
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());

                if (shouldUseNativeThinkingClient()) {
                    streamChatWithNativeThinkingClient(finalRequest, apiKey, strictBufferedMode,
                            startTime, requestTime, requestId, onNext, onError, onComplete);
                    return;
                }
                
                // 3. 创建流式模型实例
                StreamingChatLanguageModel streamingModel = modelFactory.createStreamingChatModel(model, apiKey, finalRequest);
                
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
                        if (!strictBufferedMode) {
                            StreamChatResponse response = StreamChatResponse.content(token, index.getAndIncrement());
                            response.setId(responseId.get());
                            response.setModel(model.getModelKey());
                            onNext.accept(response);
                        }
                    }
                    
                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        String finalContent = strictBufferedMode
                                ? postProcessIfNeeded(finalRequest, fullResponse.toString())
                                : fullResponse.toString();
                        RagCitationService.RagAnswerPackage answerPackage = buildAnswerPackage(finalRequest, finalContent);
                        finalContent = answerPackage.content();
                        if (strictBufferedMode) {
                            emitBufferedContent(finalContent, model.getModelKey(), responseId.get(), onNext);
                        }

                        // 计算费用并记录
                        int promptTokens = estimateTokenCount(finalRequest.getMessages().stream()
                                .map(m -> m.getContent())
                                .collect(Collectors.joining("\n")));
                        int completionTokens = estimateTokenCount(finalContent);
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
                        com.nexusvoice.infrastructure.ai.util.CallLogContextEnricher.enrich(callLog);
                        callLogRepository.save(callLog);
                        
                        // 发送结束信号
                        StreamChatResponse endResponse = StreamChatResponse.end(
                                response.finishReason() != null ? response.finishReason().toString() : "stop"
                        );
                        endResponse.setContent(finalContent);
                        endResponse.setCitations(answerPackage.citations());
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
                        com.nexusvoice.infrastructure.ai.util.CallLogContextEnricher.enrich(callLog);
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

        private ChatRequest enhanceRequestIfNeeded(ChatRequest request) {
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
                    log.info("开始执行聊天增强链，模型：{}，联网搜索：{}，RAG：{}",
                            model.getModelKey(), domainRequest.getEnableWebSearch(), domainRequest.getEnableRag());
                    context = enhancementChain.enhance(context);
                    enhancedDomainRequest = context.getEnhancedRequest();
                    log.info("聊天增强链执行完成，模型：{}", model.getModelKey());
                }
            }

            return AiModelConverter.toInfrastructureRequest(enhancedDomainRequest);
        }

        private String postProcessIfNeeded(ChatRequest request, String content) {
            if (strictRagAnswerPostProcessor == null) {
                return content;
            }
            return strictRagAnswerPostProcessor.postProcess(request, content);
        }

        private boolean isStrictBufferedMode(ChatRequest request) {
            return request != null
                    && Boolean.TRUE.equals(request.getEnableRag())
                    && "STRICT".equalsIgnoreCase(request.getRagGroundingMode());
        }

        private boolean shouldUseNativeThinkingClient() {
            Object enabled = model.getConfigMap().get("nativeThinkingProtocol");
            if (enabled instanceof Boolean booleanValue) {
                return booleanValue;
            }
            return "true".equalsIgnoreCase(String.valueOf(enabled));
        }

        private ChatResponse chatWithNativeThinkingClient(ChatRequest finalRequest,
                                                          AiApiKey apiKey,
                                                          long startTime,
                                                          LocalDateTime requestTime,
                                                          String requestId) throws Exception {
            AiProvider provider = resolveProvider();
            NativeThinkingChatClient.ChatResult nativeResult = nativeThinkingChatClient.chat(provider, model, apiKey, finalRequest);

            String finalContent = postProcessIfNeeded(finalRequest, nativeResult.content());
            RagCitationService.RagAnswerPackage answerPackage = buildAnswerPackage(finalRequest, finalContent);
            finalContent = answerPackage.content();

            int promptTokens = estimateTokenCount(finalRequest.getMessages().stream()
                    .map(m -> m.getContent())
                    .collect(Collectors.joining("\n")));
            int completionTokens = estimateTokenCount(finalContent);
            BigDecimal cost = model.calculateCost(promptTokens, completionTokens);

            apiKeyPoolManager.markSuccess(apiKey.getId(), promptTokens + completionTokens, cost);

            AiApiCallLog callLog = AiApiCallLog.success(
                    apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                    finalRequest.getUserId(), finalRequest.getConversationId(),
                    requestId, requestTime,
                    (int) (System.currentTimeMillis() - startTime),
                    promptTokens, completionTokens, cost
            );
            com.nexusvoice.infrastructure.ai.util.CallLogContextEnricher.enrich(callLog);
            callLogRepository.save(callLog);

            ChatResponse chatResponse = ChatResponse.success(
                    finalContent,
                    model.getModelKey(),
                    ChatResponse.TokenUsage.builder()
                            .promptTokens(promptTokens)
                            .completionTokens(completionTokens)
                            .totalTokens(promptTokens + completionTokens)
                            .build(),
                    System.currentTimeMillis() - startTime
            );
            chatResponse.setCitations(answerPackage.citations());
            chatResponse.setReasoningContent(shouldExposeReasoning(finalRequest) ? nativeResult.reasoningContent() : null);
            chatResponse.setFinishReason(
                    nativeResult.finishReason() != null && !nativeResult.finishReason().isBlank()
                            ? nativeResult.finishReason()
                            : "stop"
            );
            return chatResponse;
        }

        private void streamChatWithNativeThinkingClient(ChatRequest finalRequest,
                                                        AiApiKey apiKey,
                                                        boolean strictBufferedMode,
                                                        long startTime,
                                                        LocalDateTime requestTime,
                                                        String requestId,
                                                        Consumer<StreamChatResponse> onNext,
                                                        Consumer<Throwable> onError,
                                                        Runnable onComplete) {
            AiProvider provider = resolveProvider();
            AtomicInteger contentIndex = new AtomicInteger(0);
            AtomicInteger thinkingIndex = new AtomicInteger(0);
            AtomicReference<String> responseId = new AtomicReference<>("stream_" + System.currentTimeMillis());
            StringBuilder fullResponse = new StringBuilder();
            StringBuilder fullReasoning = new StringBuilder();
            AtomicBoolean thinkingStarted = new AtomicBoolean(false);
            AtomicBoolean thinkingEnded = new AtomicBoolean(false);
            AiApiKey finalApiKey = apiKey;

            try {
                StreamChatResponse startResponse = StreamChatResponse.start(responseId.get(), model.getModelKey());
                onNext.accept(startResponse);

                nativeThinkingChatClient.streamChat(provider, model, apiKey, finalRequest, new NativeThinkingChatClient.StreamListener() {
                    @Override
                    public void onThinkingStart() {
                        if (!shouldExposeReasoning(finalRequest) || thinkingStarted.getAndSet(true)) {
                            return;
                        }
                        StreamChatResponse response = StreamChatResponse.thinkingStart(responseId.get(), model.getModelKey());
                        onNext.accept(response);
                    }

                    @Override
                    public void onThinkingDelta(String delta) {
                        fullReasoning.append(delta);
                        if (!shouldExposeReasoning(finalRequest)) {
                            return;
                        }
                        StreamChatResponse response = StreamChatResponse.thinkingDelta(delta, thinkingIndex.getAndIncrement());
                        response.setId(responseId.get());
                        response.setModel(model.getModelKey());
                        onNext.accept(response);
                    }

                    @Override
                    public void onThinkingEnd() {
                        if (!shouldExposeReasoning(finalRequest) || !thinkingStarted.get() || thinkingEnded.getAndSet(true)) {
                            return;
                        }
                        StreamChatResponse response = StreamChatResponse.thinkingEnd(responseId.get(), model.getModelKey());
                        onNext.accept(response);
                    }

                    @Override
                    public void onContentDelta(String delta) {
                        fullResponse.append(delta);
                        if (!strictBufferedMode) {
                            StreamChatResponse response = StreamChatResponse.content(delta, contentIndex.getAndIncrement());
                            response.setId(responseId.get());
                            response.setModel(model.getModelKey());
                            onNext.accept(response);
                        }
                    }

                    @Override
                    public void onComplete(NativeThinkingChatClient.ChatResult result) {
                        try {
                            if (thinkingStarted.get() && !thinkingEnded.get()) {
                                onThinkingEnd();
                            }

                            String finalContent = strictBufferedMode
                                    ? postProcessIfNeeded(finalRequest, fullResponse.toString())
                                    : fullResponse.toString();
                            RagCitationService.RagAnswerPackage answerPackage = buildAnswerPackage(finalRequest, finalContent);
                            finalContent = answerPackage.content();
                            if (strictBufferedMode) {
                                emitBufferedContent(finalContent, model.getModelKey(), responseId.get(), onNext);
                            }

                            int promptTokens = estimateTokenCount(finalRequest.getMessages().stream()
                                    .map(m -> m.getContent())
                                    .collect(Collectors.joining("\n")));
                            int completionTokens = estimateTokenCount(finalContent);
                            BigDecimal cost = model.calculateCost(promptTokens, completionTokens);

                            apiKeyPoolManager.markSuccess(finalApiKey.getId(), promptTokens + completionTokens, cost);

                            AiApiCallLog callLog = AiApiCallLog.success(
                                    finalApiKey.getId(), model.getProviderCode(), model.getModelCode(),
                                    finalRequest.getUserId(), finalRequest.getConversationId(),
                                    requestId, requestTime,
                                    (int) (System.currentTimeMillis() - startTime),
                                    promptTokens, completionTokens, cost
                            );
                            com.nexusvoice.infrastructure.ai.util.CallLogContextEnricher.enrich(callLog);
                            callLogRepository.save(callLog);

                            StreamChatResponse endResponse = StreamChatResponse.end(
                                    result.finishReason() != null && !result.finishReason().isBlank()
                                            ? result.finishReason()
                                            : "stop"
                            );
                            endResponse.setContent(finalContent);
                            endResponse.setCitations(answerPackage.citations());
                            endResponse.setReasoningContent(shouldExposeReasoning(finalRequest) ? fullReasoning.toString() : null);
                            onNext.accept(endResponse);
                            onComplete.run();
                        } catch (Exception e) {
                            onError.accept(e);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("原生思考流聊天失败，模型：{}", model.getModelKey(), e);
                apiKeyPoolManager.markFailed(apiKey.getId());

                AiApiCallLog callLog = AiApiCallLog.failure(
                        apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                        finalRequest.getUserId(), finalRequest.getConversationId(),
                        requestId, requestTime, e.getMessage()
                );
                com.nexusvoice.infrastructure.ai.util.CallLogContextEnricher.enrich(callLog);
                callLogRepository.save(callLog);

                onError.accept(e);
            }
        }

        private boolean shouldExposeReasoning(ChatRequest request) {
            return request != null
                    && Boolean.TRUE.equals(request.getShowThinking())
                    && !"disabled".equalsIgnoreCase(String.valueOf(request.getThinkingMode()));
        }

        private AiProvider resolveProvider() {
            return providerRepository.findByCode(model.getProviderCode())
                    .orElseThrow(() -> BizException.of(
                            ErrorCodeEnum.AI_MODEL_CONFIG_ERROR,
                            "模型未绑定可用的服务商配置：" + model.getProviderCode()
                    ));
        }

        private RagCitationService.RagAnswerPackage buildAnswerPackage(ChatRequest request, String content) {
            if (ragCitationService == null) {
                return new RagCitationService.RagAnswerPackage(content, List.of());
            }
            return ragCitationService.buildAnswerPackage(request, content);
        }

        private void emitBufferedContent(String content,
                                         String modelKey,
                                         String responseId,
                                         Consumer<StreamChatResponse> onNext) {
            if (content == null || content.isBlank()) {
                return;
            }
            List<String> chunks = splitForStreaming(content);
            for (int i = 0; i < chunks.size(); i++) {
                StreamChatResponse response = StreamChatResponse.content(chunks.get(i), i);
                response.setId(responseId);
                response.setModel(modelKey);
                onNext.accept(response);
            }
        }

        private List<String> splitForStreaming(String content) {
            List<String> chunks = new ArrayList<>();
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                char current = content.charAt(i);
                buffer.append(current);
                boolean sentenceEnd = "。！？!?；;\n".indexOf(current) >= 0;
                if (buffer.length() >= 48 || (sentenceEnd && buffer.length() >= 16)) {
                    chunks.add(buffer.toString());
                    buffer.setLength(0);
                }
            }
            if (buffer.length() > 0) {
                chunks.add(buffer.toString());
            }
            return chunks;
        }
        
        /**
         * 转换消息格式
         * 支持多模态输入（文本+图像）
         */
        private List<ChatMessage> convertMessages(ChatRequest request) {
            List<ChatMessage> messages = new ArrayList<>();
            
            // 查找最后一条USER消息的索引
            int lastUserMessageIndex = -1;
            for (int idx = request.getMessages().size() - 1; idx >= 0; idx--) {
                if (request.getMessages().get(idx).getRole() == com.nexusvoice.domain.conversation.constant.MessageRole.USER) {
                    lastUserMessageIndex = idx;
                    break;
                }
            }
            
            for (int i = 0; i < request.getMessages().size(); i++) {
                com.nexusvoice.infrastructure.ai.model.ChatMessage msg = request.getMessages().get(i);
                switch (msg.getRole()) {
                    case SYSTEM:
                        messages.add(SystemMessage.from(msg.getContent()));
                        break;
                    case USER:
                        // 检查是否是最后一条USER消息且有图像URL
                        boolean isLastUserMessage = i == lastUserMessageIndex;
                        if (isLastUserMessage && request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
                            // 创建多模态消息（文本+图像）
                            List<Content> contents = new ArrayList<>();
                            
                            // 添加文本内容
                            if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                                contents.add(TextContent.from(msg.getContent()));
                            }
                            
                            // 添加图像内容（Base64格式或URL）
                            for (int imgIdx = 0; imgIdx < request.getImageUrls().size(); imgIdx++) {
                                String imageUrl = request.getImageUrls().get(imgIdx);
                                // 记录图片URL的前缀信息（不记录完整Base64以避免日志过大）
                                String prefix = imageUrl.length() > 50 ? imageUrl.substring(0, 50) + "..." : imageUrl;
                                log.debug("添加图像{}，格式：{}", imgIdx + 1, prefix);
                                contents.add(ImageContent.from(imageUrl));
                            }
                            
                            messages.add(UserMessage.from(contents));
                            log.info("✅ 创建多模态用户消息成功 - 文本内容：{}，图像数量：{}", 
                                msg.getContent() != null && !msg.getContent().isEmpty() ? "有" : "无",
                                request.getImageUrls().size());
                        } else {
                            // 纯文本消息
                            messages.add(UserMessage.from(msg.getContent()));
                        }
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
    
    /**
     * 动态AI图像生成服务内部类
     * 每个图像生成模型对应一个服务实例
     */
    private class DynamicAiImageService implements com.nexusvoice.infrastructure.ai.service.AiImageService {
        private final AiModel model;
        
        public DynamicAiImageService(AiModel model) {
            this.model = model;
        }
        
        @Override
        public com.nexusvoice.domain.image.model.ImageGenerationResult generateImage(
                com.nexusvoice.domain.image.model.ImageGenerationRequest request) {
            
            long startTime = System.currentTimeMillis();
            LocalDateTime requestTime = LocalDateTime.now();
            String requestId = UUID.randomUUID().toString();
            
            AiApiKey apiKey = null;
            AiApiCallLog callLog = null;
            
            try {
                // 1. 获取API密钥
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());
                
                // 2. 调用图像生成适配器
                com.nexusvoice.domain.image.model.ImageGenerationResult result = 
                        siliconFlowImageAdapter.generateImage(request, model, apiKey);
                
                // 3. 计算费用（按图片数量计费）
                int imageCount = result.getImageCount();
                BigDecimal costPerImage = model.getInputTokenPrice(); // 复用input_token_price字段存储每张图片成本
                BigDecimal cost = costPerImage.multiply(BigDecimal.valueOf(imageCount));
                
                // 4. 更新密钥使用统计（token数量用图片数量代替）
                apiKeyPoolManager.markSuccess(apiKey.getId(), imageCount, cost);
                
                // 5. 记录调用日志
                callLog = AiApiCallLog.success(
                        apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                        request.getUserId(), null, // 图像生成通常不在对话上下文中
                        requestId, requestTime,
                        (int)(System.currentTimeMillis() - startTime),
                        0, imageCount, // promptTokens=0, completionTokens=imageCount
                        cost
                );
                com.nexusvoice.infrastructure.ai.util.CallLogContextEnricher.enrich(callLog);
                callLogRepository.save(callLog);
                
                log.info("图像生成成功，模型：{}，图片数量：{}，费用：{}元", 
                        model.getModelKey(), imageCount, cost);
                
                return result;
                
            } catch (Exception e) {
                log.error("图像生成失败，模型：{}，错误：{}", model.getModelKey(), e.getMessage(), e);
                
                // 标记密钥失败
                if (apiKey != null) {
                    apiKeyPoolManager.markFailed(apiKey.getId());
                }
                
                // 记录失败日志
                if (apiKey != null) {
                    callLog = AiApiCallLog.failure(
                            apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                            request.getUserId(), null,
                            requestId, requestTime, e.getMessage()
                    );
                    com.nexusvoice.infrastructure.ai.util.CallLogContextEnricher.enrich(callLog);
                    callLogRepository.save(callLog);
                }
                
                throw e;
            }
        }
        
        @Override
        public boolean isModelAvailable() {
            return model.isEnabled() && 
                   apiKeyPoolManager.getAvailableKeyCount(model.getProviderCode(), model.getModelCode()) > 0;
        }
        
        @Override
        public String getModelName() {
            return model.getModelName();
        }
    }
    
    /**
     * 获取图像生成服务
     * 
     * @param providerCode 厂商代码
     * @param modelCode 模型代码
     * @return AI图像生成服务
     */
    public com.nexusvoice.infrastructure.ai.service.AiImageService getImageService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        DynamicAiImageService service = imageServiceMap.get(modelKey);
        
        if (service == null) {
            // 尝试动态加载
            Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
            if (modelOpt.isEmpty()) {
                throw new BizException(ErrorCodeEnum.DATA_NOT_FOUND, 
                    String.format("图像模型%s不存在", modelKey));
            }
            
            AiModel model = modelOpt.get();
            if (!model.isEnabled()) {
                throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                    String.format("图像模型%s已禁用", modelKey));
            }
            
            if (!model.isImageModel()) {
                throw new BizException(ErrorCodeEnum.PARAM_ERROR, 
                    String.format("模型%s不是图像生成模型", modelKey));
            }
            
            service = new DynamicAiImageService(model);
            imageServiceMap.put(modelKey, service);
            log.info("动态加载图像生成服务：{}", modelKey);
        }
        
        return service;
    }
    
    /**
     * 根据模型键获取图像生成服务
     */
    public com.nexusvoice.infrastructure.ai.service.AiImageService getImageServiceByModelKey(String modelKey) {
        if (modelKey == null || !modelKey.contains(":")) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "无效的模型键：" + modelKey);
        }
        
        String[] parts = modelKey.split(":", 2);
        return getImageService(parts[0], parts[1]);
    }
    
    /**
     * 获取所有可用的图像生成模型信息
     */
    public List<AiModel> getAvailableImageModels() {
        return imageServiceMap.values().stream()
                .map(service -> service.model)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取ASR服务
     * 
     * @param providerCode 厂商代码
     * @param modelCode 模型代码
     * @return AI ASR服务
     */
    public com.nexusvoice.infrastructure.ai.service.AiAsrService getAsrService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        DynamicAiAsrService service = asrServiceMap.get(modelKey);
        
        if (service == null) {
            // 尝试动态加载
            Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
            if (modelOpt.isEmpty()) {
                throw new BizException(ErrorCodeEnum.DATA_NOT_FOUND, 
                    String.format("ASR模型%s不存在", modelKey));
            }
            
            AiModel model = modelOpt.get();
            if (!model.isEnabled()) {
                throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                    String.format("ASR模型%s已禁用", modelKey));
            }
            
            if (!model.isAsrModel()) {
                throw new BizException(ErrorCodeEnum.PARAM_ERROR, 
                    String.format("模型%s不是ASR模型", modelKey));
            }
            
            service = new DynamicAiAsrService(model);
            asrServiceMap.put(modelKey, service);
            log.info("动态加载ASR服务：{}", modelKey);
        }
        
        return service;
    }
    
    /**
     * 根据模型键获取ASR服务
     */
    public com.nexusvoice.infrastructure.ai.service.AiAsrService getAsrServiceByModelKey(String modelKey) {
        if (modelKey == null || !modelKey.contains(":")) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "无效的模型键：" + modelKey);
        }
        
        String[] parts = modelKey.split(":", 2);
        return getAsrService(parts[0], parts[1]);
    }
    
    /**
     * 获取所有可用的ASR模型信息
     */
    public List<AiModel> getAvailableAsrModels() {
        return asrServiceMap.values().stream()
                .map(service -> service.model)
                .collect(Collectors.toList());
    }
    
    /**
     * 动态AI ASR服务内部类
     * 完全复用密钥池、费用统计、调用日志等基础设施
     */
    private class DynamicAiAsrService implements com.nexusvoice.infrastructure.ai.service.AiAsrService {
        private final AiModel model;
        
        public DynamicAiAsrService(AiModel model) {
            this.model = model;
        }
        
        @Override
        public com.nexusvoice.domain.audio.model.AudioTranscriptionResult transcribe(
                com.nexusvoice.domain.audio.model.AudioTranscriptionRequest request) {
            long startTime = System.currentTimeMillis();
            LocalDateTime requestTime = LocalDateTime.now();
            String requestId = UUID.randomUUID().toString();
            
            AiApiKey apiKey = null;
            AiApiCallLog callLog = null;
            
            try {
                // 1. 获取API密钥
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());
                
                // 2. 调用ASR适配器
                com.nexusvoice.domain.audio.model.AudioTranscriptionResult result = 
                        siliconFlowAsrAdapter.transcribe(request, model, apiKey);
                
                // 3. 计算费用（按音频时长计费）
                double audioDuration = result.getAudioDuration() != null ? result.getAudioDuration() : 0.0;
                int estimatedTokens = siliconFlowAsrAdapter.estimateTokenCount(audioDuration);
                BigDecimal cost = model.calculateCost(estimatedTokens, 0);
                
                // 4. 更新密钥使用统计
                apiKeyPoolManager.markSuccess(apiKey.getId(), estimatedTokens, cost);
                
                // 5. 记录调用日志
                callLog = AiApiCallLog.success(
                        apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                        request.getUserId(), null, // ASR通常不在对话上下文中
                        requestId, requestTime,
                        (int)(System.currentTimeMillis() - startTime),
                        estimatedTokens, 0, // promptTokens=estimatedTokens, completionTokens=0
                        cost
                );
                callLogRepository.save(callLog);
                
                DynamicAiModelBeanManager.log.info("ASR识别成功，模型：{}，音频时长：{}s，费用：{}元", 
                        model.getModelKey(), audioDuration, cost);
                
                return result;
                
            } catch (Exception e) {
                DynamicAiModelBeanManager.log.error("ASR识别失败，模型：{}，错误：{}", model.getModelKey(), e.getMessage(), e);
                
                // 标记密钥失败
                if (apiKey != null) {
                    apiKeyPoolManager.markFailed(apiKey.getId());
                }
                
                // 记录失败日志
                if (apiKey != null) {
                    callLog = AiApiCallLog.failure(
                            apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                            request.getUserId(), null,
                            requestId, requestTime, e.getMessage()
                    );
                    callLogRepository.save(callLog);
                }
                
                throw e;
            }
        }
        
        @Override
        public boolean isModelAvailable() {
            return model.isEnabled() && 
                   apiKeyPoolManager.getAvailableKeyCount(model.getProviderCode(), model.getModelCode()) > 0;
        }
        
        @Override
        public String getModelName() {
            return model.getModelName();
        }
    }
    
    /**
     * 获取TTS服务
     * 
     * @param providerCode 厂商代码
     * @param modelCode 模型代码
     * @return AI TTS服务
     */
    public com.nexusvoice.infrastructure.ai.service.AiTtsService getTtsService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        DynamicAiTtsService service = ttsServiceMap.get(modelKey);
        
        if (service == null) {
            // 尝试动态加载
            Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
            if (modelOpt.isEmpty()) {
                throw new BizException(ErrorCodeEnum.DATA_NOT_FOUND, 
                    String.format("TTS模型%s不存在", modelKey));
            }
            
            AiModel model = modelOpt.get();
            if (!model.isEnabled()) {
                throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                    String.format("TTS模型%s已禁用", modelKey));
            }
            
            if (!model.isTtsModel()) {
                throw new BizException(ErrorCodeEnum.PARAM_ERROR, 
                    String.format("模型%s不是TTS模型", modelKey));
            }
            
            service = new DynamicAiTtsService(model);
            ttsServiceMap.put(modelKey, service);
            log.info("动态加载TTS服务：{}", modelKey);
        }
        
        return service;
    }
    
    /**
     * 根据模型键获取TTS服务
     */
    public com.nexusvoice.infrastructure.ai.service.AiTtsService getTtsServiceByModelKey(String modelKey) {
        if (modelKey == null || !modelKey.contains(":")) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "无效的模型键：" + modelKey);
        }
        
        String[] parts = modelKey.split(":", 2);
        return getTtsService(parts[0], parts[1]);
    }
    
    /**
     * 获取所有可用的TTS模型信息
     */
    public List<AiModel> getAvailableTtsModels() {
        return ttsServiceMap.values().stream()
                .map(service -> service.model)
                .collect(Collectors.toList());
    }
    
    /**
     * 动态AI TTS服务内部类
     * 完全复用密钥池、费用统计、调用日志等基础设施
     */
    private class DynamicAiTtsService implements com.nexusvoice.infrastructure.ai.service.AiTtsService {
        private final AiModel model;
        
        public DynamicAiTtsService(AiModel model) {
            this.model = model;
        }
        
        @Override
        public com.nexusvoice.domain.tts.model.TtsResult synthesize(
                com.nexusvoice.domain.tts.model.TtsRequest request) {
            long startTime = System.currentTimeMillis();
            LocalDateTime requestTime = LocalDateTime.now();
            String requestId = UUID.randomUUID().toString();
            
            AiApiKey apiKey = null;
            AiApiCallLog callLog = null;
            
            try {
                // 1. 获取API密钥（完全复用密钥池）
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());
                
                // 2. 调用TTS适配器
                com.nexusvoice.domain.tts.model.TtsResult result = 
                        qiniuTtsAdapter.synthesize(request, model, apiKey);
                
                // 3. 计算费用（按字符数计费）
                int charCount = result.getCharCount() != null ? result.getCharCount() : 0;
                BigDecimal cost = model.calculateCost(charCount, 0);
                
                // 4. 更新密钥使用统计
                apiKeyPoolManager.markSuccess(apiKey.getId(), charCount, cost);
                
                // 5. 记录调用日志
                callLog = AiApiCallLog.success(
                        apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                        request.getUserId(), request.getConversationId(),
                        requestId, requestTime,
                        (int)(System.currentTimeMillis() - startTime),
                        charCount, 0, // promptTokens=charCount, completionTokens=0
                        cost
                );
                callLogRepository.save(callLog);
                
                DynamicAiModelBeanManager.log.info("TTS合成成功，模型：{}，字符数：{}，费用：{}元", 
                        model.getModelKey(), charCount, cost);
                
                return result;
                
            } catch (Exception e) {
                DynamicAiModelBeanManager.log.error("TTS合成失败，模型：{}，错误：{}", model.getModelKey(), e.getMessage(), e);
                
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
                
                throw e;
            }
        }
        
        @Override
        public boolean isModelAvailable() {
            return model.isEnabled() && 
                   apiKeyPoolManager.getAvailableKeyCount(model.getProviderCode(), model.getModelCode()) > 0;
        }
        
        @Override
        public String getModelName() {
            return model.getModelName();
        }
    }
    
    /**
     * 获取视频生成服务
     */
    public DynamicAiVideoService getVideoService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        DynamicAiVideoService service = videoServiceMap.get(modelKey);
        
        if (service == null) {
            log.info("视频服务未加载，尝试动态加载：{}", modelKey);
            Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
            if (modelOpt.isEmpty()) {
                throw BizException.of(ErrorCodeEnum.DATA_NOT_FOUND, 
                    String.format("视频生成模型%s不存在", modelKey));
            }
            
            AiModel model = modelOpt.get();
            if (!model.isEnabled()) {
                throw BizException.of(ErrorCodeEnum.AI_SERVICE_ERROR, 
                    String.format("视频生成模型%s已禁用", modelKey));
            }
            
            if (!model.isVideoModel()) {
                throw BizException.of(ErrorCodeEnum.PARAM_ERROR, 
                    String.format("模型%s不是视频生成模型", modelKey));
            }
            
            service = new DynamicAiVideoService(model);
            videoServiceMap.put(modelKey, service);
            log.info("动态加载视频生成服务：{}", modelKey);
        }
        
        return service;
    }
    
    /**
     * 根据模型键获取视频生成服务
     */
    public DynamicAiVideoService getVideoServiceByModelKey(String modelKey) {
        if (modelKey == null || !modelKey.contains(":")) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "无效的模型键：" + modelKey);
        }
        
        String[] parts = modelKey.split(":", 2);
        return getVideoService(parts[0], parts[1]);
    }
    
    /**
     * 获取所有可用的视频生成模型信息
     */
    public List<AiModel> getAvailableVideoModels() {
        return videoServiceMap.values().stream()
                .map(service -> service.model)
                .collect(Collectors.toList());
    }
    
    /**
     * 动态AI视频生成服务内部类
     * 完全复用密钥池、费用统计、调用日志等基础设施
     */
    public class DynamicAiVideoService {
        private final AiModel model;
        
        public DynamicAiVideoService(AiModel model) {
            this.model = model;
        }
        
        /**
         * 提交视频生成任务（异步）
         */
        public com.nexusvoice.domain.video.model.VideoGenerationResult submitTask(
                com.nexusvoice.domain.video.model.VideoGenerationRequest request) {
            long startTime = System.currentTimeMillis();
            LocalDateTime requestTime = LocalDateTime.now();
            String requestId = UUID.randomUUID().toString();
            
            AiApiKey apiKey = null;
            AiApiCallLog callLog = null;
            
            try {
                // 1. 验证请求
                String validationError = request.validate();
                if (validationError != null) {
                    throw BizException.of(ErrorCodeEnum.PARAM_ERROR, validationError);
                }
                
                // 2. 获取API密钥
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());
                
                // 3. 获取适配器并提交任务
                com.nexusvoice.domain.video.repository.VideoRepository videoRepo = getVideoRepository();
                com.nexusvoice.domain.video.model.VideoGenerationResult result = 
                        videoRepo.submitTask(request, model, apiKey);
                
                // 4. 记录调用日志（提交阶段）
                callLog = AiApiCallLog.success(
                        apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                        request.getUserId(), request.getConversationId(),
                        requestId, requestTime,
                        (int)(System.currentTimeMillis() - startTime),
                        0, 0, BigDecimal.ZERO  // 提交阶段不计费
                );
                callLogRepository.save(callLog);
                
                DynamicAiModelBeanManager.log.info("视频生成任务提交成功，任务ID：{}，模型：{}", 
                        result.getTaskId(), model.getModelKey());
                return result;
                
            } catch (Exception e) {
                DynamicAiModelBeanManager.log.error("视频生成任务提交失败，模型：{}，错误：{}", 
                        model.getModelKey(), e.getMessage(), e);
                
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
                
                throw BizException.of(ErrorCodeEnum.VIDEO_GENERATION_FAILED, 
                        "视频生成任务提交失败：" + e.getMessage(), e);
            }
        }
        
        /**
         * 查询视频生成结果
         */
        public com.nexusvoice.domain.video.model.VideoGenerationResult queryTask(
                String taskId, 
                com.nexusvoice.domain.video.model.VideoGenerationRequest originalRequest) {
            long startTime = System.currentTimeMillis();
            LocalDateTime requestTime = LocalDateTime.now();
            
            AiApiKey apiKey = null;
            AiApiCallLog callLog = null;
            
            try {
                // 1. 获取API密钥
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());
                
                // 2. 查询任务结果
                com.nexusvoice.domain.video.repository.VideoRepository videoRepo = getVideoRepository();
                com.nexusvoice.domain.video.model.VideoGenerationResult result = 
                        videoRepo.queryTask(taskId, model, apiKey);
                
                // 3. 如果成功，计算费用并记录
                if (result.isSuccess() && result.getTokenUsage() != null) {
                    int promptTokens = result.getTokenUsage().getPromptTokens() != null 
                            ? result.getTokenUsage().getPromptTokens() : 0;
                    int completionTokens = result.getTokenUsage().getCompletionTokens() != null 
                            ? result.getTokenUsage().getCompletionTokens() : 0;
                    
                    BigDecimal cost = model.calculateCost(promptTokens, completionTokens);
                    
                    // 更新密钥使用统计
                    apiKeyPoolManager.markSuccess(apiKey.getId(), promptTokens + completionTokens, cost);
                    
                    // 记录调用日志
                    callLog = AiApiCallLog.success(
                            apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                            originalRequest.getUserId(), originalRequest.getConversationId(),
                            taskId, requestTime,
                            (int)(System.currentTimeMillis() - startTime),
                            promptTokens, completionTokens, cost
                    );
                    com.nexusvoice.infrastructure.ai.util.CallLogContextEnricher.enrich(callLog);
                    callLogRepository.save(callLog);
                    
                    DynamicAiModelBeanManager.log.info("视频生成成功，任务ID：{}，模型：{}，Token：{}，费用：{}元", 
                            taskId, model.getModelKey(), (promptTokens + completionTokens), cost);
                }
                
                return result;
                
            } catch (Exception e) {
                DynamicAiModelBeanManager.log.error("视频生成结果查询失败，任务ID：{}，错误：{}", 
                        taskId, e.getMessage(), e);
                
                if (apiKey != null) {
                    apiKeyPoolManager.markFailed(apiKey.getId());
                }
                
                // 记录失败日志
                if (apiKey != null && originalRequest != null) {
                    callLog = AiApiCallLog.failure(
                            apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                            originalRequest.getUserId(), originalRequest.getConversationId(),
                            taskId, requestTime, e.getMessage()
                    );
                    com.nexusvoice.infrastructure.ai.util.CallLogContextEnricher.enrich(callLog);
                    callLogRepository.save(callLog);
                }
                
                throw BizException.of(ErrorCodeEnum.VIDEO_QUERY_FAILED, 
                        "视频生成结果查询失败：" + e.getMessage(), e);
            }
        }
        
        /**
         * 获取视频仓储实现
         */
        private com.nexusvoice.domain.video.repository.VideoRepository getVideoRepository() {
            // 根据厂商代码选择对应的适配器
            if ("zhipu".equals(model.getProviderCode())) {
                if (zhipuVideoAdapter != null) {
                    return zhipuVideoAdapter;
                }
            }
            
            throw BizException.of(ErrorCodeEnum.VIDEO_MODEL_NOT_SUPPORTED, 
                    "不支持的视频生成模型：" + model.getModelKey());
        }
        
        /**
         * 检查模型是否可用
         */
        public boolean isModelAvailable() {
            return model.isEnabled() && 
                   apiKeyPoolManager.getAvailableKeyCount(model.getProviderCode(), model.getModelCode()) > 0;
        }
        
        /**
         * 获取模型名称
         */
        public String getModelName() {
            return model.getModelName();
        }
    }
    
    // ==================== Embedding服务相关方法 ====================
    
    /**
     * 获取Embedding服务
     */
    public AiEmbeddingService getEmbeddingService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        DynamicAiEmbeddingService service = embeddingServiceMap.get(modelKey);
        
        if (service == null) {
            // 尝试动态加载
            Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
            if (modelOpt.isEmpty()) {
                throw new BizException(ErrorCodeEnum.DATA_NOT_FOUND, 
                    String.format("Embedding模型%s不存在", modelKey));
            }
            
            AiModel model = modelOpt.get();
            if (!model.isEnabled()) {
                throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                    String.format("Embedding模型%s已禁用", modelKey));
            }
            
            service = new DynamicAiEmbeddingService(model);
            embeddingServiceMap.put(modelKey, service);
            log.info("动态加载Embedding模型：{}", modelKey);
        }
        
        return service;
    }
    
    /**
     * 根据模型键获取Embedding服务
     */
    public AiEmbeddingService getEmbeddingServiceByModelKey(String modelKey) {
        if (modelKey == null || !modelKey.contains(":")) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "无效的模型键：" + modelKey);
        }
        
        String[] parts = modelKey.split(":", 2);
        return getEmbeddingService(parts[0], parts[1]);
    }
    
    /**
     * 获取所有可用的Embedding模型信息
     */
    public List<AiModel> getAvailableEmbeddingModels() {
        return embeddingServiceMap.values().stream()
                .map(service -> service.model)
                .collect(Collectors.toList());
    }
    
    // ==================== Rerank服务相关方法 ====================
    
    /**
     * 获取Rerank服务
     */
    public AiRerankService getRerankService(String providerCode, String modelCode) {
        String modelKey = providerCode + ":" + modelCode;
        DynamicAiRerankService service = rerankServiceMap.get(modelKey);
        
        if (service == null) {
            // 尝试动态加载
            Optional<AiModel> modelOpt = modelRepository.findByProviderAndModel(providerCode, modelCode);
            if (modelOpt.isEmpty()) {
                throw new BizException(ErrorCodeEnum.DATA_NOT_FOUND, 
                    String.format("Rerank模型%s不存在", modelKey));
            }
            
            AiModel model = modelOpt.get();
            if (!model.isEnabled()) {
                throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, 
                    String.format("Rerank模型%s已禁用", modelKey));
            }
            
            service = new DynamicAiRerankService(model);
            rerankServiceMap.put(modelKey, service);
            log.info("动态加载Rerank模型：{}", modelKey);
        }
        
        return service;
    }
    
    /**
     * 根据模型键获取Rerank服务
     */
    public AiRerankService getRerankServiceByModelKey(String modelKey) {
        if (modelKey == null || !modelKey.contains(":")) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "无效的模型键：" + modelKey);
        }
        
        String[] parts = modelKey.split(":", 2);
        return getRerankService(parts[0], parts[1]);
    }
    
    /**
     * 获取所有可用的Rerank模型信息
     */
    public List<AiModel> getAvailableRerankModels() {
        return rerankServiceMap.values().stream()
                .map(service -> service.model)
                .collect(Collectors.toList());
    }
    
    // ==================== 动态服务内部类 ====================
    
    /**
     * 动态AI Embedding服务内部类
     * 完全复用密钥池、费用统计、调用日志等基础设施
     */
    public class DynamicAiEmbeddingService implements AiEmbeddingService {
        private final AiModel model;
        
        public DynamicAiEmbeddingService(AiModel model) {
            this.model = model;
        }
        
        @Override
        public EmbeddingResponse embed(EmbeddingRequest request) {
            long startTime = System.currentTimeMillis();
            LocalDateTime requestTime = LocalDateTime.now();
            String requestId = UUID.randomUUID().toString();
            
            AiApiKey apiKey = null;
            AiApiCallLog callLog = null;
            
            try {
                // 1. 获取API密钥
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());
                
                // 2. 调用Embedding服务
                com.nexusvoice.infrastructure.ai.adapter.EmbeddingAdapter adapter = getEmbeddingAdapter();
                EmbeddingResponse response = adapter.embed(request, model, apiKey);
                
                // 3. 计算费用
                int totalTokens = response.getTokenUsage() != null ? 
                        response.getTokenUsage().getTotalTokens() : 0;
                BigDecimal cost = model.calculateCost(totalTokens, 0);
                
                // 4. 记录调用日志
                Long userId = request.getUserId() != null ? Long.parseLong(request.getUserId().toString()) : null;
                Long bizId = request.getBizId() != null ? Long.parseLong(request.getBizId().toString()) : null;
                callLog = AiApiCallLog.success(
                        apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                        userId, bizId,
                        requestId, requestTime,
                        (int)(System.currentTimeMillis() - startTime),
                        totalTokens, 0, cost
                );
                callLogRepository.save(callLog);
                
                // 5. 标记密钥成功
                apiKeyPoolManager.markSuccess(apiKey.getId(), totalTokens, cost);
                
                return response;
                
            } catch (Exception e) {
                DynamicAiModelBeanManager.log.error("Embedding调用失败，模型：{}，错误：{}", 
                        model.getModelKey(), e.getMessage(), e);
                
                // 标记密钥失败
                if (apiKey != null) {
                    apiKeyPoolManager.markFailed(apiKey.getId());
                }
                
                // 记录失败日志
                if (apiKey != null) {
                    Long userId = request.getUserId() != null ? Long.parseLong(request.getUserId().toString()) : null;
                    Long bizId = request.getBizId() != null ? Long.parseLong(request.getBizId().toString()) : null;
                    callLog = AiApiCallLog.failure(
                            apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                            userId, bizId,
                            requestId, requestTime, e.getMessage()
                    );
                    callLogRepository.save(callLog);
                }
                
                return EmbeddingResponse.error("Embedding调用失败：" + e.getMessage());
            }
        }
        
        /**
         * 获取Embedding适配器
         */
        private com.nexusvoice.infrastructure.ai.adapter.EmbeddingAdapter getEmbeddingAdapter() {
            String providerCode = model.getProviderCode();
            
            // 根据提供商代码选择适配器
            if ("siliconflow".equalsIgnoreCase(providerCode)) {
                if (siliconFlowEmbeddingAdapter == null) {
                    throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                            "SiliconFlow Embedding适配器未初始化");
                }
                return siliconFlowEmbeddingAdapter;
            }
            
            // 未来扩展其他厂商：openai, azure等
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                    "不支持的Embedding提供商：" + providerCode);
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
            // 简单估算：1个中文字符≈1.5tokens，1个英文单词≈1.3tokens
            return (int)(text.length() * 1.5);
        }
    }
    
    /**
     * 动态AI Rerank服务内部类
     * 完全复用密钥池、费用统计、调用日志等基础设施
     */
    public class DynamicAiRerankService implements AiRerankService {
        private final AiModel model;
        
        public DynamicAiRerankService(AiModel model) {
            this.model = model;
        }
        
        @Override
        public RerankResponse rerank(RerankRequest request) {
            long startTime = System.currentTimeMillis();
            LocalDateTime requestTime = LocalDateTime.now();
            String requestId = UUID.randomUUID().toString();
            
            AiApiKey apiKey = null;
            AiApiCallLog callLog = null;
            
            try {
                // 1. 获取API密钥
                apiKey = apiKeyPoolManager.getNextApiKey(model.getProviderCode(), model.getModelCode());
                
                // 2. 调用Rerank服务
                com.nexusvoice.infrastructure.ai.adapter.RerankAdapter adapter = getRerankAdapter();
                RerankResponse response = adapter.rerank(request, model, apiKey);
                
                // 3. 计算费用
                int totalTokens = response.getTokenUsage() != null ? 
                        response.getTokenUsage().getTotalTokens() : 0;
                BigDecimal cost = model.calculateCost(totalTokens, 0);
                
                // 4. 记录调用日志
                Long userId = request.getUserId() != null ? Long.parseLong(request.getUserId().toString()) : null;
                Long bizId = request.getBizId() != null ? Long.parseLong(request.getBizId().toString()) : null;
                callLog = AiApiCallLog.success(
                        apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                        userId, bizId,
                        requestId, requestTime,
                        (int)(System.currentTimeMillis() - startTime),
                        totalTokens, 0, cost
                );
                callLogRepository.save(callLog);
                
                // 5. 标记密钥成功
                apiKeyPoolManager.markSuccess(apiKey.getId(), totalTokens, cost);
                
                return response;
                
            } catch (Exception e) {
                DynamicAiModelBeanManager.log.error("Rerank调用失败，模型：{}，错误：{}", 
                        model.getModelKey(), e.getMessage(), e);
                
                // 标记密钥失败
                if (apiKey != null) {
                    apiKeyPoolManager.markFailed(apiKey.getId());
                }
                
                // 记录失败日志
                if (apiKey != null) {
                    Long userId = request.getUserId() != null ? Long.parseLong(request.getUserId().toString()) : null;
                    Long bizId = request.getBizId() != null ? Long.parseLong(request.getBizId().toString()) : null;
                    callLog = AiApiCallLog.failure(
                            apiKey.getId(), model.getProviderCode(), model.getModelCode(),
                            userId, bizId,
                            requestId, requestTime, e.getMessage()
                    );
                    callLogRepository.save(callLog);
                }
                
                return RerankResponse.error("Rerank调用失败：" + e.getMessage());
            }
        }
        
        /**
         * 获取Rerank适配器
         */
        private com.nexusvoice.infrastructure.ai.adapter.RerankAdapter getRerankAdapter() {
            String providerCode = model.getProviderCode();
            
            // 根据提供商代码选择适配器
            if ("siliconflow".equalsIgnoreCase(providerCode)) {
                if (siliconFlowRerankAdapter == null) {
                    throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                            "SiliconFlow Rerank适配器未初始化");
                }
                return siliconFlowRerankAdapter;
            }
            
            // 未来扩展其他厂商：openai, cohere等
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                    "不支持的Rerank提供商：" + providerCode);
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
            // 简单估算：1个中文字符≈1.5tokens，1个英文单词≈1.3tokens
            return (int)(text.length() * 1.5);
        }
    }
}
