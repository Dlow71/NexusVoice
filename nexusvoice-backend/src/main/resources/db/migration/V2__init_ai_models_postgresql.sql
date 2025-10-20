-- =====================================================
-- 初始化AI模型配置数据
-- PostgreSQL版本
-- =====================================================

-- 插入预置AI模型配置
-- 1. OpenAI GPT-4o-mini
INSERT INTO ai_models (id, provider_code, model_code, model_name, description, 
    model_class, default_base_url, default_temperature, default_max_tokens, 
    default_timeout_seconds, context_window, input_token_price, output_token_price, 
    status, priority, created_at, updated_at, deleted)
VALUES (
    1, 'openai', 'gpt-4o-mini', 'GPT-4o Mini', 
    'OpenAI最新的高性价比模型，适合日常对话',
    'dev.langchain4j.model.openai.OpenAiChatModel',
    'https://api.openai.com/v1',
    0.7, 2000, 60, 128000,
    0.00015, 0.0006,  -- 输入$0.15/1M tokens, 输出$0.60/1M tokens
    1, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (id) DO NOTHING;

-- 2. OpenAI GPT-4o
INSERT INTO ai_models (id, provider_code, model_code, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    status, priority, created_at, updated_at, deleted)
VALUES (
    2, 'openai', 'gpt-4o', 'GPT-4o',
    'OpenAI最强多模态模型，支持视觉理解',
    'dev.langchain4j.model.openai.OpenAiChatModel',
    'https://api.openai.com/v1',
    0.7, 4000, 90, 128000,
    0.0025, 0.01,  -- 输入$2.50/1M tokens, 输出$10.00/1M tokens
    1, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (id) DO NOTHING;

-- 3. OpenAI GPT-4 Turbo
INSERT INTO ai_models (id, provider_code, model_code, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    status, priority, created_at, updated_at, deleted)
VALUES (
    3, 'openai', 'gpt-4-turbo', 'GPT-4 Turbo',
    'GPT-4的优化版本，更快更便宜',
    'dev.langchain4j.model.openai.OpenAiChatModel',
    'https://api.openai.com/v1',
    0.7, 4000, 90, 128000,
    0.01, 0.03,  -- 输入$10/1M tokens, 输出$30/1M tokens
    1, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (id) DO NOTHING;

-- 4. Claude 3.5 Sonnet
INSERT INTO ai_models (id, provider_code, model_code, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    status, priority, created_at, updated_at, deleted)
VALUES (
    4, 'anthropic', 'claude-3-5-sonnet-20241022', 'Claude 3.5 Sonnet',
    'Anthropic最新模型，卓越的推理能力',
    'dev.langchain4j.model.anthropic.AnthropicChatModel',
    'https://api.anthropic.com',
    0.7, 4000, 90, 200000,
    0.003, 0.015,  -- 输入$3/1M tokens, 输出$15/1M tokens
    1, 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (id) DO NOTHING;

-- 5. Grok Beta (通过七牛云代理)
INSERT INTO ai_models (id, provider_code, model_code, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    status, priority, created_at, updated_at, deleted)
VALUES (
    5, 'grok', 'grok-beta', 'Grok Beta',
    'xAI的Grok模型，幽默风趣的AI助手',
    'OpenAiCompatible',
    'https://api.x.ai/v1',
    0.7, 2000, 60, 131072,
    0.005, 0.015,  -- 输入$5/1M tokens, 输出$15/1M tokens
    1, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (id) DO NOTHING;

-- 6. DeepSeek V3 (通过七牛云代理)
INSERT INTO ai_models (id, provider_code, model_code, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    status, priority, created_at, updated_at, deleted)
VALUES (
    6, 'deepseek', 'deepseek-chat', 'DeepSeek Chat',
    'DeepSeek高性价比对话模型',
    'OpenAiCompatible',
    'https://api.deepseek.com',
    0.7, 2000, 60, 64000,
    0.00014, 0.00028,  -- 输入$0.14/1M tokens, 输出$0.28/1M tokens
    1, 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (id) DO NOTHING;

-- 7. DeepSeek V3.1 (通过七牛云代理，支持深度思考)
INSERT INTO ai_models (id, provider_code, model_code, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    config_json, status, priority, created_at, updated_at, deleted)
VALUES (
    7, 'deepseek', 'deepseek-v3.1', 'DeepSeek V3.1',
    'DeepSeek最新模型，支持显式推理、动态搜索、工具调用',
    'OpenAiCompatible',
    'https://openai.qiniu.com/v1',
    0.7, 8000, 120, 128000,
    0.004, 0.012,  -- 输入0.004元/1K tokens, 输出0.012元/1K tokens (七牛云代理价格)
    '{"supportsThinking":true,"supportsSearch":true,"supportsToolCall":true}',
    1, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (id) DO NOTHING;

-- 8. 通义千问 Qwen-Max
INSERT INTO ai_models (id, provider_code, model_code, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    status, priority, created_at, updated_at, deleted)
VALUES (
    8, 'qwen', 'qwen-max', '通义千问 Max',
    '阿里云通义千问旗舰模型',
    'OpenAiCompatible',
    'https://dashscope.aliyuncs.com/compatible-mode/v1',
    0.7, 2000, 60, 8000,
    0.02, 0.06,  -- 输入0.02元/1K tokens, 输出0.06元/1K tokens
    1, 70, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (id) DO NOTHING;

-- 9. GPT OSS 20B (开源模型，通过七牛云代理)
INSERT INTO ai_models (id, provider_code, model_code, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    status, priority, created_at, updated_at, deleted)
VALUES (
    9, 'openai', 'gpt-oss-20b', 'GPT OSS 20B',
    '开源GPT模型，性价比极高',
    'dev.langchain4j.model.openai.OpenAiChatModel',
    'https://openai.qiniu.com/v1',
    0.7, 2000, 60, 32768,
    0.0002, 0.0002,  -- 极低价格
    1, 80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (id) DO NOTHING;

-- 10. Grok 4 Fast (通过七牛云代理)
INSERT INTO ai_models (id, provider_code, model_code, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    status, priority, created_at, updated_at, deleted)
VALUES (
    10, 'grok', 'grok-4-fast', 'Grok 4 Fast',
    'xAI最快的Grok模型版本',
    'OpenAiCompatible',
    'https://openai.qiniu.com/v1',
    0.7, 2000, 60, 131072,
    0.005, 0.015,
    1, 55, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
) ON CONFLICT (id) DO NOTHING;

-- 创建序列（如果需要）
-- PostgreSQL会自动为BIGINT主键创建序列，这里仅作为备用
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = 'public' AND sequencename = 'ai_models_id_seq') THEN
        CREATE SEQUENCE ai_models_id_seq START WITH 100;
    END IF;
END $$;
