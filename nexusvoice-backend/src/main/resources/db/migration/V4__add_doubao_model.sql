-- 添加豆包（Doubao）Seed 1.6 系列多模态深度思考模型
-- 包括 1.6 标准版和 1.6-flash 极速版
-- @author NexusVoice
-- @since 2025-10-22

-- 设置schema
SET search_path TO nexusvoice;

-- 1. 添加模型能力字段到 ai_models 表
ALTER TABLE ai_models ADD COLUMN IF NOT EXISTS capabilities TEXT[];
ALTER TABLE ai_models ADD COLUMN IF NOT EXISTS input_types TEXT[];
ALTER TABLE ai_models ADD COLUMN IF NOT EXISTS output_types TEXT[];

-- 2. 添加字段注释
COMMENT ON COLUMN ai_models.capabilities IS '模型支持的能力列表（数组）：ocr-OCR识别, vision-视觉理解, thinking-深度思考, tool_call-工具调用, multimodal-多模态';
COMMENT ON COLUMN ai_models.input_types IS '支持的输入类型（数组）：text-文本, image-图像, video-视频, audio-音频';
COMMENT ON COLUMN ai_models.output_types IS '支持的输出类型（数组）：text-文本, image-图像, audio-音频';

-- 3. 插入豆包 Seed 1.6 标准版模型配置
INSERT INTO ai_models (
    id, provider_code, model_code, model_type, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    capabilities, input_types, output_types,
    config_json, status, priority, created_at, updated_at, deleted
) VALUES (
    15, 
    'doubao', 
    'doubao-seed-1.6', 
    'chat',
    '豆包 Seed 1.6 多模态深度思考', 
    '字节跳动豆包多模态深度思考模型，支持图像理解、OCR、深度推理。256K上下文窗口，最大16K输出。适用于文档OCR、图像分析、复杂推理等场景。',
    'OpenAiChatModel', 
    'https://openai.qiniu.com/v1', 
    0.70, 
    8000, 
    90, 
    262144,
    0.004, 
    0.012,
    ARRAY['ocr', 'vision', 'thinking', 'tool_call', 'multimodal'],
    ARRAY['text', 'image'],
    ARRAY['text'],
    '{"thinkingModes": ["auto", "thinking", "non-thinking"], "maxOutputTokens": 16384, "backupBaseUrl": "https://api.qnaigc.com/v1"}',
    1, 
    100, 
    NOW(), 
    NOW(), 
    0
);

-- 4. 插入豆包 Seed 1.6-flash 极速版模型配置
INSERT INTO ai_models (
    id, provider_code, model_code, model_type, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    capabilities, input_types, output_types,
    config_json, status, priority, created_at, updated_at, deleted
) VALUES (
    16, 
    'doubao', 
    'doubao-seed-1.6-flash', 
    'chat',
    '豆包 Seed 1.6-flash 极速多模态', 
    '推理速度极致的多模态深度思考模型，TPOT低至10ms。支持文本和视觉理解（比肩友商pro系列），支持视频输入。256K上下文窗口，最大16K输出。价格分段计费，性价比极高。',
    'OpenAiChatModel', 
    'https://openai.qiniu.com/v1', 
    0.70, 
    8000, 
    60, 
    262144,
    0.0003, 
    0.003,
    ARRAY['ocr', 'vision', 'thinking', 'tool_call', 'multimodal'],
    ARRAY['text', 'image', 'video'],
    ARRAY['text'],
    '{"thinkingModes": ["auto", "thinking", "non-thinking"], "maxOutputTokens": 16384, "backupBaseUrl": "https://api.qnaigc.com/v1", "pricingTiers": [{"inputRange": "(0, 32K]", "inputPrice": 0.00015, "outputPrice": 0.0015}, {"inputRange": "(32K, 128K]", "inputPrice": 0.0003, "outputPrice": 0.003}, {"inputRange": "(128K, +∞]", "inputPrice": 0.0003, "outputPrice": 0.003}], "tpot": "10ms"}',
    1, 
    90, 
    NOW(), 
    NOW(), 
    0
);

-- 5. 插入豆包模型的API密钥配置示例（请替换为实际密钥）
-- 注意：两个模型可以共用同一个七牛云API密钥
INSERT INTO ai_api_keys (
    id, provider_code, model_code, api_key, base_url, 
    weight, status, priority, created_at, updated_at, deleted
) VALUES 
-- 1.6 标准版密钥
(
    1971856543655460801, 
    'doubao', 
    'doubao-seed-1.6', 
    '<请替换为实际的七牛云API密钥>', 
    'https://openai.qiniu.com/v1',
    1, 1, 100, NOW(), NOW(), 0
),
-- 1.6-flash 极速版密钥（可与标准版共用同一个密钥）
(
    1971856543655460802, 
    'doubao', 
    'doubao-seed-1.6-flash', 
    '<请替换为实际的七牛云API密钥>', 
    'https://openai.qiniu.com/v1',
    1, 1, 90, NOW(), NOW(), 0
);

-- 6. 添加使用说明注释
COMMENT ON COLUMN ai_api_keys.api_key IS 'API密钥（加密存储）。豆包模型使用七牛云API密钥，前往 https://portal.qiniu.com/ai-llm/api-key 获取';
