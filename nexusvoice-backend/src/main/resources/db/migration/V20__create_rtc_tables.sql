-- ==================== WebRTC实时语音对话系统 - 数据库迁移脚本 ====================
--
-- 版本：V20
-- 说明：创建RTC会话、信令消息、性能指标表
-- 架构：WebRTC + Kurento + gRPC双向流
-- 数据库：PostgreSQL 14+
--
-- 创建时间：2025-11-01
-- ==================== ====================

-- ==================== 1. 创建rtc_sessions表（RTC会话表） ====================
CREATE TABLE IF NOT EXISTS rtc_sessions (
    -- 主键
    id BIGSERIAL PRIMARY KEY,
    
    -- 会话标识
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT 'WebRTC会话ID（UUID）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT COMMENT '角色ID（可选）',
    conversation_id BIGINT COMMENT '关联对话ID（可选）',
    
    -- 会话状态
    state VARCHAR(32) NOT NULL DEFAULT 'IDLE' COMMENT '会话状态（IDLE/CONNECTING/LISTENING/RECOGNIZING/THINKING/SPEAKING/INTERRUPTED/ERROR/TERMINATED）',
    
    -- 连接信息
    local_sdp TEXT COMMENT '本地SDP',
    remote_sdp TEXT COMMENT '远端SDP',
    ice_candidates JSONB COMMENT 'ICE候选者列表',
    selected_candidate_pair JSONB COMMENT '选中的候选者对',
    
    -- Kurento管道信息
    kms_pipeline_id VARCHAR(128) COMMENT 'Kurento Pipeline ID',
    kms_webrtc_endpoint_id VARCHAR(128) COMMENT 'Kurento WebRtcEndpoint ID',
    kms_rtp_endpoint_in_id VARCHAR(128) COMMENT 'Kurento RtpEndpoint(in) ID',
    kms_rtp_endpoint_out_id VARCHAR(128) COMMENT 'Kurento RtpEndpoint(out) ID',
    
    -- gRPC流标识
    grpc_asr_session_id VARCHAR(64) COMMENT 'gRPC ASR会话ID',
    grpc_tts_session_id VARCHAR(64) COMMENT 'gRPC TTS会话ID',
    
    -- 实时数据
    current_asr_text TEXT COMMENT '当前ASR识别文本',
    current_llm_response TEXT COMMENT '当前LLM响应',
    current_tts_segment_id INTEGER DEFAULT 0 COMMENT '当前TTS片段ID',
    
    -- 统计数据
    total_audio_duration_ms BIGINT DEFAULT 0 COMMENT '总音频时长（ms）',
    total_messages_count INTEGER DEFAULT 0 COMMENT '总消息数',
    interrupt_count INTEGER DEFAULT 0 COMMENT '打断次数',
    
    -- 错误信息
    last_error_code VARCHAR(64) COMMENT '最后错误码',
    last_error_message TEXT COMMENT '最后错误消息',
    
    -- 时间戳
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    started_at TIMESTAMP COMMENT '开始时间',
    ended_at TIMESTAMP COMMENT '结束时间',
    
    -- 逻辑删除
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否删除'
);

-- 创建索引
CREATE UNIQUE INDEX uk_rtc_session_id ON rtc_sessions(session_id) WHERE NOT deleted;
CREATE INDEX idx_rtc_user_id ON rtc_sessions(user_id) WHERE NOT deleted;
CREATE INDEX idx_rtc_user_state ON rtc_sessions(user_id, state) WHERE NOT deleted;
CREATE INDEX idx_rtc_state ON rtc_sessions(state) WHERE NOT deleted;
CREATE INDEX idx_rtc_created_at ON rtc_sessions(created_at DESC);
CREATE INDEX idx_rtc_conversation_id ON rtc_sessions(conversation_id) WHERE conversation_id IS NOT NULL AND NOT deleted;

-- 添加表注释
COMMENT ON TABLE rtc_sessions IS 'WebRTC实时语音对话会话表';

