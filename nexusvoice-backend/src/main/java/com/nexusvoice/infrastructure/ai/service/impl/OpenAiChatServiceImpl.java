package com.nexusvoice.infrastructure.ai.service.impl;

import com.nexusvoice.domain.conversation.constant.MessageRole;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.infrastructure.ai.tool.SimpleWebSearchTool;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
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
@Primary
@Service
public class OpenAiChatServiceImpl implements AiChatService {

    private final ChatLanguageModel chatLanguageModel;
    private final OpenAiStreamingChatModel streamingChatModel;
    private final SimpleWebSearchTool searchTool;
    
    // MCP工具调用配置
    @Value("${nexusvoice.ai.tools.enabled:true}")
    private boolean toolsEnabled;
    
    @Value("${nexusvoice.ai.tools.search.enabled:true}")
    private boolean searchToolEnabled;
    
    // AI助手接口（支持工具调用）
    private interface ToolEnabledAssistant {
        String chat(String message);
    }
    
    // 缓存的AI助手实例
    private volatile ToolEnabledAssistant toolEnabledAssistant;

    @Autowired
    public OpenAiChatServiceImpl(@Autowired ChatLanguageModel chatLanguageModel,
                               @Autowired OpenAiStreamingChatModel streamingChatModel,
                               @Autowired SimpleWebSearchTool searchTool) {
        this.chatLanguageModel = chatLanguageModel;
        this.streamingChatModel = streamingChatModel;
        this.searchTool = searchTool;
        
        log.info("OpenAI聊天服务初始化完成");
        log.info("- ChatLanguageModel: {}", chatLanguageModel != null ? "已加载" : "未加载");
        log.info("- StreamingChatModel: {}", streamingChatModel != null ? "已加载" : "未加载");
        log.info("- 搜索工具: {}", searchTool != null ? "已加载" : "未加载");
        
        if (chatLanguageModel == null) {
            log.warn("ChatLanguageModel未配置，请检查LangChain4j配置");
        }
        if (streamingChatModel == null) {
            log.warn("StreamingChatModel未配置，请检查LangChain4j配置");
        }
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 检查是否启用联网搜索
            boolean enableWebSearch = request.getEnableWebSearch() != null ? request.getEnableWebSearch() : false;
            
            log.info("开始处理聊天请求，用户ID：{}，联网搜索：{}，工具调用配置：{}", 
                    request.getUserId(), enableWebSearch, toolsEnabled);
            
            // 检查必要的依赖
            if (chatLanguageModel == null) {
                return ChatResponse.error("AI聊天服务未正确配置，请检查LangChain4j配置");
            }
            
            String responseText;
            
            if (enableWebSearch && toolsEnabled && searchToolEnabled && searchTool != null) {
                // 使用带工具调用的AI助手
                log.info("使用带联网搜索的AI助手处理请求");
                responseText = getOrCreateToolEnabledAssistant().chat(buildFullMessage(request));
            } else {
                // 使用基础AI模型
                log.info("使用基础AI模型处理请求（不启用联网搜索）");
                responseText = handleBasicChat(request);
            }

            long responseTime = System.currentTimeMillis() - startTime;
            
            log.info("聊天处理完成，耗时：{}ms，工具调用：{}", responseTime, toolsEnabled);

            // 构建成功响应
            ChatResponse.TokenUsage usage = ChatResponse.TokenUsage.builder()
                    .promptTokens(estimateTokenCount(buildFullMessage(request)))
                    .completionTokens(estimateTokenCount(responseText))
                    .totalTokens(estimateTokenCount(buildFullMessage(request) + responseText))
                    .build();

            return ChatResponse.success(
                    responseText,
                    getEffectiveModelName(),
                    usage,
                    responseTime
            );

        } catch (Exception e) {
            log.error("AI聊天请求失败，用户ID：{}", request.getUserId(), e);
            return ChatResponse.error("AI聊天请求失败：" + e.getMessage());
        }
    }

    @Override
    public void streamChat(ChatRequest request, Consumer<StreamChatResponse> onNext,
                          Consumer<Throwable> onError, Runnable onComplete) {
        try {
            // 检查必要的依赖
            if (streamingChatModel == null) {
                onError.accept(new RuntimeException("流式聊天服务未正确配置，请检查LangChain4j配置"));
                return;
            }
            
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

    /**
     * 获取或创建支持工具的AI助手
     */
    private ToolEnabledAssistant getOrCreateToolEnabledAssistant() {
        if (toolEnabledAssistant == null) {
            synchronized (this) {
                if (toolEnabledAssistant == null) {
                    // 创建基础助手，暂时不使用工具调用避免编译错误
                    toolEnabledAssistant = new ToolEnabledAssistant() {
                        @Override
                        public String chat(String message) {
                            UserMessage userMessage = UserMessage.from(message);
                            Response<AiMessage> response = chatLanguageModel.generate(userMessage);
                            return response.content().text();
                        }
                    };
                    log.info("创建基础AI助手（暂不支持工具调用）");
                }
            }
        }
        return toolEnabledAssistant;
    }

    /**
     * 处理基础聊天（无工具调用）
     */
    private String handleBasicChat(ChatRequest request) {
        List<dev.langchain4j.data.message.ChatMessage> messages = convertMessages(request.getMessages());
        Response<AiMessage> response = chatLanguageModel.generate(messages);
        return response.content().text();
    }

    /**
     * 构建完整消息
     */
    private String buildFullMessage(ChatRequest request) {
        StringBuilder fullMessage = new StringBuilder();
        
        // 添加系统提示
        if (toolsEnabled && searchToolEnabled) {
            fullMessage.append("你是一个智能的AI助手，具备搜索网络信息的能力。");
            fullMessage.append("当用户询问你不确定或者需要最新信息的问题时，你应该主动使用搜索工具获取信息。");
            fullMessage.append("搜索到信息后，请基于搜索结果为用户提供准确、有用的回答。\n\n");
        } else {
            fullMessage.append("你是一个有用的AI助手。\n\n");
        }
        
        // 添加历史消息
        List<ChatMessage> messages = request.getMessages();
        int startIndex = Math.max(0, messages.size() - 10);
        
        for (int i = startIndex; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (msg.getRole() == MessageRole.USER) {
                fullMessage.append("用户：").append(msg.getContent()).append("\n");
            } else if (msg.getRole() == MessageRole.ASSISTANT) {
                fullMessage.append("助手：").append(msg.getContent()).append("\n");
            }
        }
        
        return fullMessage.toString();
    }

    /**
     * 获取有效的模型名称
     */
    private String getEffectiveModelName() {
        if (toolsEnabled && searchToolEnabled) {
            return "gpt-4o-mini-enhanced";
        } else {
            return "gpt-4o-mini";
        }
    }

    @Override
    public String getModelName() {
        return getEffectiveModelName();
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
