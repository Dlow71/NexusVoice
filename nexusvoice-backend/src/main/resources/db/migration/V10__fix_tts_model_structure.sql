-- V10: 修复TTS模型结构 - 将音色从模型标识改为请求参数
-- Author: NexusVoice
-- Date: 2025-11-11
-- Description: 
--   1. 删除原有的3个音色模型（zh_female_wwxkjx、zh_male_wwxkjx、en_female_wwxkjx）
--   2. 创建统一的七牛云TTS模型（qiniu-tts）
--   3. 音色配置存储在config_json中，作为默认值
--   4. 所有API密钥共用同一个model_code（qiniu-tts）
--   5. 用户可以通过请求参数覆盖默认音色

-- ==================== 1. 删除旧的TTS模型和密钥 ====================

-- 删除旧的API密钥（软删除）
UPDATE ai_api_keys 
SET deleted = 1, updated_at = CURRENT_TIMESTAMP
WHERE provider_code = 'qiniu' 
  AND model_code IN ('zh_female_wwxkjx', 'zh_male_wwxkjx', 'en_female_wwxkjx')
  AND deleted = 0;

-- 删除旧的模型（软删除）
UPDATE ai_models 
SET deleted = 1, updated_at = CURRENT_TIMESTAMP
WHERE provider_code = 'qiniu' 
  AND model_code IN ('zh_female_wwxkjx', 'zh_male_wwxkjx', 'en_female_wwxkjx')
  AND deleted = 0;

-- ==================== 2. 创建统一的七牛云TTS模型 ====================

-- 先删除可能存在的ID=20的记录（硬删除，确保干净）
DELETE FROM ai_models WHERE id = 20;

-- 插入七牛云TTS统一模型
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
    20,
    'qiniu',
    'qiniu-tts',
    'tts',
    '七牛云语音合成',
    '七牛云TTS语音合成服务，支持多种中英文音色，通过请求参数指定voice_type',
    'https://api.qnaigc.com/v1/voice/tts',
    NULL,
    NULL,
    60,
    NULL,
    0.002,  -- 按千字符计费：0.002元/千字
    0,      -- TTS无输出token费用
    '{
        "voiceType": "zh_female_wwxkjx",
        "encoding": "mp3",
        "speedRatio": 1.0,
        "supportedVoices": [
            "zh_female_wwxkjx",
            "zh_male_wwxkjx",
            "en_female_wwxkjx",
            "zh_male_M392_conversation_wvae_bigtts",
            "zh_female_F107_conversation_wvae_bigtts"
        ],
        "voiceDescriptions": {
            "zh_female_wwxkjx": "中文女声（标准）",
            "zh_male_wwxkjx": "中文男声（标准）",
            "en_female_wwxkjx": "英文女声（标准）",
            "zh_male_M392_conversation_wvae_bigtts": "中文男声（对话风格）",
            "zh_female_F107_conversation_wvae_bigtts": "中文女声（对话风格）"
        }
    }',
    1,
    100,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

-- ==================== 3. 创建统一的TTS API密钥 ====================

-- 先删除可能存在的ID=1971856543655460810的记录（硬删除）
DELETE FROM ai_api_keys WHERE id = 1971856543655460810;

-- 插入七牛云TTS API密钥（所有音色共用）
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
    1971856543655460810,
    'qiniu',
    'qiniu-tts',
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

-- ==================== 4. 更新系统配置 ====================

-- 更新默认TTS模型配置
UPDATE system_config 
SET config_value = 'qiniu:qiniu-tts',
    updated_at = CURRENT_TIMESTAMP
WHERE config_key = 'tts.default.model';

-- 添加默认音色配置（如果不存在）
-- 先删除可能存在的配置
DELETE FROM system_config WHERE config_key = 'tts.default.voice';

-- 插入新配置
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
    202,
    'tts.default.voice',
    'zh_female_wwxkjx',
    '默认TTS音色',
    'tts',
    1,
    0,
    201,
    '系统默认使用的TTS音色类型（可选值见ai_models表config_json中的supportedVoices）',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ==================== 注释说明 ====================

-- 架构改进说明：
-- 
-- 【原有问题】
-- 1. 将每个音色作为独立的model_code（如zh_female_wwxkjx）
-- 2. 导致ApiKeyPoolManager无法找到对应密钥（查找qiniu:zh_female_wwxkjx失败）
-- 3. 每个音色需要单独配置API密钥，管理复杂
-- 
-- 【新架构设计】
-- 1. model_code统一为'qiniu-tts'，代表七牛云TTS服务
-- 2. voice_type作为请求参数传递，不是模型标识的一部分
-- 3. 所有音色共用同一个密钥池，简化管理
-- 4. 默认音色在config_json中配置，用户可通过请求参数覆盖
-- 
-- 【API调用示例】
-- curl --location 'https://api.qnaigc.com/v1/voice/tts' \
--   --header 'Content-Type: application/json' \
--   --header 'Authorization: Bearer <API_KEY>' \
--   --data '{
--     "audio": {
--       "voice_type": "zh_male_M392_conversation_wvae_bigtts",  # 音色在这里指定
--       "encoding": "mp3",
--       "speed_ratio": 1.0
--     },
--     "request": {
--       "text": "你好，世界！"
--     }
--   }'
-- 
-- 【使用方式】
-- 1. 默认使用：不传voiceType，使用config_json中的默认音色
-- 2. 指定音色：在TtsRequest中传入voiceType参数覆盖默认值
-- 3. 音色列表：从config_json的supportedVoices中查看支持的所有音色
-- 
-- 【配置密钥】
-- 需要将 'your_qiniu_tts_token_replace_me' 替换为真实的七牛云TTS Token
-- 获取方式：登录七牛云控制台 → AI能力 → 语音合成 → 获取Token