-- ==================== 2. 创建rtc_signal_messages表（信令消息表） ====================
CREATE TABLE IF NOT EXISTS rtc_signal_messages (
    -- 主键
    id BIGSERIAL PRIMARY KEY,
    
    -- 关联会话
    session_id VARCHAR(64) NOT NULL COMMENT 'WebRTC会话ID',
    
    -- 消息标识
    message_type VARCHAR(32) NOT NULL COMMENT '消息类型（OFFER/ANSWER/ICE_CANDIDATE/INTERRUPT/STATE_UPDATE/BACKPRESSURE等）',
    sequence BIGINT NOT NULL COMMENT '序列号',
    idempotency_key VARCHAR(64) COMMENT '幂等键',
    
    -- 消息内容
    payload JSONB NOT NULL COMMENT '消息内容（JSON）',
    
    -- 方向
    direction VARCHAR(16) NOT NULL COMMENT '方向（INBOUND/OUTBOUND）',
    
    -- 状态
    processed BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已处理',
    error_message TEXT COMMENT '处理错误消息',
    
    -- 时间戳
    timestamp BIGINT NOT NULL COMMENT '消息时间戳（ms）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- 创建索引
CREATE INDEX idx_signal_session_id ON rtc_signal_messages(session_id);
CREATE INDEX idx_signal_session_seq ON rtc_signal_messages(session_id, sequence DESC);
CREATE INDEX idx_signal_idempotency ON rtc_signal_messages(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_signal_created_at ON rtc_signal_messages(created_at DESC);

-- 添加表注释
COMMENT ON TABLE rtc_signal_messages IS 'WebRTC信令消息表（用于调试和幂等验证）';

-- ==================== 3. 创建rtc_session_metrics表（性能指标表） ====================
CREATE TABLE IF NOT EXISTS rtc_session_metrics (
    -- 主键
    id BIGSERIAL PRIMARY KEY,
    
    -- 关联会话
    session_id VARCHAR(64) NOT NULL COMMENT 'WebRTC会话ID',
    
    -- 延迟指标（ms）
    end_to_end_latency_ms INTEGER COMMENT '端到端延迟（用户开口→首帧播放）',
    asr_first_stable_ms INTEGER COMMENT 'ASR首稳定转写延迟',
    llm_first_token_ms INTEGER COMMENT 'LLM首token延迟',
    tts_first_byte_ms INTEGER COMMENT 'TTS首字节延迟',
    
    -- 打断指标（ms）
    interrupt_perceived_ms INTEGER COMMENT '感知中断延迟（前端ducking起效）',
    interrupt_source_ms INTEGER COMMENT '媒体源中断延迟（源停至缓冲耗尽）',
    
    -- 音频质量指标
    avg_bitrate BIGINT COMMENT '平均码率（bps）',
    packet_loss_rate DECIMAL(5, 2) COMMENT '丢包率（%）',
    avg_jitter_ms INTEGER COMMENT '平均抖动（ms）',
    avg_rtt_ms INTEGER COMMENT '平均RTT（ms）',
    fec_used_count INTEGER COMMENT 'FEC使用次数',
    silence_injected_frames INTEGER COMMENT '静音帧注入数',
    
    -- 编解码指标
    opus_encoder_queue_ms INTEGER COMMENT 'Opus编码器队列（ms）',
    resampler_queue_ms INTEGER COMMENT '重采样器队列（ms）',
    jitter_buffer_min_ms INTEGER COMMENT '抖动缓冲最小值（ms）',
    jitter_buffer_avg_ms INTEGER COMMENT '抖动缓冲平均值（ms）',
    jitter_buffer_max_ms INTEGER COMMENT '抖动缓冲最大值（ms）',
    
    -- 网络切换指标
    ice_restart_count INTEGER DEFAULT 0 COMMENT 'ICE重启次数',
    ice_restart_success_count INTEGER DEFAULT 0 COMMENT 'ICE重启成功次数',
    network_migration_count INTEGER DEFAULT 0 COMMENT '网络迁移次数',
    
    -- 时间戳
    measured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '测量时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- 创建索引
CREATE INDEX idx_metrics_session_id ON rtc_session_metrics(session_id);
CREATE INDEX idx_metrics_measured_at ON rtc_session_metrics(measured_at DESC);

-- 添加表注释
COMMENT ON TABLE rtc_session_metrics IS 'WebRTC会话性能指标表';

-- ==================== 4. 创建v_active_rtc_sessions视图（活跃会话视图） ====================
CREATE OR REPLACE VIEW v_active_rtc_sessions AS
SELECT 
    s.id,
    s.session_id,
    s.user_id,
    u.username,
    u.email,
    s.role_id,
    r.role_name,
    s.state,
    s.conversation_id,
    s.total_audio_duration_ms,
    s.total_messages_count,
    s.interrupt_count,
    s.created_at,
    s.started_at,
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - s.started_at)) * 1000 AS session_duration_ms,
    s.last_error_code,
    s.last_error_message
FROM rtc_sessions s
LEFT JOIN users u ON s.user_id = u.id
LEFT JOIN role_assistants r ON s.role_id = r.id
WHERE s.state NOT IN ('TERMINATED', 'ERROR')
  AND s.deleted = FALSE
ORDER BY s.created_at DESC;

-- 添加视图注释
COMMENT ON VIEW v_active_rtc_sessions IS 'WebRTC活跃会话视图（关联用户和角色信息）';

-- ==================== 5. 添加系统配置（RTC相关） ====================
INSERT INTO system_config (config_key, config_value, config_type, description, category, is_encrypted, created_at, updated_at)
VALUES
    -- 并发控制
    ('rtc.max.concurrent.sessions', '3', 'INTEGER', '每用户最大并发RTC会话数', 'RTC', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('rtc.session.timeout.minutes', '30', 'INTEGER', 'RTC会话超时（分钟）', 'RTC', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- 性能阈值
    ('rtc.slo.end_to_end_p95_ms', '600', 'INTEGER', '端到端延迟P95目标（ms）', 'RTC', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('rtc.slo.end_to_end_p99_ms', '800', 'INTEGER', '端到端延迟P99目标（ms）', 'RTC', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('rtc.slo.interrupt_perceived_p95_ms', '30', 'INTEGER', '感知中断P95目标（ms）', 'RTC', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('rtc.slo.interrupt_source_p95_ms', '150', 'INTEGER', '媒体源中断P95目标（ms）', 'RTC', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- 单flight保护
    ('rtc.single_flight.enabled', 'true', 'BOOLEAN', '启用单flight保护（防止并发请求冲突）', 'RTC', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- 背压控制
    ('rtc.backpressure.enabled', 'true', 'BOOLEAN', '启用背压控制', 'RTC', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('rtc.backpressure.asr_queue_threshold_ms', '100', 'INTEGER', 'ASR队列阈值（ms）', 'RTC', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('rtc.backpressure.tts_queue_threshold_ms', '200', 'INTEGER', 'TTS队列阈值（ms）', 'RTC', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- 调试配置
    ('rtc.debug.enabled', 'false', 'BOOLEAN', '启用RTC详细日志', 'RTC', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('rtc.debug.save_audio', 'false', 'BOOLEAN', '保存RTC音频文件（调试用）', 'RTC', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (config_key) DO NOTHING;

-- ==================== 6. 添加错误码（RTC相关） ====================
-- 注意：此处假设error_codes表存在，如不存在则需先创建
-- CREATE TABLE IF NOT EXISTS error_codes (...);

INSERT INTO error_codes (code, message, http_status, category, created_at)
VALUES
    -- RTC会话错误（5100-5199）
    (5100, 'RTC会话不存在', 404, 'RTC', CURRENT_TIMESTAMP),
    (5101, 'RTC会话已存在', 400, 'RTC', CURRENT_TIMESTAMP),
    (5102, 'RTC会话已终止', 400, 'RTC', CURRENT_TIMESTAMP),
    (5103, '超过最大并发会话数', 429, 'RTC', CURRENT_TIMESTAMP),
    (5104, 'RTC会话状态不允许此操作', 400, 'RTC', CURRENT_TIMESTAMP),
    (5105, 'RTC会话超时', 408, 'RTC', CURRENT_TIMESTAMP),
    
    -- SDP/ICE错误（5110-5119）
    (5110, 'SDP协商失败', 500, 'RTC', CURRENT_TIMESTAMP),
    (5111, 'ICE连接失败', 500, 'RTC', CURRENT_TIMESTAMP),
    (5112, 'ICE候选者收集失败', 500, 'RTC', CURRENT_TIMESTAMP),
    
    -- Kurento错误（5120-5129）
    (5120, 'Kurento连接失败', 500, 'RTC', CURRENT_TIMESTAMP),
    (5121, 'Kurento Pipeline创建失败', 500, 'RTC', CURRENT_TIMESTAMP),
    (5122, 'Kurento Endpoint创建失败', 500, 'RTC', CURRENT_TIMESTAMP),
    (5123, 'Kurento Endpoint释放失败', 500, 'RTC', CURRENT_TIMESTAMP),
    
    -- gRPC错误（5130-5139）
    (5130, 'ASR gRPC连接失败', 500, 'RTC', CURRENT_TIMESTAMP),
    (5131, 'TTS gRPC连接失败', 500, 'RTC', CURRENT_TIMESTAMP),
    (5132, 'ASR流式识别失败', 500, 'RTC', CURRENT_TIMESTAMP),
    (5133, 'TTS流式合成失败', 500, 'RTC', CURRENT_TIMESTAMP),
    
    -- 信令错误（5140-5149）
    (5140, '信令消息格式错误', 400, 'RTC', CURRENT_TIMESTAMP),
    (5141, '信令消息序列号错误', 400, 'RTC', CURRENT_TIMESTAMP),
    (5142, '信令消息幂等键重复', 400, 'RTC', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- ==================== 7. 创建清理历史会话的函数（可选） ====================
CREATE OR REPLACE FUNCTION cleanup_old_rtc_sessions(days_to_keep INTEGER DEFAULT 7)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    -- 软删除超过N天的已终止会话
    WITH deleted AS (
        UPDATE rtc_sessions
        SET deleted = TRUE, updated_at = CURRENT_TIMESTAMP
        WHERE state IN ('TERMINATED', 'ERROR')
          AND deleted = FALSE
          AND ended_at < CURRENT_TIMESTAMP - (days_to_keep || ' days')::INTERVAL
        RETURNING id
    )
    SELECT COUNT(*) INTO deleted_count FROM deleted;
    
    RETURN deleted_count;
END;
$$;

COMMENT ON FUNCTION cleanup_old_rtc_sessions IS '清理超过指定天数的已终止RTC会话（软删除）';

-- ==================== 迁移完成 ====================

