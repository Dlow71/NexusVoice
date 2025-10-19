-- 快速添加DeepSeek V3.1支持（可直接执行）
-- 请先将 'YOUR-API-KEY' 替换为您的实际七牛云API密钥

-- 1. 添加DeepSeek V3.1模型配置
INSERT INTO ai_models (
    id, provider_code, model_code, model_name, description, 
    model_class, default_base_url, default_temperature, 
    default_max_tokens, default_timeout_seconds, context_window, 
    input_token_price, output_token_price, status, priority
) VALUES (
    13, 'deepseek', 'deepseek-v3.1', 'DeepSeek V3.1', 
    'DeepSeek V3.1 - 支持显式推理(Think)、动态搜索(Search)、工具调用(Tool)，128K上下文窗口', 
    'OpenAiChatModel', 'https://api.deepseek.com/v1', 0.7, 
    8000, 120, 131072, 0.004, 0.012, 1, 90
) ON DUPLICATE KEY UPDATE 
    description = VALUES(description),
    default_max_tokens = VALUES(default_max_tokens),
    default_timeout_seconds = VALUES(default_timeout_seconds);

-- 2. 添加DeepSeek API密钥（七牛云代理）
INSERT INTO ai_api_keys (
    id, provider_code, model_code, api_key, base_url,
    status, weight, daily_limit, monthly_limit,
    total_used, daily_used, monthly_used,
    health_status, created_at, updated_at
) VALUES (
    103, 'deepseek', 'deepseek-v3.1', 
    'YOUR-API-KEY',  -- ⚠️ 请替换为您的七牛云API密钥
    'https://openai.qiniu.com/v1',
    1, 1, 10000, 300000,
    0, 0, 0, 1, NOW(), NOW()
) ON DUPLICATE KEY UPDATE 
    api_key = VALUES(api_key),
    base_url = VALUES(base_url),
    updated_at = NOW();

-- 3. 如果您已经配置了Grok的七牛云密钥，可以执行这个来复用同一个密钥：
-- UPDATE ai_api_keys a1
-- SET a1.api_key = (
--     SELECT a2.api_key 
--     FROM (SELECT api_key FROM ai_api_keys WHERE provider_code = 'grok' LIMIT 1) a2
-- )
-- WHERE a1.provider_code = 'deepseek';

-- 验证配置
SELECT 
    m.model_name,
    m.model_code,
    m.context_window,
    k.base_url,
    CASE 
        WHEN k.api_key IS NOT NULL AND k.api_key != '' 
        THEN '已配置' 
        ELSE '未配置' 
    END as api_key_status
FROM ai_models m
LEFT JOIN ai_api_keys k ON m.provider_code = k.provider_code 
    AND m.model_code = k.model_code
WHERE m.provider_code = 'deepseek';
