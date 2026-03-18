package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.voice.enums.VoiceResponseMode;
import com.nexusvoice.domain.voice.enums.VoiceSessionState;
import com.nexusvoice.domain.voice.enums.VoiceTransportMode;
import com.nexusvoice.domain.voice.model.VoiceSession;
import com.nexusvoice.infrastructure.persistence.po.VoiceSessionPO;
import org.springframework.stereotype.Component;

/**
 * 语音会话PO转换器。
 */
@Component
public class VoiceSessionPOConverter {

    public VoiceSessionPO toPO(VoiceSession domain) {
        if (domain == null) {
            return null;
        }

        VoiceSessionPO po = new VoiceSessionPO();
        po.setId(domain.getId());
        po.setVoiceSessionId(domain.getVoiceSessionId());
        po.setConversationId(domain.getConversationId());
        po.setUserId(domain.getUserId());
        po.setRoleId(domain.getRoleId());
        po.setState(domain.getState() != null ? domain.getState().name() : null);
        po.setTransportMode(domain.getTransportMode() != null ? domain.getTransportMode().name() : null);
        po.setResponseMode(domain.getResponseMode() != null ? domain.getResponseMode().name() : null);
        po.setSelectedModel(domain.getSelectedModel());
        po.setSelectedVoiceType(domain.getSelectedVoiceType());
        po.setSelectedAsrModel(domain.getSelectedAsrModel());
        po.setKnowledgeBaseIds(domain.getKnowledgeBaseIds());
        po.setStrictMode(domain.getStrictMode());
        po.setRagEnabled(domain.getRagEnabled());
        po.setCompactEnabled(domain.getCompactEnabled());
        po.setShowThinking(domain.getShowThinking());
        po.setThinkingMode(domain.getThinkingMode());
        po.setContextStrategy(domain.getContextStrategy());
        po.setRuntimeConfigSnapshot(domain.getRuntimeConfigSnapshot());
        po.setClientCapabilities(domain.getClientCapabilities());
        po.setSessionSummary(domain.getSessionSummary());
        po.setCurrentTurnNo(domain.getCurrentTurnNo());
        po.setLastErrorCode(domain.getLastErrorCode());
        po.setLastErrorMessage(domain.getLastErrorMessage());
        po.setStartedAt(domain.getStartedAt());
        po.setLastActiveAt(domain.getLastActiveAt());
        po.setEndedAt(domain.getEndedAt());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setDeleted(domain.getDeleted());
        return po;
    }

    public VoiceSession toDomain(VoiceSessionPO po) {
        if (po == null) {
            return null;
        }

        VoiceSession domain = new VoiceSession();
        domain.setId(po.getId());
        domain.setVoiceSessionId(po.getVoiceSessionId());
        domain.setConversationId(po.getConversationId());
        domain.setUserId(po.getUserId());
        domain.setRoleId(po.getRoleId());
        domain.setState(enumOrNull(po.getState(), VoiceSessionState.class));
        domain.setTransportMode(enumOrNull(po.getTransportMode(), VoiceTransportMode.class));
        domain.setResponseMode(enumOrNull(po.getResponseMode(), VoiceResponseMode.class));
        domain.setSelectedModel(po.getSelectedModel());
        domain.setSelectedVoiceType(po.getSelectedVoiceType());
        domain.setSelectedAsrModel(po.getSelectedAsrModel());
        domain.setKnowledgeBaseIds(po.getKnowledgeBaseIds());
        domain.setStrictMode(po.getStrictMode());
        domain.setRagEnabled(po.getRagEnabled());
        domain.setCompactEnabled(po.getCompactEnabled());
        domain.setShowThinking(po.getShowThinking());
        domain.setThinkingMode(po.getThinkingMode());
        domain.setContextStrategy(po.getContextStrategy());
        domain.setRuntimeConfigSnapshot(po.getRuntimeConfigSnapshot());
        domain.setClientCapabilities(po.getClientCapabilities());
        domain.setSessionSummary(po.getSessionSummary());
        domain.setCurrentTurnNo(po.getCurrentTurnNo());
        domain.setLastErrorCode(po.getLastErrorCode());
        domain.setLastErrorMessage(po.getLastErrorMessage());
        domain.setStartedAt(po.getStartedAt());
        domain.setLastActiveAt(po.getLastActiveAt());
        domain.setEndedAt(po.getEndedAt());
        domain.setCreatedAt(po.getCreatedAt());
        domain.setUpdatedAt(po.getUpdatedAt());
        domain.setDeleted(po.getDeleted());
        return domain;
    }

    private <T extends Enum<T>> T enumOrNull(String value, Class<T> type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
