package com.nexusvoice.domain.voice.repository;

import com.nexusvoice.domain.voice.model.VoiceSession;

import java.util.Optional;

/**
 * 语音会话仓储接口。
 */
public interface VoiceSessionRepository {

    VoiceSession save(VoiceSession voiceSession);

    Optional<VoiceSession> findByVoiceSessionId(String voiceSessionId);
}
