package com.nexusvoice.application.voice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.application.audio.service.AsrApplicationService;
import com.nexusvoice.application.conversation.dto.ChatRequestDto;
import com.nexusvoice.application.conversation.dto.ChatResponseDto;
import com.nexusvoice.application.conversation.dto.ConversationCreateRequest;
import com.nexusvoice.application.conversation.dto.ConversationCreateResponse;
import com.nexusvoice.application.conversation.dto.ConversationRuntimeConfigDto;
import com.nexusvoice.application.conversation.dto.ConversationRuntimePolicyDto;
import com.nexusvoice.application.conversation.service.ConversationApplicationService;
import com.nexusvoice.application.tts.dto.TTSRequestDTO;
import com.nexusvoice.application.tts.dto.TTSResponseDTO;
import com.nexusvoice.application.tts.service.TTSService;
import com.nexusvoice.application.voice.dto.VoiceSessionRuntimeConfigDto;
import com.nexusvoice.application.voice.dto.VoiceSessionRuntimeUpdateRequest;
import com.nexusvoice.application.voice.dto.VoiceSessionStartRequest;
import com.nexusvoice.application.voice.dto.VoiceSessionStartResponse;
import com.nexusvoice.application.voice.dto.VoiceTurnResultDto;
import com.nexusvoice.domain.conversation.constant.ConversationStatus;
import com.nexusvoice.domain.conversation.model.Conversation;
import com.nexusvoice.domain.conversation.repository.ConversationRepository;
import com.nexusvoice.domain.role.model.Role;
import com.nexusvoice.domain.role.repository.RoleRepository;
import com.nexusvoice.domain.audio.model.AudioTranscriptionResult;
import com.nexusvoice.domain.voice.enums.VoiceResponseMode;
import com.nexusvoice.domain.voice.enums.VoiceSessionState;
import com.nexusvoice.domain.voice.enums.VoiceTransportMode;
import com.nexusvoice.domain.voice.model.VoiceSession;
import com.nexusvoice.domain.voice.repository.VoiceSessionRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.MarkdownTextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 语音会话应用服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceSessionApplicationService {

    private final VoiceSessionRepository voiceSessionRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationApplicationService conversationApplicationService;
    private final AsrApplicationService asrApplicationService;
    private final TTSService ttsService;
    private final RoleRepository roleRepository;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, VoiceAudioUploadBuffer> audioUploadBuffers = new ConcurrentHashMap<>();

    @Value("${server.port:8081}")
    private int serverPort;

    @Value("${voice.tts.timeout-seconds:25}")
    private long voiceTtsTimeoutSeconds;

    @Transactional
    public VoiceSessionStartResponse startSession(VoiceSessionStartRequest request, Long userId) {
        Conversation conversation = resolveConversation(request, userId);
        Long effectiveRoleId = request.getRoleId() != null ? request.getRoleId() : conversation.getRoleId();
        validateRoleAccess(effectiveRoleId, userId);

        applyInitialRuntimeOverrides(conversation.getId(), request, userId);
        ConversationRuntimeConfigDto runtimeConfig = conversationApplicationService.getConversationRuntimeConfig(conversation.getId(), userId);

        VoiceSession voiceSession = new VoiceSession();
        voiceSession.setVoiceSessionId(UUID.randomUUID().toString());
        voiceSession.setConversationId(conversation.getId());
        voiceSession.setUserId(userId);
        voiceSession.setRoleId(effectiveRoleId);
        voiceSession.setTransportMode(VoiceTransportMode.WEBSOCKET_STREAM);
        voiceSession.setResponseMode(VoiceResponseMode.VOICE_CALL);
        voiceSession.setSelectedModel(conversation.getModelName());
        voiceSession.setSelectedVoiceType(request.getVoiceType());
        voiceSession.setSelectedAsrModel(resolveAsrModelKey(request.getAsrModelKey()));
        voiceSession.setKnowledgeBaseIds(toJson(request.getKnowledgeBaseIds()));
        voiceSession.setStrictMode(Boolean.TRUE.equals(request.getStrictMode()));
        boolean hasKnowledgeBases = request.getKnowledgeBaseIds() != null && !request.getKnowledgeBaseIds().isEmpty();
        voiceSession.setRagEnabled(Boolean.TRUE.equals(request.getRagEnabled()) && hasKnowledgeBases);
        voiceSession.setCompactEnabled(true);
        if (runtimeConfig != null && runtimeConfig.getPolicy() != null) {
            voiceSession.setShowThinking(runtimeConfig.getPolicy().getShowThinking());
            voiceSession.setThinkingMode(runtimeConfig.getPolicy().getThinkingMode());
            voiceSession.setContextStrategy(runtimeConfig.getPolicy().getContextStrategy());
            voiceSession.setRuntimeConfigSnapshot(toJson(runtimeConfig.getPolicy()));
        }
        voiceSession.start();
        voiceSessionRepository.save(voiceSession);

        return VoiceSessionStartResponse.builder()
                .id(voiceSession.getId())
                .voiceSessionId(voiceSession.getVoiceSessionId())
                .conversationId(voiceSession.getConversationId())
                .realtimeUrl("ws://localhost:" + serverPort + "/ws/voice/stream")
                .state(voiceSession.getState().name())
                .runtimeConfig(toRuntimeDto(voiceSession, runtimeConfig))
                .build();
    }

    public VoiceSessionRuntimeConfigDto getRuntimeConfig(String voiceSessionId, Long userId) {
        VoiceSession voiceSession = getVoiceSessionOrThrow(voiceSessionId, userId);
        ConversationRuntimeConfigDto runtimeConfig = conversationApplicationService.getConversationRuntimeConfig(
                voiceSession.getConversationId(),
                userId
        );
        return toRuntimeDto(voiceSession, runtimeConfig);
    }

    @Transactional
    public VoiceSessionRuntimeConfigDto updateRuntimeConfig(String voiceSessionId,
                                                            VoiceSessionRuntimeUpdateRequest request,
                                                            Long userId) {
        VoiceSession voiceSession = getVoiceSessionOrThrow(voiceSessionId, userId);

        if (request.getStrictMode() != null) {
            voiceSession.setStrictMode(request.getStrictMode());
        }
        if (request.getRagEnabled() != null) {
            voiceSession.setRagEnabled(request.getRagEnabled());
        }
        if (request.getVoiceType() != null && !request.getVoiceType().isBlank()) {
            voiceSession.setSelectedVoiceType(request.getVoiceType().trim());
        }
        if (request.getAsrModelKey() != null && !request.getAsrModelKey().isBlank()) {
            voiceSession.setSelectedAsrModel(request.getAsrModelKey().trim());
        }
        if (request.getKnowledgeBaseIds() != null) {
            voiceSession.setKnowledgeBaseIds(toJson(request.getKnowledgeBaseIds()));
        }
        if (request.getPolicy() != null) {
            ConversationRuntimeConfigDto runtimeConfig = conversationApplicationService.updateConversationRuntimeConfig(
                    voiceSession.getConversationId(),
                    request.getPolicy(),
                    userId
            );
            voiceSession.setRuntimeConfigSnapshot(toJson(runtimeConfig.getPolicy()));
            if (runtimeConfig.getPolicy() != null) {
                voiceSession.setShowThinking(runtimeConfig.getPolicy().getShowThinking());
                voiceSession.setThinkingMode(runtimeConfig.getPolicy().getThinkingMode());
                voiceSession.setContextStrategy(runtimeConfig.getPolicy().getContextStrategy());
            }
        }

        voiceSession.onUpdate();
        voiceSessionRepository.save(voiceSession);
        return getRuntimeConfig(voiceSessionId, userId);
    }

    @Transactional
    public VoiceSessionRuntimeConfigDto initializeRealtimeSession(String voiceSessionId,
                                                                  String clientCapabilities,
                                                                  Long userId) {
        VoiceSession voiceSession = getVoiceSessionOrThrow(voiceSessionId, userId);
        voiceSession.attachClientCapabilities(clientCapabilities);
        voiceSession.transitionTo(VoiceSessionState.READY);
        voiceSessionRepository.save(voiceSession);
        return getRuntimeConfig(voiceSessionId, userId);
    }

    @Transactional
    public VoiceTurnResultDto processTextUtterance(String voiceSessionId, String transcript, Long userId) {
        if (transcript == null || transcript.isBlank()) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "语音文本不能为空");
        }

        VoiceSession voiceSession = getVoiceSessionOrThrow(voiceSessionId, userId);
        voiceSession.transitionTo(VoiceSessionState.UNDERSTANDING);
        int turnNo = voiceSession.nextTurn();
        voiceSessionRepository.save(voiceSession);

        ConversationRuntimeConfigDto runtimeConfig = conversationApplicationService.getConversationRuntimeConfig(
                voiceSession.getConversationId(),
                userId
        );

        ChatRequestDto request = new ChatRequestDto();
        request.setConversationId(voiceSession.getConversationId());
        request.setMessage(transcript.trim());
        request.setModelName(voiceSession.getSelectedModel());
        request.setEnableAudio(false);
        List<Long> knowledgeBaseIds = parseKnowledgeBaseIds(voiceSession.getKnowledgeBaseIds());
        boolean enableRag = Boolean.TRUE.equals(voiceSession.getRagEnabled())
                && knowledgeBaseIds != null
                && !knowledgeBaseIds.isEmpty();
        request.setEnableRag(enableRag);
        request.setKnowledgeBaseIds(knowledgeBaseIds);
        request.setRagGroundingMode(Boolean.TRUE.equals(voiceSession.getStrictMode()) ? "STRICT" : "FLEXIBLE");
        request.setRoleId(voiceSession.getRoleId());
        if (runtimeConfig != null && runtimeConfig.getPolicy() != null) {
            request.setTemperature(runtimeConfig.getPolicy().getTemperature());
            request.setMaxTokens(runtimeConfig.getPolicy().getMaxTokens());
            request.setTopP(runtimeConfig.getPolicy().getTopP());
            request.setFrequencyPenalty(runtimeConfig.getPolicy().getFrequencyPenalty());
            request.setPresencePenalty(runtimeConfig.getPolicy().getPresencePenalty());
            request.setThinkingMode(runtimeConfig.getPolicy().getThinkingMode());
            request.setShowThinking(runtimeConfig.getPolicy().getShowThinking());
            request.setThinkingBudgetTokens(runtimeConfig.getPolicy().getThinkingBudgetTokens());
            request.setReasoningEffort(runtimeConfig.getPolicy().getReasoningEffort());
            request.setContextStrategy(runtimeConfig.getPolicy().getContextStrategy());
            request.setRecentTurnsToKeep(runtimeConfig.getPolicy().getRecentTurnsToKeep());
            request.setReservedOutputTokens(runtimeConfig.getPolicy().getReservedOutputTokens());
            request.setCompactTriggerRatio(runtimeConfig.getPolicy().getCompactTriggerRatio());
        }

        ChatResponseDto response = conversationApplicationService.chat(request, userId);
        if (Boolean.FALSE.equals(response.getSuccess())) {
            voiceSession.recordError("VOICE_CHAT_FAILED", response.getErrorMessage());
            voiceSessionRepository.save(voiceSession);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    response.getErrorMessage() != null ? response.getErrorMessage() : "语音轮次处理失败");
        }

        String spokenText = MarkdownTextUtils.cleanForTTS(response.getContent());
        List<VoiceTurnResultDto.AudioSegmentDto> audioSegments = generateVoiceAudioSegments(voiceSession, response.getContent(), spokenText);

        voiceSession.transitionTo(audioSegments.isEmpty() ? VoiceSessionState.READY : VoiceSessionState.RESPONDING_AUDIO);
        voiceSession.transitionTo(VoiceSessionState.READY);
        voiceSession.setRuntimeConfigSnapshot(runtimeConfig != null ? toJson(runtimeConfig.getPolicy()) : null);
        voiceSessionRepository.save(voiceSession);

        return VoiceTurnResultDto.builder()
                .turnNo(turnNo)
                .userTranscript(transcript.trim())
                .displayText(response.getContent())
                .spokenText(spokenText)
                .reasoningContent(response.getReasoningContent())
                .citations(response.getCitations())
                .audioSegments(audioSegments)
                .contextSnapshot(response.getContextSnapshot())
                .build();
    }

    public void appendAudioChunk(String voiceSessionId,
                                 String base64Audio,
                                 String filename,
                                 String contentType,
                                 Long userId) {
        if (base64Audio == null || base64Audio.isBlank()) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "音频分片不能为空");
        }

        VoiceSession voiceSession = getVoiceSessionOrThrow(voiceSessionId, userId);
        VoiceAudioUploadBuffer buffer = audioUploadBuffers.computeIfAbsent(
                voiceSessionId,
                ignored -> new VoiceAudioUploadBuffer(
                        filename != null && !filename.isBlank() ? filename : "voice-chunk.webm",
                        contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream"
                )
        );

        try {
            buffer.append(Base64.getDecoder().decode(base64Audio));
            voiceSession.transitionTo(VoiceSessionState.USER_SPEAKING);
            voiceSessionRepository.save(voiceSession);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "音频分片Base64不合法");
        }
    }

    @Transactional
    public VoiceTurnResultDto completeAudioInput(String voiceSessionId,
                                                 String filename,
                                                 String contentType,
                                                 String asrModelKey,
                                                 Long userId) {
        VoiceSession voiceSession = getVoiceSessionOrThrow(voiceSessionId, userId);
        VoiceAudioUploadBuffer buffer = audioUploadBuffers.remove(voiceSessionId);
        if (buffer == null || buffer.isEmpty()) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "没有可识别的音频内容");
        }

        String effectiveFileName = filename != null && !filename.isBlank() ? filename : buffer.filename;
        String effectiveContentType = contentType != null && !contentType.isBlank() ? contentType : buffer.contentType;
        String effectiveAsrModel = resolveAsrModelKey(asrModelKey != null && !asrModelKey.isBlank()
                ? asrModelKey
                : voiceSession.getSelectedAsrModel());

        AudioTranscriptionResult transcriptionResult = asrApplicationService.transcribe(
                effectiveAsrModel,
                new InMemoryMultipartFile(effectiveFileName, effectiveContentType, buffer.toByteArray()),
                userId
        );
        if (!transcriptionResult.isSuccess()) {
            throw new BizException(ErrorCodeEnum.AI_SERVICE_ERROR, "语音识别结果为空");
        }

        voiceSession.setSelectedAsrModel(effectiveAsrModel);
        voiceSession.transitionTo(VoiceSessionState.UNDERSTANDING);
        voiceSessionRepository.save(voiceSession);
        return processTextUtterance(voiceSessionId, transcriptionResult.getText(), userId);
    }

    @Transactional
    public void interrupt(String voiceSessionId, Long userId) {
        VoiceSession voiceSession = getVoiceSessionOrThrow(voiceSessionId, userId);
        voiceSession.transitionTo(VoiceSessionState.INTERRUPTING);
        voiceSession.transitionTo(VoiceSessionState.READY);
        voiceSessionRepository.save(voiceSession);
    }

    @Transactional
    public void endSession(String voiceSessionId, Long userId) {
        VoiceSession voiceSession = getVoiceSessionOrThrow(voiceSessionId, userId);
        voiceSession.end();
        audioUploadBuffers.remove(voiceSessionId);
        voiceSessionRepository.save(voiceSession);
    }

    public VoiceSession getVoiceSessionOrThrow(String voiceSessionId, Long userId) {
        VoiceSession voiceSession = voiceSessionRepository.findByVoiceSessionId(voiceSessionId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.NOT_FOUND, "语音会话不存在"));
        if (!voiceSession.getUserId().equals(userId)) {
            throw new BizException(ErrorCodeEnum.FORBIDDEN, "无权访问该语音会话");
        }
        return voiceSession;
    }

    private Conversation resolveConversation(VoiceSessionStartRequest request, Long userId) {
        if (request.getConversationId() != null) {
            return conversationRepository.findByIdAndUserId(request.getConversationId(), userId)
                    .orElseThrow(() -> new BizException(ErrorCodeEnum.NOT_FOUND, "对话不存在"));
        }

        ConversationCreateRequest createRequest = new ConversationCreateRequest();
        createRequest.setTitle(request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle().trim() : "语音通话");
        createRequest.setModelName(request.getModelName());
        createRequest.setRoleId(request.getRoleId());
        createRequest.setEnableAudio(false);

        ConversationCreateResponse response = conversationApplicationService.createConversation(createRequest, userId);
        return conversationRepository.findByIdAndUserId(response.getConversationId(), userId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.NOT_FOUND, "创建语音会话时对话不存在"));
    }

    private void applyInitialRuntimeOverrides(Long conversationId, VoiceSessionStartRequest request, Long userId) {
        if (request.getThinkingMode() == null
                && request.getShowThinking() == null
                && request.getContextStrategy() == null
                && request.getTemperature() == null) {
            return;
        }

        ConversationRuntimePolicyDto policyDto = ConversationRuntimePolicyDto.builder()
                .thinkingMode(request.getThinkingMode())
                .showThinking(request.getShowThinking())
                .contextStrategy(request.getContextStrategy())
                .temperature(request.getTemperature())
                .build();
        conversationApplicationService.updateConversationRuntimeConfig(conversationId, policyDto, userId);
    }

    private void validateRoleAccess(Long roleId, Long userId) {
        if (roleId == null) {
            return;
        }
        Optional<Role> role = roleRepository.findById(roleId);
        if (role.isEmpty()) {
            throw new BizException(ErrorCodeEnum.NOT_FOUND, "角色不存在");
        }
        if (role.get().getUserId() != null && !role.get().getUserId().equals(userId)) {
            throw new BizException(ErrorCodeEnum.FORBIDDEN, "无权访问此角色");
        }
    }

    private VoiceSessionRuntimeConfigDto toRuntimeDto(VoiceSession voiceSession, ConversationRuntimeConfigDto runtimeConfig) {
        return VoiceSessionRuntimeConfigDto.builder()
                .id(voiceSession.getId())
                .voiceSessionId(voiceSession.getVoiceSessionId())
                .conversationId(voiceSession.getConversationId())
                .roleId(voiceSession.getRoleId())
                .state(voiceSession.getState() != null ? voiceSession.getState().name() : null)
                .modelName(voiceSession.getSelectedModel())
                .voiceType(voiceSession.getSelectedVoiceType())
                .asrModelKey(voiceSession.getSelectedAsrModel())
                .strictMode(voiceSession.getStrictMode())
                .ragEnabled(voiceSession.getRagEnabled())
                .knowledgeBaseIds(voiceSession.getKnowledgeBaseIds())
                .runtimeConfig(runtimeConfig)
                .build();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "语音会话配置序列化失败");
        }
    }

    private List<Long> parseKnowledgeBaseIds(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
            );
        } catch (JsonProcessingException e) {
            log.warn("解析语音会话知识库ID失败: {}", e.getMessage());
            return null;
        }
    }

    private String resolveAsrModelKey(String asrModelKey) {
        if (asrModelKey == null || asrModelKey.isBlank()) {
            return "siliconflow:telespeech-asr";
        }
        return asrModelKey.trim();
    }

    private List<VoiceTurnResultDto.AudioSegmentDto> generateVoiceAudioSegments(VoiceSession voiceSession,
                                                                                String content,
                                                                                String spokenText) {
        TTSResponseDTO fallbackTts = generateFallbackTts(voiceSession, content);
        List<VoiceTurnResultDto.AudioSegmentDto> audioSegments = new ArrayList<>();
        if (fallbackTts == null) {
            return audioSegments;
        }
        if (fallbackTts.getSegments() != null && !fallbackTts.getSegments().isEmpty()) {
            for (int i = 0; i < fallbackTts.getSegments().size(); i++) {
                TTSResponseDTO.Segment segment = fallbackTts.getSegments().get(i);
                audioSegments.add(VoiceTurnResultDto.AudioSegmentDto.builder()
                        .segmentIndex(i)
                        .displayText(segment.getText())
                        .spokenText(MarkdownTextUtils.cleanForTTS(segment.getText()))
                        .audioUrl(segment.getUrl())
                        .isLast(i == fallbackTts.getSegments().size() - 1)
                        .build());
            }
        } else if (fallbackTts.getAudioData() != null && !fallbackTts.getAudioData().isBlank()) {
            audioSegments.add(VoiceTurnResultDto.AudioSegmentDto.builder()
                    .segmentIndex(0)
                    .displayText(content)
                    .spokenText(spokenText)
                    .audioUrl(fallbackTts.getAudioData())
                    .isLast(true)
                    .build());
        }
        return audioSegments;
    }

    private TTSResponseDTO generateFallbackTts(VoiceSession voiceSession, String content) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                TTSRequestDTO request = new TTSRequestDTO();
                request.setText(MarkdownTextUtils.cleanForTTS(content));
                request.setVoiceType(
                        voiceSession.getSelectedVoiceType() != null && !voiceSession.getSelectedVoiceType().isBlank()
                                ? voiceSession.getSelectedVoiceType()
                                : "qiniu_zh_female_wwxkjx"
                );
                request.setEncoding("mp3");
                request.setSpeedRatio(1.0);
                try {
                    return ttsService.textToSpeech(request);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get(voiceTtsTimeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.warn("语音会话TTS兜底失败: sessionId={}, error={}", voiceSession.getVoiceSessionId(), ex.getMessage());
            return null;
        }
    }

    private static final class VoiceAudioUploadBuffer {
        private final String filename;
        private final String contentType;
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        private VoiceAudioUploadBuffer(String filename, String contentType) {
            this.filename = filename;
            this.contentType = contentType;
        }

        private synchronized void append(byte[] chunk) {
            try {
                outputStream.write(chunk);
            } catch (IOException e) {
                throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "缓存音频分片失败");
            }
        }

        private synchronized byte[] toByteArray() {
            return outputStream.toByteArray();
        }

        private synchronized boolean isEmpty() {
            return outputStream.size() == 0;
        }
    }

    private static final class InMemoryMultipartFile implements MultipartFile {
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        private InMemoryMultipartFile(String originalFilename, String contentType, byte[] content) {
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
