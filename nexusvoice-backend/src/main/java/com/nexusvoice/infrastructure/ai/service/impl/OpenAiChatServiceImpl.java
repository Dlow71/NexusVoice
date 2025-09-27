package com.nexusvoice.infrastructure.ai.service.impl;

import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * OpenAI聊天服务实现
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Slf4j
//@org.springframework.context.annotation.Primary
@Service
//@ConditionalOnBean({ChatLanguageModel.class, OpenAiStreamingChatModel.class})
public class OpenAiChatServiceImpl implements AiChatService {

    private final ChatLanguageModel chatLanguageModel;
    private final OpenAiStreamingChatModel streamingChatModel;

    @Autowired
    public OpenAiChatServiceImpl(ChatLanguageModel chatLanguageModel, 
                               OpenAiStreamingChatModel streamingChatModel) {
        this.chatLanguageModel = chatLanguageModel;
        this.streamingChatModel = streamingChatModel;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 转换消息格式
            List<dev.langchain4j.data.message.ChatMessage> messages = convertMessages(request.getMessages());
            
            // 调用LangChain4j进行聊天
            Response<AiMessage> response = chatLanguageModel.generate(messages);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (response != null && response.content() != null) {
                // 构建成功响应
                ChatResponse.TokenUsage usage = ChatResponse.TokenUsage.builder()
                        .promptTokens(response.tokenUsage() != null ? response.tokenUsage().inputTokenCount() : 0)
                        .completionTokens(response.tokenUsage() != null ? response.tokenUsage().outputTokenCount() : 0)
                        .totalTokens(response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : 0)
                        .build();

                return ChatResponse.success(
                        response.content().text(),
                        request.getModel(),
                        usage,
                        responseTime
                );
            } else {
                throw new BizException(ErrorCodeEnum.AI_RESPONSE_INVALID, "AI模型返回空响应");
            }
            
        } catch (Exception e) {
            log.error("AI聊天请求失败，用户ID：{}，对话ID：{}", request.getUserId(), request.getConversationId(), e);
            return ChatResponse.error("AI聊天请求失败：" + e.getMessage());
        }
    }

    @Override
    public void streamChat(ChatRequest request, Consumer<StreamChatResponse> onNext, 
                          Consumer<Throwable> onError, Runnable onComplete) {
        try {
            // 转换消息格式
            List<dev.langchain4j.data.message.ChatMessage> messages = convertMessages(request.getMessages());
            
            // 创建流式响应处理器
            AtomicInteger index = new AtomicInteger(0);
            AtomicReference<String> responseId = new AtomicReference<>("stream_" + System.currentTimeMillis());
            
            StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    try {
                        StreamChatResponse response = StreamChatResponse.content(
                                token, 
                                index.getAndIncrement()
                        );
                        response.setId(responseId.get());
                        response.setModel(request.getModel());
                        onNext.accept(response);
                    } catch (Exception e) {
                        log.error("处理流式响应时发生错误", e);
                        onError.accept(e);
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    try {
                        // 发送结束信号
                        StreamChatResponse endResponse = StreamChatResponse.end(
                                response.finishReason() != null ? response.finishReason().toString() : "stop"
                        );
                        onNext.accept(endResponse);
                        onComplete.run();
                    } catch (Exception e) {
                        log.error("完成流式响应时发生错误", e);
                        onError.accept(e);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("流式聊天请求失败，用户ID：{}，对话ID：{}", 
                            request.getUserId(), request.getConversationId(), throwable);
                    onError.accept(throwable);
                }
            };

            // 发送开始信号
            StreamChatResponse startResponse = StreamChatResponse.start(responseId.get(), request.getModel());
            onNext.accept(startResponse);
            
            // 开始流式请求
            streamingChatModel.generate(messages, handler);
            
        } catch (Exception e) {
            log.error("启动流式聊天请求失败，用户ID：{}，对话ID：{}", 
                    request.getUserId(), request.getConversationId(), e);
            onError.accept(e);
        }
    }

    @Override
    public String getModelName() {
        return "gpt-4o-mini";
    }

    @Override
    public boolean isModelAvailable() {
        try {
            // 简单的健康检查
            List<dev.langchain4j.data.message.ChatMessage> testMessages = List.of(
                    SystemMessage.from("你是一个AI助手"),
                    UserMessage.from("测试连接")
            );
            Response<AiMessage> response = chatLanguageModel.generate(testMessages);
            return response != null && response.content() != null;
        } catch (Exception e) {
            log.warn("AI模型可用性检查失败", e);
            return false;
        }
    }

    @Override
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单的令牌估算：大约4个字符=1个令牌（英文），中文可能更少
        // 这是一个粗略估算，实际应该使用tiktoken库
        return (int) Math.ceil(text.length() / 3.0);
    }

    /**
     * 转换消息格式为LangChain4j格式
     */
    private List<dev.langchain4j.data.message.ChatMessage> convertMessages(List<ChatMessage> messages) {
        List<dev.langchain4j.data.message.ChatMessage> langchainMessages = new ArrayList<>();
        
        for (ChatMessage message : messages) {
            switch (message.getRole()) {
                case SYSTEM:
                    langchainMessages.add(SystemMessage.from(message.getContent()));
                    break;
                case USER:
                    langchainMessages.add(UserMessage.from(message.getContent()));
                    break;
                case ASSISTANT:
                    langchainMessages.add(AiMessage.from(message.getContent()));
                    break;
                default:
                    // 忽略其他类型的消息
                    log.warn("忽略不支持的消息角色：{}", message.getRole());
                    break;
            }
        }
        
        return langchainMessages;
    }
}
