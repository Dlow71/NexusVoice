-- =============================================
-- ASR语音识别模型配置迁移脚本
-- 作者: NexusVoice
-- 日期: 2025-10-26
-- 说明: 添加硅基流动ASR语音识别模型配置和API密钥
-- =============================================

-- =============================================
-- 1. 插入ASR模型配置
-- =============================================

-- TeleAI/TeleSpeechASR - 星辰超多方言语音识别大模型
INSERT INTO ai_models (
    provider_code,
    model_code,
    model_type,
    model_name,
    description,
    model_class,
    default_base_url,
    default_temperature,
    default_max_tokens,
    default_timeout_seconds,
    context_window,
    input_token_price,
    output_token_price,
    config_json,
    status,
    priority,
    created_at,
    updated_at,
    deleted
) VALUES (
    'siliconflow',
    'telespeech-asr',
    'asr',
    'TeleAI/TeleSpeechASR',
    '星辰超多方言语音识别大模型，支持普通话+英文+50种方言混说，业内首个同时支持普通话+英文+50种方言自由混说的语音识别大模型',
    'SiliconFlowAsrAdapter',
    'https://api.siliconflow.cn/v1',
    NULL,
    NULL,
    60,
    NULL,
    0.008,  -- 按秒计费：0.008元/秒
    0.000,  -- ASR模型无输出token费用
    '{"supported_formats": ["mp3", "wav", "m4a", "flac", "opus"], "max_duration_seconds": 3600, "max_file_size_mb": 100, "supports_multi_dialect": true, "supports_punctuation": true, "supports_itn": true, "languages": ["zh-CN", "en-US", "yue", "wuu", "hsn"], "description": "支持粤语、上海话、四川话等主要方言，业内领先的多方言识别准确率"}',
    1,  -- 启用
    10, -- 优先级
    NOW(),
    NOW(),
    0
);

-- FunAudioLLM/SenseVoiceSmall - 高效多语言语音识别模型
INSERT INTO ai_models (
    provider_code,
    model_code,
    model_type,
    model_name,
    description,
    model_class,
    default_base_url,
    default_temperature,
    default_max_tokens,
    default_timeout_seconds,
    context_window,
    input_token_price,
    output_token_price,
    config_json,
    status,
    priority,
    created_at,
    updated_at,
    deleted
) VALUES (
    'siliconflow',
    'sensevoice-small',
    'asr',
    'FunAudioLLM/SenseVoiceSmall',
    '高效多语言语音识别模型，支持中文、英文、日语、韩语等多种语言识别',
    'SiliconFlowAsrAdapter',
    'https://api.siliconflow.cn/v1',
    NULL,
    NULL,
    60,
    NULL,
    0.005,  -- 按秒计费：0.005元/秒
    0.000,  -- ASR模型无输出token费用
    '{"supported_formats": ["mp3", "wav", "m4a", "flac"], "max_duration_seconds": 1800, "max_file_size_mb": 50, "supports_emotion_recognition": true, "languages": ["zh-CN", "en-US", "ja-JP", "ko-KR"], "description": "高效的多语言语音识别，支持情感识别"}',
    1,  -- 启用
    20, -- 优先级
    NOW(),
    NOW(),
    0
);

-- =============================================
-- 2. 插入API密钥配置（需要替换为真实密钥）
-- =============================================

-- 硅基流动ASR主密钥（TeleAI/TeleSpeechASR）
INSERT INTO ai_api_keys (
    provider_code,
    model_code,
    api_key,
    api_secret,
    base_url,
    proxy_url,
    weight,
    rate_limit,
    concurrent_limit,
    status,
    fail_count,
    last_fail_time,
    last_success_time,
    health_check_time,
    total_requests,
    total_tokens_used,
    total_cost,
    last_used_at,
    monthly_requests,
    monthly_tokens_used,
    monthly_cost,
    monthly_reset_date,
    daily_quota_limit,
    monthly_quota_limit,
    daily_tokens_used,
    created_at,
    updated_at,
    deleted
) VALUES (
    'siliconflow',
    'telespeech-asr',
    'sk-your-siliconflow-api-key-here',  -- ⚠️ 请替换为真实的硅基流动API密钥
    NULL,
    'https://api.siliconflow.cn/v1',
    NULL,
    10,   -- 权重
    100,  -- 速率限制（请求/分钟）
    5,    -- 并发限制
    1,    -- 启用状态
    0,    -- 失败次数
    NULL,
    NULL,
    NULL,
    0,    -- 总请求数
    0,    -- 总token使用量
    0.00, -- 总费用
    NULL,
    0,    -- 月请求数
    0,    -- 月token使用量
    0.00, -- 月费用
    DATE_FORMAT(NOW(), '%Y-%m-01'), -- 月度重置日期
    10000,   -- 日限额（tokens）
    300000,  -- 月限额（tokens）
    0,       -- 日已使用tokens
    NOW(),
    NOW(),
    0
);

-- 硅基流动ASR备用密钥（FunAudioLLM/SenseVoiceSmall）
INSERT INTO ai_api_keys (
    provider_code,
    model_code,
    api_key,
    api_secret,
    base_url,
    proxy_url,
    weight,
    rate_limit,
    concurrent_limit,
    status,
    fail_count,
    last_fail_time,
    last_success_time,
    health_check_time,
    total_requests,
    total_tokens_used,
    total_cost,
    last_used_at,
    monthly_requests,
    monthly_tokens_used,
    monthly_cost,
    monthly_reset_date,
    daily_quota_limit,
    monthly_quota_limit,
    daily_tokens_used,
    created_at,
    updated_at,
    deleted
) VALUES (
    'siliconflow',
    'sensevoice-small',
    'sk-your-siliconflow-api-key-here',  -- ⚠️ 请替换为真实的硅基流动API密钥
    NULL,
    'https://api.siliconflow.cn/v1',
    NULL,
    10,   -- 权重
    100,  -- 速率限制（请求/分钟）
    5,    -- 并发限制
    1,    -- 启用状态
    0,    -- 失败次数
    NULL,
    NULL,
    NULL,
    0,    -- 总请求数
    0,    -- 总token使用量
    0.00, -- 总费用
    NULL,
    0,    -- 月请求数
    0,    -- 月token使用量
    0.00, -- 月费用
    DATE_FORMAT(NOW(), '%Y-%m-01'), -- 月度重置日期
    10000,   -- 日限额（tokens）
    300000,  -- 月限额（tokens）
    0,       -- 日已使用tokens
    NOW(),
    NOW(),
    0
);

-- =============================================
-- 3. 验证插入结果
-- =============================================

-- 查看已插入的ASR模型
SELECT 
    id,
    provider_code,
    model_code,
    model_type,
    model_name,
    status,
    priority
FROM ai_models 
WHERE model_type = 'asr'
ORDER BY priority;

-- 查看已插入的ASR密钥
SELECT 
    id,
    provider_code,
    model_code,
    CONCAT(LEFT(api_key, 10), '...') as api_key_masked,
    status,
    weight
FROM ai_api_keys 
WHERE model_code IN ('telespeech-asr', 'sensevoice-small')
ORDER BY provider_code, model_code;
