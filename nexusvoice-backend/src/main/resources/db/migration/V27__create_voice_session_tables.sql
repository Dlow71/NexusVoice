-- ==================== Voice Call基础表 ====================
--
-- 说明：
-- 为站内语音通话模式提供会话、轮次、播放片段与上下文快照基础表。
-- 当前阶段优先支撑产品级语音会话域，不替代旧rtc_sessions实验表。
--
-- 创建时间：2026-03-17
-- ==================== ====================

CREATE TABLE IF NOT EXISTS voice_sessions (
    id BIGSERIAL PRIMARY KEY,
    voice_session_id VARCHAR(64) NOT NULL,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NULL,

    state VARCHAR(32) NOT NULL,
    transport_mode VARCHAR(32) NOT NULL DEFAULT 'WEBSOCKET_STREAM',
    response_mode VARCHAR(32) NOT NULL DEFAULT 'VOICE_CALL',

    selected_model VARCHAR(128) NULL,
    selected_voice_type VARCHAR(128) NULL,
    knowledge_base_ids TEXT NULL,

    strict_mode BOOLEAN NOT NULL DEFAULT TRUE,
    rag_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    compact_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    show_thinking BOOLEAN NOT NULL DEFAULT FALSE,
    thinking_mode VARCHAR(32) NOT NULL DEFAULT 'disabled',
    context_strategy VARCHAR(32) NOT NULL DEFAULT 'COMPACT',

    runtime_config_snapshot TEXT NULL,
    client_capabilities TEXT NULL,
    session_summary TEXT NULL,

    current_turn_no INTEGER NOT NULL DEFAULT 0,
    last_error_code VARCHAR(64) NULL,
    last_error_message TEXT NULL,

    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_voice_sessions_sid
    ON voice_sessions(voice_session_id)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_voice_sessions_conversation
    ON voice_sessions(conversation_id)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_voice_sessions_user_state
    ON voice_sessions(user_id, state)
    WHERE deleted = 0;

COMMENT ON TABLE voice_sessions IS '站内语音通话会话表';

CREATE TABLE IF NOT EXISTS voice_turns (
    id BIGSERIAL PRIMARY KEY,
    voice_session_id VARCHAR(64) NOT NULL,
    conversation_id BIGINT NOT NULL,
    turn_no INTEGER NOT NULL,

    user_transcript_partial TEXT NULL,
    user_transcript_final TEXT NULL,

    assistant_text_display TEXT NULL,
    assistant_text_spoken TEXT NULL,
    reasoning_content TEXT NULL,
    rag_citations TEXT NULL,

    retrieval_query TEXT NULL,
    retrieval_summary TEXT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'processing',
    interrupted BOOLEAN NOT NULL DEFAULT FALSE,
    interrupt_reason VARCHAR(64) NULL,

    user_speech_started_at TIMESTAMP NULL,
    user_finalized_at TIMESTAMP NULL,
    retrieval_started_at TIMESTAMP NULL,
    retrieval_completed_at TIMESTAMP NULL,
    assistant_first_token_at TIMESTAMP NULL,
    assistant_first_audio_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_voice_turns_session_turn
    ON voice_turns(voice_session_id, turn_no);

CREATE INDEX IF NOT EXISTS idx_voice_turns_session
    ON voice_turns(voice_session_id);

COMMENT ON TABLE voice_turns IS '语音通话轮次表';

CREATE TABLE IF NOT EXISTS voice_playback_segments (
    id BIGSERIAL PRIMARY KEY,
    voice_turn_id BIGINT NOT NULL,
    segment_index INTEGER NOT NULL,

    display_text TEXT NOT NULL,
    spoken_text TEXT NOT NULL,
    audio_url TEXT NULL,

    audio_duration_ms INTEGER NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    is_late_update BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_voice_segments_turn_index
    ON voice_playback_segments(voice_turn_id, segment_index);

COMMENT ON TABLE voice_playback_segments IS '语音播放分段表';

CREATE TABLE IF NOT EXISTS voice_realtime_events (
    id BIGSERIAL PRIMARY KEY,
    voice_session_id VARCHAR(64) NOT NULL,
    turn_no INTEGER NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_voice_events_session_time
    ON voice_realtime_events(voice_session_id, created_at DESC);

COMMENT ON TABLE voice_realtime_events IS '语音实时事件调试表';

CREATE TABLE IF NOT EXISTS voice_context_snapshots (
    id BIGSERIAL PRIMARY KEY,
    voice_session_id VARCHAR(64) NOT NULL,
    snapshot_type VARCHAR(32) NOT NULL,
    raw_message_count INTEGER NOT NULL DEFAULT 0,
    estimated_tokens INTEGER NOT NULL DEFAULT 0,
    summary_text TEXT NOT NULL,
    metadata TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_voice_context_snapshots_session
    ON voice_context_snapshots(voice_session_id, created_at DESC);

COMMENT ON TABLE voice_context_snapshots IS '语音上下文摘要快照表';
