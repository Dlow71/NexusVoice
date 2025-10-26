-- V9: 添加TTS语音合成模型配置
-- Author: NexusVoice
-- Date: 2025-10-27
-- Description: 将TTS服务集成到动态AI模型管理架构
-- 包含七牛云TTS模型配置（中文女声、中文男声、英文女声）

-- ==================== TTS模型配置 ====================

-- 七牛云TTS模型（中文女声 - 默认）
INSERT INTO ai_models (
    id,
    provider_code,
    model_code,
    model_type,
    model_name,
    description,
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
    17,
    'qiniu',
    'zh_female_wwxkjx',
    'tts',
    '七牛云中文女声',
    '七牛云TTS中文女声合成，标准音色',
    'wss://openai.qiniu.com/v1/voice/tts',
    NULL,
    NULL,
    60,
    NULL,
    0.002,  -- 按千字符计费：0.002元/千字
    0,
    '{"voiceType":"qiniu_zh_female_wwxkjx","encoding":"mp3","speedRatio":1.0}',
    1,
    100,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

-- 七牛云TTS模型（中文男声）
INSERT INTO ai_models (
    id,
    provider_code,
    model_code,
    model_type,
    model_name,
    description,
    default_base_url,
    default_timeout_seconds,
    input_token_price,
    output_token_price,
    config_json,
    status,
    priority,
    created_at,
    updated_at,
    deleted
) VALUES (
    18,
    'qiniu',
    'zh_male_wwxkjx',
    'tts',
    '七牛云中文男声',
    '七牛云TTS中文男声合成，标准音色',
    'wss://openai.qiniu.com/v1/voice/tts',
    60,
    0.002,
    0,
    '{"voiceType":"qiniu_zh_male_wwxkjx","encoding":"mp3","speedRatio":1.0}',
    1,
    101,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

-- 七牛云TTS模型（英文女声）
INSERT INTO ai_models (
    id,
    provider_code,
    model_code,
    model_type,
    model_name,
    description,
    default_base_url,
    default_timeout_seconds,
    input_token_price,
    output_token_price,
    config_json,
    status,
    priority,
    created_at,
    updated_at,
    deleted
) VALUES (
    19,
    'qiniu',
    'en_female_wwxkjx',
    'tts',
    '七牛云英文女声',
    '七牛云TTS英文女声合成，标准音色',
    'wss://openai.qiniu.com/v1/voice/tts',
    60,
    0.002,
    0,
    '{"voiceType":"qiniu_en_female_wwxkjx","encoding":"mp3","speedRatio":1.0}',
    1,
    102,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

-- ==================== TTS API密钥配置 ====================
-- 注意：这里使用示例密钥，实际部署时需要替换为真实的七牛云TTS Token

-- 七牛云TTS密钥（共用同一个密钥，支持所有语音类型）
INSERT INTO ai_api_keys (
    id,
    provider_code,
    model_code,
    api_key,
    weight,
    rate_limit,
    concurrent_limit,
    status,
    fail_count,
    total_requests,
    total_tokens_used,
    total_cost,
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
    -- 中文女声密钥
    1971856543655460803,
    'qiniu',
    'zh_female_wwxkjx',
    'your_qiniu_tts_token_replace_me',  -- 需要替换为真实Token
    1,
    100,
    10,
    1,
    0,
    0,
    0,
    0.00,
    0,
    0,
    0.00,
    CURRENT_DATE,
    NULL,
    NULL,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    -- 中文男声密钥
    1971856543655460804,
    'qiniu',
    'zh_male_wwxkjx',
    'your_qiniu_tts_token_replace_me',  -- 需要替换为真实Token
    1,
    100,
    10,
    1,
    0,
    0,
    0,
    0.00,
    0,
    0,
    0.00,
    CURRENT_DATE,
    NULL,
    NULL,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    -- 英文女声密钥
    1971856543655460805,
    'qiniu',
    'en_female_wwxkjx',
    'your_qiniu_tts_token_replace_me',  -- 需要替换为真实Token
    1,
    100,
    10,
    1,
    0,
    0,
    0,
    0.00,
    0,
    0,
    0.00,
    CURRENT_DATE,
    NULL,
    NULL,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

-- ==================== 系统配置更新 ====================
-- 添加默认TTS模型配置

INSERT INTO system_config (
    id,
    config_key,
    config_value,
    config_name,
    category,
    is_modifiable,
    is_public,
    sort_order,
    description,
    deleted,
    created_at,
    updated_at
) VALUES (
    201,
    'tts.default.model',
    'qiniu:zh_female_wwxkjx',
    '默认TTS模型',
    'tts',
    1,
    0,
    200,
    '系统默认使用的TTS语音合成模型（格式：provider:model）',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (config_key) DO UPDATE 
SET config_value = EXCLUDED.config_value,
    updated_at = CURRENT_TIMESTAMP;

-- ==================== 注释说明 ====================

-- TTS模型说明：
-- 1. provider_code: 'qiniu' - 七牛云TTS服务
-- 2. model_type: 'tts' - 语音合成模型类型
-- 3. input_token_price: 按千字符计费，0.002元/千字
-- 4. output_token_price: TTS模型无输出token费用，设置为0
-- 5. config_json: 包含voiceType、encoding、speedRatio等TTS特定参数
-- 6. default_timeout_seconds: 60秒，TTS合成通常较快

-- API密钥说明：
-- 1. 三个语音类型可以共用同一个七牛云TTS Token
-- 2. 实际部署时需要将 'your_qiniu_tts_token_replace_me' 替换为真实Token
-- 3. Token获取方式：登录七牛云控制台 → TTS服务 → 获取Token
-- 4. rate_limit: 每分钟100次请求限制
-- 5. concurrent_limit: 同时10个并发请求

-- 系统配置说明：
-- 1. tts.default.model: 指定系统默认使用的TTS模型
-- 2. 格式必须为 'provider:model'，如 'qiniu:zh_female_wwxkjx'
-- 3. TTSService会读取此配置来选择默认模型
-- 4. 用户可以在调用时指定其他模型覆盖默认配置

-- 迁移说明：
-- 1. 原有system_config中的tts.voice.default等配置保留，用于向后兼容
-- 2. 新架构优先使用tts.default.model来选择模型
-- 3. 模型配置在ai_models表中统一管理，支持热更新
-- 4. API密钥在ai_api_keys表中管理，支持密钥池轮询
