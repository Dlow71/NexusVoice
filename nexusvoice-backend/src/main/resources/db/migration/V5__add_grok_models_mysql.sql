-- 添加Grok模型配置
-- 作者: NexusVoice
-- 日期: 2025-10-19
-- 说明: 添加xAI Grok模型支持

-- 插入Grok模型配置
INSERT INTO ai_models (
    id, 
    provider_code, 
    model_code, 
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
    status, 
    priority
) VALUES 
    -- Grok 4 Fast (用户指定的模型)
    (10, 'grok', 'grok-4-fast', 'Grok 4 Fast', 'xAI Grok 4快速版，具有强大的推理和对话能力，响应更快', 
     'OpenAiChatModel', 'https://api.x.ai/v1', 0.7, 4000, 
     90, 131072, 0.002, 0.01, 1, 100),
    
    -- Grok 2 Latest (备选模型)
    (11, 'grok', 'grok-2-latest', 'Grok 2', 'xAI Grok 2最新版本，平衡性能和速度', 
     'OpenAiChatModel', 'https://api.x.ai/v1', 0.7, 4000, 
     90, 131072, 0.002, 0.01, 1, 110);

-- 插入OpenAI GPT-OSS-20B模型配置
INSERT INTO ai_models (
    id, 
    provider_code, 
    model_code, 
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
    status, 
    priority
) VALUES 
    (12, 'openai', 'gpt-oss-20b', 'GPT OSS 20B', 'OpenAI兼容的开源模型，20B参数量', 
     'OpenAiChatModel', 'https://api.openai.com/v1', 0.7, 4000, 
     90, 128000, 0.001, 0.002, 1, 50);

-- 示例：添加API密钥（需要实际配置）
-- 请在实际部署时，替换下面的API密钥为真实的值
-- INSERT INTO ai_api_keys (id, provider_code, model_code, api_key, status, weight, created_at)
-- VALUES 
--     (100, 'grok', 'grok-4-fast', 'xai-xxx', 1, 1, NOW()),
--     (101, 'grok', 'grok-2-latest', 'xai-xxx', 1, 1, NOW()),
--     (102, 'openai', 'gpt-oss-20b', 'sk-xxx', 1, 1, NOW());

-- 提示：Grok API使用OpenAI兼容的接口格式
-- 基础URL: https://api.x.ai/v1
-- 认证方式: Bearer token (API Key)
