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
import com.nexusvoice.domain.config.service.SystemConfigService;
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
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
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
    private final DynamicAiModelBeanManager modelBeanManager;
    private final TTSService ttsService;
    private final RoleApplicationService roleApplicationService;
    private final SystemConfigService systemConfigService;
    private final ConversationResourceCleanupService resourceCleanupService;
    private final com.nexusvoice.domain.conversation.service.ConversationTitleGenerator titleGenerator;

    public ConversationApplicationService(ConversationRepository conversationRepository,
                                        ConversationMessageRepository messageRepository,
                                        ConversationDomainService conversationDomainService,
                                        DynamicAiModelBeanManager modelBeanManager,
                                        TTSService ttsService,
                                        RoleApplicationService roleApplicationService,
                                        SystemConfigService systemConfigService,
                                        ConversationResourceCleanupService resourceCleanupService,
                                        com.nexusvoice.domain.conversation.service.ConversationTitleGenerator titleGenerator) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationDomainService = conversationDomainService;
        this.modelBeanManager = modelBeanManager;
        this.ttsService = ttsService;
        this.roleApplicationService = roleApplicationService;
        this.systemConfigService = systemConfigService;
        this.resourceCleanupService = resourceCleanupService;
        this.titleGenerator = titleGenerator;
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
            conversationDomainService.checkMessageCountLimit(conversation.getId(), 
                    systemConfigService.getConversationMaxMessages()); // 从配置获取最大消息数
            conversationDomainService.checkTokenLimit(conversation.getId(), 
                    systemConfigService.getConversationMaxTokens()); // 从配置获取最大令牌数

            // 4. 验证模型一致性（方案一：会话级固定模型）
            // 如果请求中指定了模型，验证是否与会话绑定的模型一致
            if (requestDto.getModelName() != null && !requestDto.getModelName().trim().isEmpty()) {
                try {
                    conversation.validateModelConsistency(requestDto.getModelName());
                } catch (BizException e) {
                    // 模型不一致，向用户返回友好提示
                    log.info("会话{}模型一致性验证失败：{}", conversation.getId(), e.getMessage());
                    throw e; // 重新抛出异常，让外层处理
                }
            }

            // 5. 查询角色信息（如果指定了角色ID）
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

            // 6. 保存用户消息
            ConversationMessage userMessage = ConversationMessage.createUserMessage(
                    conversation.getId(), 
                    requestDto.getMessage(), 
                    null
            );
            userMessage = conversationDomainService.addMessageToConversation(conversation.getId(), userMessage);

            // 7. 构建AI请求
            ChatRequest aiRequest = buildAiRequest(conversation, requestDto, role);

            // 8. 调用AI服务
            // 解析模型信息并获取对应的服务
            String modelName = aiRequest.getModel();
            AiChatService aiChatService = getAiChatService(modelName);
            ChatResponse aiResponse = aiChatService.chat(aiRequest);

            if (aiResponse.getSuccess()) {
                // 9. 根据enableAudio参数决定是否调用TTS服务生成音频
                String audioUrl = null;
                TTSResponseDTO ttsResponse = null;
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
                        ttsResponse = ttsService.textToSpeech(ttsRequest);
                        audioUrl = ttsResponse.getAudioData(); // TTSService返回的是音频URL（分段时为首段）
                        
                        log.info("TTS转换成功，对话ID：{}，音频URL：{}", conversation.getId(), audioUrl);
                    } catch (Exception e) {
                        log.error("TTS转换失败，对话ID：{}，错误：{}", conversation.getId(), e.getMessage(), e);
                        // TTS失败不影响正常聊天流程，继续保存文本消息
                    }
                } else {
                    log.debug("跳过TTS音频生成，enableAudio=false，对话ID：{}", conversation.getId());
                }

                // 10. 保存AI回复（包含音频URL）
                ConversationMessage aiMessage = ConversationMessage.createAssistantMessage(
                        conversation.getId(),
                        aiResponse.getContent(),
                        null,
                        audioUrl
                );
                aiMessage.setTokenCount(aiResponse.getUsage() != null ? aiResponse.getUsage().getCompletionTokens() : 0);
                aiMessage = conversationDomainService.addMessageToConversation(conversation.getId(), aiMessage);

                // 11. 自动更新对话标题（如果是新对话且未设置标题）
                if (conversation.getTitle() == null || conversation.getTitle().equals("新对话")) {
                    String generatedTitle = conversationDomainService.generateConversationTitle(conversation.getId());
                    conversation.updateTitle(generatedTitle);
                    conversationRepository.save(conversation);
                }

                // 12. 构建响应
                ChatResponseDto.TokenUsageDto usageDto = null;
                if (aiResponse.getUsage() != null) {
                    usageDto = ChatResponseDto.TokenUsageDto.builder()
                            .promptTokens(aiResponse.getUsage().getPromptTokens())
                            .completionTokens(aiResponse.getUsage().getCompletionTokens())
                            .totalTokens(aiResponse.getUsage().getTotalTokens())
                            .build();
                }

                if (ttsResponse != null) {
                    // 保障单段TTS也返回统一的分段结构
                    if (ttsResponse.getSegments() == null || ttsResponse.getSegments().isEmpty()) {
                        List<TTSResponseDTO.Segment> segs = new ArrayList<>(1);
                        TTSResponseDTO.Segment seg = new TTSResponseDTO.Segment();
                        seg.setIndex(0);
                        seg.setText(ttsResponse.getText());
                        seg.setUrl(ttsResponse.getAudioData());
                        seg.setSize(ttsResponse.getAudioSize());
                        segs.add(seg);
                        ttsResponse.setSegments(segs);
                        ttsResponse.setChunked(false);
                    }
                    return ChatResponseDto.successWithTts(
                            conversation.getId(),
                            aiMessage.getId(),
                            aiResponse.getContent(),
                            aiResponse.getModel(),
                            usageDto,
                            aiResponse.getResponseTimeMs(),
                            ttsResponse
                    );
                } else {
                    return ChatResponseDto.success(
                            conversation.getId(),
                            aiMessage.getId(),
                            aiResponse.getContent(),
                            aiResponse.getModel(),
                            usageDto,
                            aiResponse.getResponseTimeMs(),
                            audioUrl
                    );
                }
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
     * 完整流程（注意顺序，避免竞态条件）：
     * 1. 验证权限
     * 2. 查询该对话的所有消息（在删除前先查询，避免丢失资源URL）
     * 3. 逻辑删除对话记录（conversations表）
     * 4. 物理删除该对话的所有消息（conversation_messages表）
     * 5. 异步清理CDN/MinIO上的资源文件（使用步骤2查询到的消息列表）
     */
    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        // 1. 验证权限
        conversationDomainService.validateConversationAccess(conversationId, userId);
        
        // 2. 查询该对话的所有消息（必须在删除之前查询，否则异步清理时找不到数据）
        List<ConversationMessage> messages = messageRepository.findByConversationId(conversationId);
        Long messageCount = (long) messages.size();
        
        // 3. 逻辑删除对话记录（conversations表）
        conversationRepository.logicalDeleteById(conversationId);
        
        // 4. 逻辑删除该对话的所有消息（conversation_messages表，软删除）
        // 使用软删除保留数据，方便审计和数据恢复
        messageRepository.logicalDeleteByConversationId(conversationId);
        
        log.info("用户删除对话成功，用户ID：{}，对话ID：{}，消息数量：{}，开始异步清理资源文件", 
                userId, conversationId, messageCount);
        
        // 5. 异步清理资源文件（传入已查询的消息列表，避免异步线程查询时数据已被删除）
        // 使用虚拟线程执行，不阻塞主线程，避免影响删除操作的响应速度
        resourceCleanupService.cleanupConversationResourcesAsync(conversationId, messages);
    }

    /**
     * 创建新对话
     */
    @Transactional
    public ConversationCreateResponse createConversation(ConversationCreateRequest request, Long userId) {
        String title = request.getTitle() != null && !request.getTitle().trim().isEmpty() 
                ? request.getTitle().trim() : systemConfigService.getDefaultConversationTitle();
        String modelName = request.getModelName() != null && !request.getModelName().trim().isEmpty() 
                ? request.getModelName().trim() : systemConfigService.getDefaultAiModel();
        String systemPrompt = request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty() 
                ? request.getSystemPrompt().trim() : systemConfigService.getDefaultSystemPrompt();

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
            // 创建新对话，所有默认值从系统配置获取
            String title = requestDto.getTitle() != null ? requestDto.getTitle() : systemConfigService.getDefaultConversationTitle();
            String modelName = requestDto.getModelName() != null ? requestDto.getModelName() : systemConfigService.getDefaultAiModel();
            String systemPrompt = requestDto.getSystemPrompt() != null ? requestDto.getSystemPrompt() : systemConfigService.getDefaultSystemPrompt();
            
            return conversationDomainService.createConversation(userId, title, modelName, systemPrompt, requestDto.getRoleId());
        }
    }

    /**
     * 获取AI聊天服务
     * 根据模型名称动态获取对应的服务实例
     */
    private AiChatService getAiChatService(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            // 从系统配置获取默认模型
            modelName = systemConfigService.getDefaultAiModel();
            log.debug("使用默认AI模型：{}", modelName);
        }
        
        // 兼容旧格式（没有provider前缀的）
        if (!modelName.contains(":")) {
            String defaultProvider = systemConfigService.getDefaultAiModelProvider();
            modelName = defaultProvider + ":" + modelName;
            log.debug("自动添加模型厂商前缀：{}", modelName);
        }
        
        return modelBeanManager.getServiceByModelKey(modelName);
    }

    /**
     * 构建AI请求
     */
    private ChatRequest buildAiRequest(Conversation conversation, ChatRequestDto requestDto, Role role) {
        // 获取对话历史（已包含刚保存的用户消息）
        List<ConversationMessage> history = messageRepository.findByConversationIdOrderBySequence(conversation.getId());

        // 转换为AI请求格式
        List<ChatMessage> messages = new ArrayList<>();
        
        // 构建系统消息，集成角色信息
        String systemPrompt = buildSystemPrompt(conversation, requestDto, role);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(ChatMessage.system(systemPrompt));
        }
        
        // 添加历史消息（动态截断，避免超额 token）
        addTrimmedHistory(messages, history, systemPrompt);
        
        // 构建请求
        String modelName = requestDto.getModelName() != null ? requestDto.getModelName() : conversation.getModelName();
        // 支持新的模型格式：provider:model，如果没有provider默认使用openai
        if (modelName != null && !modelName.contains(":")) {
            modelName = "openai:" + modelName;
        }
        
        return ChatRequest.builder()
                .messages(messages)
                .model(modelName)
                .temperature(requestDto.getTemperature() != null ? requestDto.getTemperature() : 0.7)
                .maxTokens(requestDto.getMaxTokens() != null ? requestDto.getMaxTokens() : 2000)
                .userId(conversation.getUserId())
                .conversationId(conversation.getId())
                .enableWebSearch(requestDto.getEnableWebSearch() != null ? requestDto.getEnableWebSearch() : false)
                .enableRag(requestDto.getEnableRag() != null ? requestDto.getEnableRag() : false)
                .knowledgeBaseIds(requestDto.getKnowledgeBaseIds())
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
        // 3. 最后使用系统配置中的默认提示词
        else {
            systemPromptBuilder.append(systemConfigService.getDefaultSystemPrompt());
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
        
        // 5. 添加全局回复风格要求
        systemPromptBuilder.append("\n\n");
        systemPromptBuilder.append("【重要】回复风格要求：请保持回答简洁精炼，直击要点，避免冗长的解释和不必要的铺垫。");
        
        return systemPromptBuilder.toString();
    }

    /**
     * 估算token数量
     * 简单的估算方法：平均3-4个字符一个token
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 中文字符通常占更多token，英文相对较少
        // 这里使用简单的估算：大约3个字符=1个token
        return (int) Math.ceil(text.length() / 3.0);
    }
    
    /**
     * 根据简单 token 预算从尾部选择历史消息，避免重复添加当前用户消息
     */
    private void addTrimmedHistory(List<ChatMessage> target, List<ConversationMessage> history, String systemPrompt) {
        if (history == null || history.isEmpty()) return;
        // 预估预算（粗略）：限制在 ~2500 tokens 的上下文（不含输出）
        int budget = 2500;
        int used = 0;
        if (systemPrompt != null) used += estimateTokenCount(systemPrompt);

        // 从尾到头累加，再正序加入，最多 20 条
        List<ConversationMessage> buffer = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0 && buffer.size() < 20; i--) {
            ConversationMessage msg = history.get(i);
            String content = msg.getContent();
            if (content == null || content.isEmpty()) continue;
            int t = estimateTokenCount(content);
            if (used + t > budget) break;
            used += t;
            buffer.add(msg);
        }
        // 反转为时间顺序
        for (int i = buffer.size() - 1; i >= 0; i--) {
            ConversationMessage msg = buffer.get(i);
            switch (msg.getRole()) {
                case com.nexusvoice.domain.conversation.constant.MessageRole.USER -> target.add(ChatMessage.user(msg.getContent()));
                case com.nexusvoice.domain.conversation.constant.MessageRole.ASSISTANT -> target.add(ChatMessage.assistant(msg.getContent()));
                default -> {}
            }
        }
    }

    /**
     * 从消息附件中提取图片URL列表
     * 符合DDD架构：应用层编排业务逻辑，不在接口层处理
     * 
     * @param message 消息对象
     * @return 图片URL列表，如果没有图片返回空列表
     */
    public List<String> extractImageUrlsFromMessage(ConversationMessage message) {
        List<String> imageUrls = new ArrayList<>();
        
        if (message == null || !message.hasAttachments()) {
            return imageUrls;
        }
        
        for (com.nexusvoice.domain.conversation.model.MessageAttachment attachment : message.getAttachments()) {
            if (attachment.isImage()) {
                imageUrls.add(attachment.getUrl());
            }
        }
        
        log.debug("从消息中提取了{}张图片用于多模态识别", imageUrls.size());
        return imageUrls;
    }

    /**
     * 生成对话标题
     * 使用AI分析对话内容并生成简洁标题
     *
     * @param conversationId 对话ID
     * @param userId 用户ID（用于权限验证）
     * @return 生成的标题
     */
    @Transactional
    public String generateConversationTitle(Long conversationId, Long userId) {
        log.info("开始生成对话标题，conversationId: {}, userId: {}", conversationId, userId);
        
        // 1. 验证权限
        conversationDomainService.validateConversationAccess(conversationId, userId);
        
        // 2. 获取对话
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.CONVERSATION_NOT_FOUND));
        
        // 3. 获取对话前几条消息（用于生成标题）
        List<ConversationMessage> messages = messageRepository
                .findByConversationIdOrderBySequence(conversationId)
                .stream()
                .limit(4)  // 只取前4条消息（2轮对话）
                .collect(Collectors.toList());
        
        // 4. 使用AI生成标题
        String generatedTitle = titleGenerator.generateTitle(messages);
        
        // 5. 更新对话标题
        conversation.updateTitle(generatedTitle);
        conversationRepository.save(conversation);
        
        log.info("对话标题生成成功，conversationId: {}, title: {}", conversationId, generatedTitle);
        return generatedTitle;
    }
}
