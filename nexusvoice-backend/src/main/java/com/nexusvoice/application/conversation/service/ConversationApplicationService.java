package com.nexusvoice.application.conversation.service;

import com.nexusvoice.application.conversation.dto.ChatRequestDto;
import com.nexusvoice.application.conversation.dto.ChatResponseDto;
import com.nexusvoice.application.conversation.dto.ConversationListDto;
import com.nexusvoice.application.role.service.RoleApplicationService;
import com.nexusvoice.application.tts.dto.TTSRequestDTO;
import com.nexusvoice.application.tts.dto.TTSResponseDTO;
import com.nexusvoice.application.tts.service.TTSService;
import com.nexusvoice.domain.conversation.model.Conversation;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.repository.ConversationRepository;
import com.nexusvoice.domain.conversation.repository.ConversationMessageRepository;
import com.nexusvoice.domain.conversation.service.ConversationDomainService;
import com.nexusvoice.domain.role.model.Role;
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
    private final TTSService ttsService;
    private final RoleApplicationService roleApplicationService;

    public ConversationApplicationService(ConversationRepository conversationRepository,
                                        ConversationMessageRepository messageRepository,
                                        ConversationDomainService conversationDomainService,
                                        AiChatService aiChatService,
                                        TTSService ttsService,
                                        RoleApplicationService roleApplicationService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationDomainService = conversationDomainService;
        this.aiChatService = aiChatService;
        this.ttsService = ttsService;
        this.roleApplicationService = roleApplicationService;
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

            // 4. 查询角色信息（如果指定了角色ID）
            Role role = null;
            if (requestDto.getRoleId() != null) {
                try {
                    // 尝试获取角色信息，如果角色不存在或无权访问，不报错，继续正常聊天
                    role = roleApplicationService.getRoleForChat(requestDto.getRoleId(), userId);
                    log.info("使用角色进行聊天，角色ID：{}，角色名称：{}", role.getId(), role.getName());
                } catch (Exception e) {
                    log.warn("获取角色信息失败，角色ID：{}，用户ID：{}，错误：{}，将继续正常聊天", 
                            requestDto.getRoleId(), userId, e.getMessage());
                    // 不抛出异常，继续正常聊天流程
                }
            }

            // 5. 保存用户消息
            ConversationMessage userMessage = ConversationMessage.createUserMessage(
                    conversation.getId(), 
                    requestDto.getMessage(), 
                    messageRepository.getNextSequenceByConversationId(conversation.getId())
            );
            userMessage = conversationDomainService.addMessageToConversation(conversation.getId(), userMessage);

            // 6. 构建AI请求
            ChatRequest aiRequest = buildAiRequest(conversation, requestDto, role);

            // 7. 调用AI服务
            ChatResponse aiResponse = aiChatService.chat(aiRequest);

            if (aiResponse.getSuccess()) {
                // 8. 调用TTS服务生成音频
                String audioUrl = null;
                try {
                    TTSRequestDTO ttsRequest = new TTSRequestDTO();
                    ttsRequest.setText(aiResponse.getContent());
                    ttsRequest.setVoiceType("qiniu_zh_female_wwxkjx"); // 默认语音类型
                    ttsRequest.setEncoding("mp3"); // 默认音频格式
                    ttsRequest.setSpeedRatio(1.0); // 默认语速
                    
                    TTSResponseDTO ttsResponse = ttsService.textToSpeech(ttsRequest);
                    audioUrl = ttsResponse.getAudioData(); // TTSService返回的是音频URL
                    
                    log.info("TTS转换成功，对话ID：{}，音频URL：{}", conversation.getId(), audioUrl);
                } catch (Exception e) {
                    log.error("TTS转换失败，对话ID：{}，错误：{}", conversation.getId(), e.getMessage(), e);
                    // TTS失败不影响正常聊天流程，继续保存文本消息
                }

                // 9. 保存AI回复（包含音频URL）
                ConversationMessage aiMessage = ConversationMessage.createAssistantMessage(
                        conversation.getId(),
                        aiResponse.getContent(),
                        messageRepository.getNextSequenceByConversationId(conversation.getId()),
                        audioUrl
                );
                aiMessage.setTokenCount(aiResponse.getUsage() != null ? aiResponse.getUsage().getCompletionTokens() : 0);
                aiMessage = conversationDomainService.addMessageToConversation(conversation.getId(), aiMessage);

                // 10. 自动更新对话标题（如果是新对话且未设置标题）
                if (conversation.getTitle() == null || conversation.getTitle().equals("新对话")) {
                    String generatedTitle = conversationDomainService.generateConversationTitle(conversation.getId());
                    conversation.updateTitle(generatedTitle);
                    conversationRepository.save(conversation);
                }

                // 11. 构建响应
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
                        aiResponse.getResponseTimeMs(),
                        audioUrl
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
    private ChatRequest buildAiRequest(Conversation conversation, ChatRequestDto requestDto, Role role) {
        // 获取对话历史
        List<ConversationMessage> history = messageRepository.findByConversationIdOrderBySequence(conversation.getId());
        
        // 转换为AI请求格式
        List<ChatMessage> messages = new ArrayList<>();
        
        // 构建系统消息，集成角色信息
        String systemPrompt = buildSystemPrompt(conversation, requestDto, role);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(ChatMessage.system(systemPrompt));
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
                .build();
    }

    /**
     * 构建系统提示词，集成角色信息
     */
    private String buildSystemPrompt(Conversation conversation, ChatRequestDto requestDto, Role role) {
        StringBuilder systemPromptBuilder = new StringBuilder();
        
        // 1. 优先使用请求中的系统提示词
        if (requestDto.getSystemPrompt() != null && !requestDto.getSystemPrompt().trim().isEmpty()) {
            systemPromptBuilder.append(requestDto.getSystemPrompt().trim());
        }
        // 2. 其次使用对话中保存的系统提示词
        else if (conversation.getSystemPrompt() != null && !conversation.getSystemPrompt().trim().isEmpty()) {
            systemPromptBuilder.append(conversation.getSystemPrompt().trim());
        }
        // 3. 最后使用默认提示词
        else {
            systemPromptBuilder.append("你是一个有用的AI助手");
        }
        
        // 4. 如果指定了角色，集成角色的人设信息
        if (role != null) {
            systemPromptBuilder.append("\n\n");
            systemPromptBuilder.append("=== 角色设定 ===\n");
            
            // 添加角色描述
            if (role.getDescription() != null && !role.getDescription().trim().isEmpty()) {
                systemPromptBuilder.append("角色描述：").append(role.getDescription().trim()).append("\n");
            }
            
            // 添加角色人设提示词
            if (role.getPersonaPrompt() != null && !role.getPersonaPrompt().trim().isEmpty()) {
                systemPromptBuilder.append("人设要求：").append(role.getPersonaPrompt().trim()).append("\n");
            }
            
            systemPromptBuilder.append("请严格按照以上角色设定进行对话，保持角色的一致性。");
            
            log.info("集成角色信息到系统提示词，角色ID：{}，角色名称：{}", role.getId(), role.getName());
        }
        
        return systemPromptBuilder.toString();
    }
}
