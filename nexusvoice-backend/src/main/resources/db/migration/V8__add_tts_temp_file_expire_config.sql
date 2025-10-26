-- V8: 添加TTS临时文件过期时间配置
-- Author: NexusVoice
-- Date: 2025-10-26
-- Description: 添加流式聊天语音回复的临时文件过期时间配置项（单位：分钟）
-- 注意：仅用于ChatStreamHandler中的语音回复，开场白等使用持久文件

-- 插入TTS临时文件过期时间配置
INSERT INTO system_config (
    id,
    config_key,
    config_value,
    description,
    config_group,
    enabled,
    readonly,
    sort_order,
    remark,
    version,
    created_at,
    updated_at,
    deleted
)
VALUES 
(
    200,
    'tts.stream.response.expire.minutes',
    '1440',
    '流式聊天语音回复过期时间（分钟）',
    'tts',
    1,
    0,
    200,
    '流式聊天中AI语音回复的临时文件过期时间。超过此时间后文件将被自动清理。默认1440分钟（24小时）。推荐范围：720分钟（12小时快速清理）、1440分钟（24小时平衡）、2880分钟（48小时保留更久）',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
)
ON CONFLICT (id) DO UPDATE SET
    config_value = EXCLUDED.config_value,
    description = EXCLUDED.description,
    remark = EXCLUDED.remark,
    updated_at = CURRENT_TIMESTAMP;

-- 添加注释
-- 使用场景说明：
-- 1. ChatStreamHandler - 流式聊天的语音回复（临时文件）✅
-- 2. TTSService开场白 - 角色开场白（持久文件）❌ 不使用此配置
-- 3. TTSService普通TTS - 用户主动请求的TTS（持久文件）❌ 不使用此配置
-- 
-- 配置建议：
-- - 720分钟（12小时）：高频使用，快速清理，节省存储
-- - 1440分钟（24小时）：默认值，平衡用户体验和存储成本
-- - 2880分钟（48小时）：低频使用，保留更久
-- - 4320分钟（72小时）：长期保留，适合重要对话
