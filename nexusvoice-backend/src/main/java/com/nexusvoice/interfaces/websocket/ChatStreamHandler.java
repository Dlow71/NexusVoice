package com.nexusvoice.interfaces.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.application.conversation.dto.ChatRequestDto;
import com.nexusvoice.application.conversation.service.ConversationApplicationService;
import com.nexusvoice.domain.conversation.model.Conversation;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.repository.ConversationRepository;
import com.nexusvoice.domain.conversation.service.ConversationDomainService;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.StreamChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流式聊天WebSocket处理器
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Slf4j
@Component
public class ChatStreamHandler implements WebSocketHandler {

    private final AiChatService aiChatService;
    private final ConversationApplicationService conversationApplicationService;
    private final ConversationRepository conversationRepository;
    private final ConversationDomainService conversationDomainService;
    private final ObjectMapper objectMapper;
    
    // 存储会话信息
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    public ChatStreamHandler(AiChatService aiChatService,
                           ConversationApplicationService conversationApplicationService,
                           ConversationRepository conversationRepository,
                           ConversationDomainService conversationDomainService,
                           ObjectMapper objectMapper) {
        this.aiChatService = aiChatService;
        this.conversationApplicationService = conversationApplicationService;
        this.conversationRepository = conversationRepository;
        this.conversationDomainService = conversationDomainService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        
        log.info("WebSocket连接建立，会话ID：{}", sessionId);
        
        // 发送连接成功消息
        sendMessage(session, StreamChatResponse.builder()
                .type(StreamChatResponse.StreamMessageType.START)
                .delta("连接成功")
                .build());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        
        try {
            // 解析请求消息
            String payload = (String) message.getPayload();
            ChatRequestDto requestDto = objectMapper.readValue(payload, ChatRequestDto.class);
            
            log.info("收到流式聊天请求，会话ID：{}，对话ID：{}", sessionId, requestDto.getConversationId());
            
            // 从会话中获取用户ID（实际项目中应该从JWT中获取）
            Long userId = getUserIdFromSession(session);
            if (userId == null) {
                sendErrorMessage(session, "用户未认证");
                return;
            }
            
            // 处理流式聊天
            handleStreamChat(session, requestDto, userId);
            
        } catch (Exception e) {
            log.error("处理WebSocket消息失败，会话ID：{}", sessionId, e);
            sendErrorMessage(session, "处理消息失败：" + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("WebSocket传输错误，会话ID：{}", sessionId, exception);
        
        sendErrorMessage(session, "连接出现错误：" + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        activeSessions.remove(sessionId);
        
        log.info("WebSocket连接关闭，会话ID：{}，状态：{}", sessionId, closeStatus);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 处理流式聊天
     */
    private void handleStreamChat(WebSocketSession session, ChatRequestDto requestDto, Long userId) {
        try {
            // 1. 获取或创建对话
            Conversation conversation = getOrCreateConversation(requestDto, userId);
            
            // 2. 验证权限和限制
            conversationDomainService.validateConversationAccess(conversation.getId(), userId);
            conversationDomainService.checkMessageCountLimit(conversation.getId(), 100);
            conversationDomainService.checkTokenLimit(conversation.getId(), 50000);
            
            // 3. 保存用户消息
            ConversationMessage userMessage = ConversationMessage.createUserMessage(
                    conversation.getId(), 
                    requestDto.getMessage(),
                    getNextSequence(conversation.getId())
            );
            conversationDomainService.addMessageToConversation(conversation.getId(), userMessage);
            
            // 4. 构建AI请求
            ChatRequest aiRequest = buildStreamAiRequest(conversation, requestDto);
            
            // 5. 开始流式响应
            StringBuilder responseContent = new StringBuilder();
            
            aiChatService.streamChat(aiRequest,
                    // onNext - 处理流式数据
                    (streamResponse) -> {
                        try {
                            if (streamResponse.getType() == StreamChatResponse.StreamMessageType.CONTENT && 
                                streamResponse.getDelta() != null) {
                                responseContent.append(streamResponse.getDelta());
                            }
                            sendMessage(session, streamResponse);
                        } catch (Exception e) {
                            log.error("发送流式响应失败", e);
                        }
                    },
                    // onError - 处理错误
                    (error) -> {
                        log.error("流式聊天出错，对话ID：{}", conversation.getId(), error);
                        sendErrorMessage(session, "AI响应出错：" + error.getMessage());
                    },
                    // onComplete - 完成处理
                    () -> {
                        try {
                            // 保存AI回复消息
                            if (responseContent.length() > 0) {
                                ConversationMessage aiMessage = ConversationMessage.createAssistantMessage(
                                        conversation.getId(),
                                        responseContent.toString(),
                                        getNextSequence(conversation.getId())
                                );
                                conversationDomainService.addMessageToConversation(conversation.getId(), aiMessage);
                                
                                log.info("流式聊天完成，对话ID：{}，响应长度：{}", 
                                        conversation.getId(), responseContent.length());
                            }
                        } catch (Exception e) {
                            log.error("保存AI回复消息失败", e);
                        }
                    }
            );
            
        } catch (BizException e) {
            log.error("流式聊天业务异常，用户ID：{}", userId, e);
            sendErrorMessage(session, e.getMessage());
        } catch (Exception e) {
            log.error("流式聊天系统异常，用户ID：{}", userId, e);
            sendErrorMessage(session, "系统繁忙，请稍后重试");
        }
    }

    /**
     * 发送消息到WebSocket
     */
    private void sendMessage(WebSocketSession session, StreamChatResponse response) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        StreamChatResponse errorResponse = StreamChatResponse.error(errorMessage);
        sendMessage(session, errorResponse);
    }

    /**
     * 从会话中获取用户ID（简化实现，实际应该从JWT中解析）
     */
    private Long getUserIdFromSession(WebSocketSession session) {
        // 这里简化实现，实际项目中应该从JWT token或者session中获取
        // 可以从连接时的查询参数或header中获取token
        Object userId = session.getAttributes().get("userId");
        if (userId != null) {
            return Long.valueOf(userId.toString());
        }
        
        // 从URI查询参数中获取（示例）
        String uri = session.getUri().toString();
        if (uri.contains("userId=")) {
            String userIdStr = uri.substring(uri.indexOf("userId=") + 7);
            if (userIdStr.contains("&")) {
                userIdStr = userIdStr.substring(0, userIdStr.indexOf("&"));
            }
            try {
                return Long.valueOf(userIdStr);
            } catch (NumberFormatException e) {
                log.warn("无效的用户ID格式：{}", userIdStr);
            }
        }
        
        return null;
    }

    /**
     * 获取或创建对话
     */
    private Conversation getOrCreateConversation(ChatRequestDto requestDto, Long userId) {
        if (requestDto.getConversationId() != null) {
            return conversationRepository.findByIdAndUserId(requestDto.getConversationId(), userId)
                    .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "对话不存在"));
        } else {
            String title = requestDto.getTitle() != null ? requestDto.getTitle() : "新对话";
            String modelName = requestDto.getModelName() != null ? requestDto.getModelName() : "gpt-4o-mini";
            String systemPrompt = requestDto.getSystemPrompt() != null ? requestDto.getSystemPrompt() : "你是一个有用的AI助手";
            
            return conversationDomainService.createConversation(userId, title, modelName, systemPrompt, requestDto.getRoleId());
        }
    }

    /**
     * 构建流式AI请求
     */
    private ChatRequest buildStreamAiRequest(Conversation conversation, ChatRequestDto requestDto) {
        // 获取对话历史（限制数量）
        List<ConversationMessage> history = conversationApplicationService.getConversationHistory(
                conversation.getId(), conversation.getUserId());
        
        List<ChatMessage> messages = new ArrayList<>();
        
        // 添加系统消息
        if (conversation.getSystemPrompt() != null) {
            messages.add(ChatMessage.system(conversation.getSystemPrompt()));
        }
        
        // 添加历史消息（最多20条）
        int maxHistory = 20;
        int startIndex = Math.max(0, history.size() - maxHistory);
        for (int i = startIndex; i < history.size(); i++) {
            ConversationMessage msg = history.get(i);
            switch (msg.getRole()) {
                case USER:
                    messages.add(ChatMessage.user(msg.getContent()));
                    break;
                case ASSISTANT:
                    messages.add(ChatMessage.assistant(msg.getContent()));
                    break;
                default:
                    break;
            }
        }
        
        // 添加当前消息
        messages.add(ChatMessage.user(requestDto.getMessage()));
        
        return ChatRequest.builder()
                .messages(messages)
                .model(requestDto.getModelName() != null ? requestDto.getModelName() : conversation.getModelName())
                .temperature(requestDto.getTemperature() != null ? requestDto.getTemperature() : 0.7)
                .maxTokens(requestDto.getMaxTokens() != null ? requestDto.getMaxTokens() : 2000)
                .stream(true)
                .userId(conversation.getUserId())
                .conversationId(conversation.getId())
                .build();
    }

    /**
     * 获取下一个消息序号
     */
    private Integer getNextSequence(Long conversationId) {
        // 这里简化实现，实际应该调用repository方法
        return 1; // 临时返回1，实际应该查询数据库
    }
}
