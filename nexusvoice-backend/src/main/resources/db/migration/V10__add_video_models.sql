-- V10: 添加视频生成模型配置（智谱CogVideoX-Flash）
-- Author: NexusVoice
-- Date: 2025-10-27
-- Description: 集成智谱AI视频生成模型到动态AI模型管理架构

-- ==================== 视频模型配置 ====================

-- 智谱AI CogVideoX-Flash（免费模型）
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
    input_token_price,
    output_token_price,
    status,
    config_json,
    created_at,
    updated_at
) VALUES (
    20, -- ID
    'zhipu', -- 厂商代码
    'cogvideox-flash', -- 模型代码（用于内部标识）
    'video', -- 模型类型
    'CogVideoX-Flash', -- 模型名称
    '智谱AI免费视频生成模型，支持文生视频、图生视频、首尾帧生成，5-10秒视频输出', -- 描述
    'https://open.bigmodel.cn/api', -- API基础URL
    NULL, -- temperature（视频生成不需要）
    NULL, -- max_tokens（视频生成不需要）
    300, -- 超时时间（秒），视频生成需要更长时间
    0.0, -- 输入token价格（免费）
    0.0, -- 输出token价格（免费）
    1, -- 状态：启用
    '{"model_code_for_api": "cogvideox-3", "default_quality": "speed", "default_fps": 30, "default_duration": 5, "supported_sizes": ["1280x720", "720x1280", "1024x1024", "1920x1080", "1080x1920", "2048x1080", "3840x2160"], "max_prompt_length": 1000, "supports_text_to_video": true, "supports_image_to_video": true, "supports_first_last_frame": true}', -- 配置JSON
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ==================== API密钥配置示例 ====================
-- 注意：实际使用时需要将下面的 'your_zhipu_api_key_here' 替换为真实的智谱AI API密钥

-- INSERT INTO ai_api_keys (
--     id,
--     provider_code,
--     model_code,
--     api_key,
--     base_url,
--     weight,
--     status,
--     daily_limit,
--     monthly_limit,
--     created_at,
--     updated_at
-- ) VALUES (
--     SNOWFLAKE_ID(), -- 使用雪花ID生成器
--     'zhipu',
--     'cogvideox-flash',
--     'your_zhipu_api_key_here', -- ⚠️ 需要替换为真实的智谱AI API密钥
--     'https://open.bigmodel.cn/api',
--     100, -- 权重
--     1, -- 状态：启用
--     NULL, -- 无日限额（免费版本）
--     NULL, -- 无月限额
--     CURRENT_TIMESTAMP,
--     CURRENT_TIMESTAMP
-- );
