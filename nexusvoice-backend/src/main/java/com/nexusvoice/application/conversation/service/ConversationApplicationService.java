package com.nexusvoice.application.conversation.service;

import com.nexusvoice.application.conversation.dto.ChatRequestDto;
import com.nexusvoice.application.conversation.dto.ChatResponseDto;
import com.nexusvoice.application.conversation.dto.ConversationListDto;
import com.nexusvoice.application.conversation.dto.ConversationCreateRequest;
import com.nexusvoice.application.conversation.dto.ConversationCreateResponse;
import com.nexusvoice.application.conversation.dto.ConversationMessageWithRoleDto;
import com.nexusvoice.application.conversation.assembler.ConversationAssembler;
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
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.utils.SecurityUtils;
import com.nexusvoice.utils.JwtUtils;
import com.nexusvoice.utils.MarkdownTextUtils;
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
            Long effectiveRoleId = requestDto.getRoleId() != null ? requestDto.getRoleId() : conversation.getRoleId();
            if (effectiveRoleId != null) {
                try {
                    // 尝试获取角色信息，如果角色不存在或无权访问，不报错，继续正常聊天
                    role = roleApplicationService.getRoleForChat(effectiveRoleId, userId);
                    log.info("使用角色进行聊天，角色ID：{}，角色名称：{}", role.getId(), role.getName());
                } catch (Exception e) {
                    log.warn("获取角色信息失败，角色ID：{}，用户ID：{}，错误：{}，将继续正常聊天", 
                            effectiveRoleId, userId, e.getMessage());
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
                // 8. 根据enableAudio参数决定是否调用TTS服务生成音频
                String audioUrl = null;
                boolean shouldGenerateAudio = requestDto.getEnableAudio() != null && requestDto.getEnableAudio();
                if (shouldGenerateAudio) {
                    try {
                        // 清理Markdown格式，使文本适合语音合成
                        String cleanedText = MarkdownTextUtils.cleanForTTS(aiResponse.getContent());
                        
                        TTSRequestDTO ttsRequest = new TTSRequestDTO();
                        ttsRequest.setText(cleanedText);
                        // 优先使用角色的语音类型，其次使用默认
                        String selectedVoiceType = (role != null && role.getVoiceType() != null && !role.getVoiceType().trim().isEmpty())
                                ? role.getVoiceType().trim()
                                : "qiniu_zh_female_wwxkjx";
                        ttsRequest.setVoiceType(selectedVoiceType);
                        ttsRequest.setEncoding("mp3"); // 默认音频格式
                        ttsRequest.setSpeedRatio(1.0); // 默认语速
                        
                        log.info("使用TTS语音类型：{}，对话ID：{}，{}", selectedVoiceType, conversation.getId(), 
                                MarkdownTextUtils.getCleaningStats(aiResponse.getContent(), cleanedText));
                        TTSResponseDTO ttsResponse = ttsService.textToSpeech(ttsRequest);
                        audioUrl = ttsResponse.getAudioData(); // TTSService返回的是音频URL
                        
                        log.info("TTS转换成功，对话ID：{}，音频URL：{}", conversation.getId(), audioUrl);
                    } catch (Exception e) {
                        log.error("TTS转换失败，对话ID：{}，错误：{}", conversation.getId(), e.getMessage(), e);
                        // TTS失败不影响正常聊天流程，继续保存文本消息
                    }
                } else {
                    log.debug("跳过TTS音频生成，enableAudio=false，对话ID：{}", conversation.getId());
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
            
            // 获取绑定的角色信息
            Role role = null;
            if (conversation.getRoleId() != null) {
                try {
                    role = roleApplicationService.getRoleForChat(conversation.getRoleId(), userId);
                } catch (Exception e) {
                    log.warn("获取对话绑定角色失败，对话ID：{}，角色ID：{}，错误：{}", 
                            conversation.getId(), conversation.getRoleId(), e.getMessage());
                }
            }
            
            // 构建最后消息预览
            String lastMessageContent = null;
            if (lastMessage != null) {
                lastMessageContent = lastMessage.getContent().length() > 100 ? 
                        lastMessage.getContent().substring(0, 100) + "..." : 
                        lastMessage.getContent();
            }
            
            // 使用转换器构建DTO
            return ConversationAssembler.toConversationListDto(conversation, role, lastMessageContent, messageCount.intValue());
        }).collect(Collectors.toList());
    }

    /**
     * 获取对话历史
     */
    public List<ConversationMessageWithRoleDto> getConversationHistory(Long conversationId, Long userId) {
        // 验证权限
        conversationDomainService.validateConversationAccess(conversationId, userId);
        
        // 获取对话信息
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.CONVERSATION_NOT_FOUND, "对话不存在"));
        
        // 获取绑定的角色信息
        Role role = null;
        if (conversation.getRoleId() != null) {
            try {
                role = roleApplicationService.getRoleForChat(conversation.getRoleId(), userId);
            } catch (Exception e) {
                log.warn("获取对话绑定角色失败，对话ID：{}，角色ID：{}，错误：{}", 
                        conversationId, conversation.getRoleId(), e.getMessage());
            }
        }
        
        // 获取消息历史
        List<ConversationMessage> messages = conversationDomainService.getConversationHistory(conversationId);
        
        // 转换为包含角色信息的DTO
        return ConversationAssembler.toConversationMessageWithRoleDtoList(messages, role);
    }

    /**
     * 获取对话历史（内部使用，返回原始ConversationMessage）
     * 用于WebSocket等不需要Role信息的场景
     */
    public List<ConversationMessage> getConversationHistoryForInternal(Long conversationId, Long userId) {
        // 验证权限
        conversationDomainService.validateConversationAccess(conversationId, userId);
        
        // 获取消息历史
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
     * 创建新对话
     */
    @Transactional
    public ConversationCreateResponse createConversation(ConversationCreateRequest request, Long userId) {
        String title = request.getTitle() != null && !request.getTitle().trim().isEmpty() ? request.getTitle().trim() : "新对话";
        String modelName = request.getModelName() != null && !request.getModelName().trim().isEmpty() ? request.getModelName().trim() : "gpt-4o-mini";
        String systemPrompt = request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty() ? request.getSystemPrompt().trim() : "你是一个有用的AI助手";

        Conversation conversation = conversationDomainService.createConversation(userId, title, modelName, systemPrompt, request.getRoleId());

        // 如果绑定了角色且角色有开场白，将开场白作为第一条助手消息保存
        if (request.getRoleId() != null) {
            try {
                Role role = roleApplicationService.getRoleForChat(request.getRoleId(), userId);
                if (role != null && role.getGreetingMessage() != null && !role.getGreetingMessage().trim().isEmpty()) {
                    // 根据enableAudio参数决定是否生成开场白音频
                    String greetingAudioUrl = role.getGreetingAudioUrl();
                    boolean shouldGenerateAudio = request.getEnableAudio() != null && request.getEnableAudio();
                    
                    if (shouldGenerateAudio && (greetingAudioUrl == null || greetingAudioUrl.trim().isEmpty())) {
                        try {
                            // 清理角色开场白的Markdown格式
                            String cleanedGreeting = MarkdownTextUtils.cleanForTTS(role.getGreetingMessage().trim());
                            
                            TTSRequestDTO ttsReq = new TTSRequestDTO();
                            ttsReq.setText(cleanedGreeting);
                            String selectedVoiceType = (role.getVoiceType() != null && !role.getVoiceType().trim().isEmpty())
                                    ? role.getVoiceType().trim()
                                    : "qiniu_zh_female_wwxkjx";
                            ttsReq.setVoiceType(selectedVoiceType);
                            ttsReq.setEncoding("mp3");
                            ttsReq.setSpeedRatio(1.0);
                            log.info("创建会话时为角色开场白生成TTS音频，使用语音类型：{}，角色ID：{}，{}", selectedVoiceType, role.getId(),
                                    MarkdownTextUtils.getCleaningStats(role.getGreetingMessage().trim(), cleanedGreeting));
                            TTSResponseDTO ttsRes = ttsService.textToSpeech(ttsReq);
                            if (ttsRes != null && ttsRes.getAudioData() != null && !ttsRes.getAudioData().trim().isEmpty()) {
                                greetingAudioUrl = ttsRes.getAudioData();
                            }
                        } catch (Exception ttsEx) {
                            log.warn("创建会话时生成角色开场白音频失败，角色ID：{}，错误：{}", role.getId(), ttsEx.getMessage());
                        }
                    } else if (!shouldGenerateAudio) {
                        // 如果不生成音频，清空音频URL
                        greetingAudioUrl = null;
                        log.debug("跳过角色开场白TTS生成，enableAudio=false，角色ID：{}", role.getId());
                    }

                    // 创建开场白消息
                    ConversationMessage greetingMessage = ConversationMessage.createAssistantMessage(
                            conversation.getId(),
                            role.getGreetingMessage().trim(),
                            messageRepository.getNextSequenceByConversationId(conversation.getId()),
                            greetingAudioUrl // 使用角色配置或生成的开场白音频URL
                    );
                    
                    // 保存开场白消息
                    conversationDomainService.addMessageToConversation(conversation.getId(), greetingMessage);
                    
                    log.info("为会话添加角色开场白消息，会话ID：{}，角色ID：{}，角色名称：{}", 
                            conversation.getId(), role.getId(), role.getName());
                }
            } catch (Exception e) {
                // 角色获取失败不影响会话创建，只记录日志
                log.warn("获取角色开场白失败，角色ID：{}，用户ID：{}，错误：{}，会话创建继续", 
                        request.getRoleId(), userId, e.getMessage());
            }
        }

        return ConversationCreateResponse.builder()
                .conversationId(conversation.getId())
                .title(conversation.getTitle())
                .modelName(conversation.getModelName())
                .systemPrompt(conversation.getSystemPrompt())
                .roleId(conversation.getRoleId())
                .createdAt(conversation.getCreatedAt())
                .build();
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
            
            return conversationDomainService.createConversation(userId, title, modelName, systemPrompt, requestDto.getRoleId());
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
                .enableWebSearch(requestDto.getEnableWebSearch() != null ? requestDto.getEnableWebSearch() : false)
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
