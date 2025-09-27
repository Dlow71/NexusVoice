package com.nexusvoice.application.conversation.service;

import com.nexusvoice.application.conversation.dto.ChatRequestDto;
import com.nexusvoice.application.conversation.dto.ChatResponseDto;
import com.nexusvoice.application.conversation.dto.ConversationListDto;
import com.nexusvoice.domain.conversation.model.Conversation;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.repository.ConversationRepository;
import com.nexusvoice.domain.conversation.repository.ConversationMessageRepository;
import com.nexusvoice.domain.conversation.service.ConversationDomainService;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话应用服务
 * 负责对话相关的业务流程编排
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Slf4j
@Service
public class ConversationApplicationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ConversationDomainService conversationDomainService;
    private final AiChatService aiChatService;

    public ConversationApplicationService(ConversationRepository conversationRepository,
                                        ConversationMessageRepository messageRepository,
                                        ConversationDomainService conversationDomainService,
                                        AiChatService aiChatService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationDomainService = conversationDomainService;
        this.aiChatService = aiChatService;
    }

    /**
     * 同步聊天
     */
    @Transactional
    public ChatResponseDto chat(ChatRequestDto requestDto, Long userId) {
        try {
            // 1. 获取或创建对话
            Conversation conversation = getOrCreateConversation(requestDto, userId);

            // 2. 验证权限
            conversationDomainService.validateConversationAccess(conversation.getId(), userId);

            // 3. 检查限制
            conversationDomainService.checkMessageCountLimit(conversation.getId(), 100); // 最大100条消息
            conversationDomainService.checkTokenLimit(conversation.getId(), 50000); // 最大5万令牌

            // 4. 保存用户消息
            ConversationMessage userMessage = ConversationMessage.createUserMessage(
                    conversation.getId(), 
                    requestDto.getMessage(), 
                    messageRepository.getNextSequenceByConversationId(conversation.getId())
            );
            userMessage = conversationDomainService.addMessageToConversation(conversation.getId(), userMessage);

            // 5. 构建AI请求
            ChatRequest aiRequest = buildAiRequest(conversation, requestDto);

            // 6. 调用AI服务
            ChatResponse aiResponse = aiChatService.chat(aiRequest);

            if (aiResponse.getSuccess()) {
                // 7. 保存AI回复
                ConversationMessage aiMessage = ConversationMessage.createAssistantMessage(
                        conversation.getId(),
                        aiResponse.getContent(),
                        messageRepository.getNextSequenceByConversationId(conversation.getId())
                );
                aiMessage.setTokenCount(aiResponse.getUsage() != null ? aiResponse.getUsage().getCompletionTokens() : 0);
                aiMessage = conversationDomainService.addMessageToConversation(conversation.getId(), aiMessage);

                // 8. 自动更新对话标题（如果是新对话且未设置标题）
                if (conversation.getTitle() == null || conversation.getTitle().equals("新对话")) {
                    String generatedTitle = conversationDomainService.generateConversationTitle(conversation.getId());
                    conversation.updateTitle(generatedTitle);
                    conversationRepository.save(conversation);
                }

                // 9. 构建响应
                ChatResponseDto.TokenUsageDto usageDto = null;
                if (aiResponse.getUsage() != null) {
                    usageDto = ChatResponseDto.TokenUsageDto.builder()
                            .promptTokens(aiResponse.getUsage().getPromptTokens())
                            .completionTokens(aiResponse.getUsage().getCompletionTokens())
                            .totalTokens(aiResponse.getUsage().getTotalTokens())
                            .build();
                }

                return ChatResponseDto.success(
                        conversation.getId(),
                        aiMessage.getId(),
                        aiResponse.getContent(),
                        aiResponse.getModel(),
                        usageDto,
                        aiResponse.getResponseTimeMs()
                );
            } else {
                log.error("AI聊天失败，对话ID：{}，错误：{}", conversation.getId(), aiResponse.getErrorMessage());
                return ChatResponseDto.error("AI聊天失败：" + aiResponse.getErrorMessage());
            }

        } catch (BizException e) {
            log.error("聊天业务异常，用户ID：{}", userId, e);
            return ChatResponseDto.error(e.getMessage());
        } catch (Exception e) {
            log.error("聊天系统异常，用户ID：{}", userId, e);
            return ChatResponseDto.error("系统繁忙，请稍后重试");
        }
    }

    /**
     * 获取用户的对话列表
     */
    public List<ConversationListDto> getUserConversations(Long userId, Integer limit) {
        List<Conversation> conversations = conversationRepository.findRecentByUserId(userId, limit != null ? limit : 20);
        
        return conversations.stream().map(conversation -> {
            // 获取最后一条消息
            ConversationMessage lastMessage = messageRepository.findLastMessageByConversationId(conversation.getId())
                    .orElse(null);
            
            // 获取消息数量
            Long messageCount = messageRepository.countByConversationId(conversation.getId());
            
            return ConversationListDto.builder()
                    .id(conversation.getId())
                    .title(conversation.getTitle())
                    .modelName(conversation.getModelName())
                    .status(conversation.getStatus().name())
                    .lastMessage(lastMessage != null ? 
                            (lastMessage.getContent().length() > 100 ? 
                                    lastMessage.getContent().substring(0, 100) + "..." : 
                                    lastMessage.getContent()) : 
                            null)
                    .messageCount(messageCount.intValue())
                    .lastActiveAt(conversation.getLastActiveAt())
                    .createdAt(conversation.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 获取对话历史
     */
    public List<ConversationMessage> getConversationHistory(Long conversationId, Long userId) {
        // 验证权限
        conversationDomainService.validateConversationAccess(conversationId, userId);
        
        return conversationDomainService.getConversationHistory(conversationId);
    }

    /**
     * 删除对话
     */
    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        // 验证权限
        conversationDomainService.validateConversationAccess(conversationId, userId);
        
        // 逻辑删除对话
        conversationRepository.logicalDeleteById(conversationId);
        
        log.info("用户删除对话成功，用户ID：{}，对话ID：{}", userId, conversationId);
    }

    /**
     * 获取或创建对话
     */
    private Conversation getOrCreateConversation(ChatRequestDto requestDto, Long userId) {
        if (requestDto.getConversationId() != null) {
            // 使用现有对话
            return conversationRepository.findByIdAndUserId(requestDto.getConversationId(), userId)
                    .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "对话不存在"));
        } else {
            // 创建新对话
            String title = requestDto.getTitle() != null ? requestDto.getTitle() : "新对话";
            String modelName = requestDto.getModelName() != null ? requestDto.getModelName() : "gpt-4o-mini";
            String systemPrompt = requestDto.getSystemPrompt() != null ? requestDto.getSystemPrompt() : "你是一个有用的AI助手";
            
            return conversationDomainService.createConversation(userId, title, modelName, systemPrompt);
        }
    }

    /**
     * 构建AI请求
     */
    private ChatRequest buildAiRequest(Conversation conversation, ChatRequestDto requestDto) {
        // 获取对话历史
        List<ConversationMessage> history = messageRepository.findByConversationIdOrderBySequence(conversation.getId());
        
        // 转换为AI请求格式
        List<ChatMessage> messages = new ArrayList<>();
        
        // 添加系统消息
        if (conversation.getSystemPrompt() != null && !conversation.getSystemPrompt().isEmpty()) {
            messages.add(ChatMessage.system(conversation.getSystemPrompt()));
        }
        
        // 添加历史消息（限制数量以避免超过令牌限制）
        int maxHistoryMessages = 20; // 最多包含20条历史消息
        int startIndex = Math.max(0, history.size() - maxHistoryMessages);
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
                    // 忽略其他类型
                    break;
            }
        }
        
        // 添加当前用户消息
        messages.add(ChatMessage.user(requestDto.getMessage()));
        
        // 构建请求
        return ChatRequest.builder()
                .messages(messages)
                .model(requestDto.getModelName() != null ? requestDto.getModelName() : conversation.getModelName())
                .temperature(requestDto.getTemperature() != null ? requestDto.getTemperature() : 0.7)
                .maxTokens(requestDto.getMaxTokens() != null ? requestDto.getMaxTokens() : 2000)
                .userId(conversation.getUserId())
                .conversationId(conversation.getId())
                .enableWebSearch(requestDto.getEnableWebSearch() != null ? requestDto.getEnableWebSearch() : false)
                .build();
    }
}
