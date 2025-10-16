package com.nexusvoice.infrastructure.ai.template;

import com.nexusvoice.domain.ai.model.AiModelInfo;
import com.nexusvoice.domain.ai.model.EnhancementContext;
import com.nexusvoice.domain.ai.enums.AiProviderEnum;
import com.nexusvoice.infrastructure.ai.chain.ChatEnhancementChain;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.infrastructure.ai.strategy.ChatStrategy;
import com.nexusvoice.infrastructure.ai.factory.ChatStrategyFactory;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.function.Consumer;

/**
 * AI聊天服务抽象模板类
 * 使用模板方法模式定义处理流程
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
public abstract class AbstractAiChatService implements AiChatService {
    
    @Autowired
    protected ChatStrategyFactory strategyFactory;
    
    @Autowired
    protected ChatEnhancementChain enhancementChain;
    
    /**
     * 模板方法：定义聊天处理的标准流程
     */
    @Override
    public final ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 验证请求
            validateRequest(request);
            
            // 2. 解析模型信息
            AiModelInfo modelInfo = parseModelInfo(request);
            
            // 3. 选择处理策略
            ChatStrategy strategy = selectStrategy(request, modelInfo);
            
            // 4. 创建增强上下文
            EnhancementContext context = createEnhancementContext(request);
            
            // 5. 执行请求增强链
            if (context.hasEnhancements()) {
                context = enhancementChain.enhance(context);
                request = context.getEnhancedRequest();
            }
            
            // 6. 执行具体的聊天处理
            ChatResponse response = executeChat(request, modelInfo, strategy);
            
            // 7. 后处理
            response = postProcessResponse(response, context);
            
            // 8. 记录指标
            long responseTime = System.currentTimeMillis() - startTime;
            response.setResponseTimeMs(responseTime);
            
            log.info("聊天处理完成，提供商：{}，模型：{}，耗时：{}ms，增强：{}", 
                    modelInfo.getProvider().getCode(), 
                    modelInfo.getModelCode(), 
                    responseTime,
                    context.hasEnhancements());
            
            return response;
            
        } catch (BizException e) {
            log.error("聊天业务异常，用户ID：{}，错误：{}", request.getUserId(), e.getMessage());
            return ChatResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("聊天系统异常，用户ID：{}", request.getUserId(), e);
            return ChatResponse.error("系统繁忙，请稍后重试");
        }
    }
    
    /**
     * 流式聊天（暂时保持原有逻辑，后续可重构）
     */
    @Override
    public void streamChat(ChatRequest request, 
                          Consumer<StreamChatResponse> onNext,
                          Consumer<Throwable> onError, 
                          Runnable onComplete) {
        try {
            // 验证请求
            validateRequest(request);
            
            // 解析模型信息
            AiModelInfo modelInfo = parseModelInfo(request);
            
            // 执行流式处理（由子类实现）
            doStreamChat(request, modelInfo, onNext, onError, onComplete);
            
        } catch (Exception e) {
            log.error("流式聊天启动失败，用户ID：{}", request.getUserId(), e);
            onError.accept(e);
        }
    }
    
    /**
     * 验证请求（钩子方法，子类可重写）
     */
    protected void validateRequest(ChatRequest request) {
        if (request == null) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "请求不能为空");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "消息列表不能为空");
        }
        if (request.getModel() == null || request.getModel().trim().isEmpty()) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "模型名称不能为空");
        }
    }
    
    /**
     * 解析模型信息
     * 期望格式：provider:model 如 "openai:gpt-oss-20b"
     */
    protected AiModelInfo parseModelInfo(ChatRequest request) {
        String modelStr = request.getModel();
        
        // 兼容旧格式（直接传模型名）
        if (!modelStr.contains(":")) {
            // 默认使用OpenAI
            return AiModelInfo.builder()
                    .provider(AiProviderEnum.OPENAI)
                    .modelCode(modelStr)
                    .modelName(modelStr)
                    .build();
        }
        
        // 新格式：provider:model
        String[] parts = modelStr.split(":", 2);
        String providerCode = parts[0];
        String modelCode = parts[1];
        
        AiProviderEnum provider = AiProviderEnum.fromCode(providerCode);
        if (provider == null) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "不支持的AI提供商：" + providerCode);
        }
        
        return AiModelInfo.builder()
                .provider(provider)
                .modelCode(modelCode)
                .modelName(modelCode)
                .build();
    }
    
    /**
     * 选择处理策略（钩子方法，子类可重写）
     */
    protected ChatStrategy selectStrategy(ChatRequest request, AiModelInfo modelInfo) {
        return strategyFactory.createStrategy(request, modelInfo);
    }
    
    /**
     * 创建增强上下文
     */
    protected EnhancementContext createEnhancementContext(ChatRequest request) {
        return EnhancementContext.builder()
                .originalRequest(request)
                .enhancedRequest(request)
                .enableWebSearch(request.getEnableWebSearch())
                .enableRag(request.getEnableRag())
                .enableMultiModal(request.getEnableMultiModal())
                .build();
    }
    
    /**
     * 后处理响应（钩子方法，子类可重写）
     */
    protected ChatResponse postProcessResponse(ChatResponse response, EnhancementContext context) {
        // 添加增强信息到响应中（可选）
        if (context.hasEnhancements()) {
            if (context.getSearchResults() != null) {
                response.setMetadata("searchResults", context.getSearchResults());
            }
            if (context.getRagResults() != null) {
                response.setMetadata("ragResults", context.getRagResults());
            }
        }
        return response;
    }
    
    /**
     * 执行具体的聊天处理（抽象方法，必须由子类实现）
     */
    protected abstract ChatResponse executeChat(ChatRequest request, 
                                               AiModelInfo modelInfo, 
                                               ChatStrategy strategy);
    
    /**
     * 执行流式聊天（抽象方法，必须由子类实现）
     */
    protected abstract void doStreamChat(ChatRequest request,
                                        AiModelInfo modelInfo,
                                        Consumer<StreamChatResponse> onNext,
                                        Consumer<Throwable> onError,
                                        Runnable onComplete);
    
    /**
     * 获取提供商类型（抽象方法，必须由子类实现）
     */
    public abstract AiProviderEnum getProviderType();
}
