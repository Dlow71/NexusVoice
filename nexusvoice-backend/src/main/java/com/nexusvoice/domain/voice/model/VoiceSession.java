package com.nexusvoice.domain.voice.model;

import com.nexusvoice.domain.common.BaseDomainEntity;
import com.nexusvoice.domain.voice.enums.VoiceResponseMode;
import com.nexusvoice.domain.voice.enums.VoiceSessionState;
import com.nexusvoice.domain.voice.enums.VoiceTransportMode;

import java.time.LocalDateTime;

/**
 * 语音通话会话聚合根。
 */
public class VoiceSession extends BaseDomainEntity {

    private String voiceSessionId;

    private Long conversationId;

    private Long userId;

    private Long roleId;

    private VoiceSessionState state;

    private VoiceTransportMode transportMode;

    private VoiceResponseMode responseMode;

    private String selectedModel;

    private String selectedVoiceType;

    private String selectedAsrModel;

    private String knowledgeBaseIds;

    private Boolean strictMode;

    private Boolean ragEnabled;

    private Boolean compactEnabled;

    private Boolean showThinking;

    private String thinkingMode;

    private String contextStrategy;

    private String runtimeConfigSnapshot;

    private String clientCapabilities;

    private String sessionSummary;

    private Integer currentTurnNo;

    private String lastErrorCode;

    private String lastErrorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime lastActiveAt;

    private LocalDateTime endedAt;

    public void start() {
        onCreate();
        this.startedAt = getCreatedAt();
        this.lastActiveAt = getCreatedAt();
        this.state = VoiceSessionState.PREPARING;
        if (this.transportMode == null) {
            this.transportMode = VoiceTransportMode.WEBSOCKET_STREAM;
        }
        if (this.responseMode == null) {
            this.responseMode = VoiceResponseMode.VOICE_CALL;
        }
        if (this.strictMode == null) {
            this.strictMode = Boolean.TRUE;
        }
        if (this.ragEnabled == null) {
            this.ragEnabled = Boolean.TRUE;
        }
        if (this.compactEnabled == null) {
            this.compactEnabled = Boolean.TRUE;
        }
        if (this.showThinking == null) {
            this.showThinking = Boolean.FALSE;
        }
        if (this.thinkingMode == null || this.thinkingMode.isBlank()) {
            this.thinkingMode = "disabled";
        }
        if (this.contextStrategy == null || this.contextStrategy.isBlank()) {
            this.contextStrategy = "COMPACT";
        }
        if (this.currentTurnNo == null) {
            this.currentTurnNo = 0;
        }
    }

    public void transitionTo(VoiceSessionState newState) {
        this.state = newState;
        this.lastActiveAt = LocalDateTime.now();
        onUpdate();
    }

    public int nextTurn() {
        if (this.currentTurnNo == null) {
            this.currentTurnNo = 0;
        }
        this.currentTurnNo += 1;
        this.lastActiveAt = LocalDateTime.now();
        onUpdate();
        return this.currentTurnNo;
    }

    public void attachClientCapabilities(String clientCapabilities) {
        this.clientCapabilities = clientCapabilities;
        this.lastActiveAt = LocalDateTime.now();
        onUpdate();
    }

    public void recordError(String errorCode, String errorMessage) {
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
        this.state = VoiceSessionState.DEGRADED;
        this.lastActiveAt = LocalDateTime.now();
        onUpdate();
    }

    public void end() {
        this.state = VoiceSessionState.TERMINATED;
        this.endedAt = LocalDateTime.now();
        this.lastActiveAt = this.endedAt;
        onUpdate();
    }

    public String getVoiceSessionId() {
        return voiceSessionId;
    }

    public void setVoiceSessionId(String voiceSessionId) {
        this.voiceSessionId = voiceSessionId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public VoiceSessionState getState() {
        return state;
    }

    public void setState(VoiceSessionState state) {
        this.state = state;
    }

    public VoiceTransportMode getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(VoiceTransportMode transportMode) {
        this.transportMode = transportMode;
    }

    public VoiceResponseMode getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(VoiceResponseMode responseMode) {
        this.responseMode = responseMode;
    }

    public String getSelectedModel() {
        return selectedModel;
    }

    public void setSelectedModel(String selectedModel) {
        this.selectedModel = selectedModel;
    }

    public String getSelectedVoiceType() {
        return selectedVoiceType;
    }

    public void setSelectedVoiceType(String selectedVoiceType) {
        this.selectedVoiceType = selectedVoiceType;
    }

    public String getKnowledgeBaseIds() {
        return knowledgeBaseIds;
    }

    public void setKnowledgeBaseIds(String knowledgeBaseIds) {
        this.knowledgeBaseIds = knowledgeBaseIds;
    }

    public String getSelectedAsrModel() {
        return selectedAsrModel;
    }

    public void setSelectedAsrModel(String selectedAsrModel) {
        this.selectedAsrModel = selectedAsrModel;
    }

    public Boolean getStrictMode() {
        return strictMode;
    }

    public void setStrictMode(Boolean strictMode) {
        this.strictMode = strictMode;
    }

    public Boolean getRagEnabled() {
        return ragEnabled;
    }

    public void setRagEnabled(Boolean ragEnabled) {
        this.ragEnabled = ragEnabled;
    }

    public Boolean getCompactEnabled() {
        return compactEnabled;
    }

    public void setCompactEnabled(Boolean compactEnabled) {
        this.compactEnabled = compactEnabled;
    }

    public Boolean getShowThinking() {
        return showThinking;
    }

    public void setShowThinking(Boolean showThinking) {
        this.showThinking = showThinking;
    }

    public String getThinkingMode() {
        return thinkingMode;
    }

    public void setThinkingMode(String thinkingMode) {
        this.thinkingMode = thinkingMode;
    }

    public String getContextStrategy() {
        return contextStrategy;
    }

    public void setContextStrategy(String contextStrategy) {
        this.contextStrategy = contextStrategy;
    }

    public String getRuntimeConfigSnapshot() {
        return runtimeConfigSnapshot;
    }

    public void setRuntimeConfigSnapshot(String runtimeConfigSnapshot) {
        this.runtimeConfigSnapshot = runtimeConfigSnapshot;
    }

    public String getClientCapabilities() {
        return clientCapabilities;
    }

    public void setClientCapabilities(String clientCapabilities) {
        this.clientCapabilities = clientCapabilities;
    }

    public String getSessionSummary() {
        return sessionSummary;
    }

    public void setSessionSummary(String sessionSummary) {
        this.sessionSummary = sessionSummary;
    }

    public Integer getCurrentTurnNo() {
        return currentTurnNo;
    }

    public void setCurrentTurnNo(Integer currentTurnNo) {
        this.currentTurnNo = currentTurnNo;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
}
