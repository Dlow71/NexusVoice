-- 插入AI模型API密钥
-- 作者: NexusVoice
-- 日期: 2025-10-19
-- 说明: 为Grok和GPT OSS模型配置API密钥
-- 重要：请将下面的API密钥替换为您的实际密钥！

-- 插入Grok 4 Fast的API密钥
INSERT INTO ai_api_keys (
    id,
    provider_code,
    model_code,
    api_key,
    base_url,
    proxy_url,
    status,
    weight,
    daily_limit,
    monthly_limit,
    total_used,
    daily_used,
    monthly_used,
    last_used_time,
    health_check_time,
    health_status,
    created_at,
    updated_at
) VALUES (
    100,
    'grok',
    'grok-4-fast',
    'YOUR-QINIU-API-KEY-HERE',      -- 请替换为您的七牛云API密钥
    'https://openai.qiniu.com/v1',  -- 七牛云API基础URL（自动识别并转换模型名称）
    NULL,                            -- 代理URL（如果需要）
    1,                               -- 状态：1=启用，0=禁用
    1,                               -- 权重（用于负载均衡）
    10000,                           -- 日限额（可选）
    300000,                          -- 月限额（可选）
    0,                               -- 总使用量
    0,                               -- 日使用量
    0,                               -- 月使用量
    NULL,                            -- 最后使用时间
    NULL,                            -- 健康检查时间
    1,                               -- 健康状态：1=健康
    NOW(),                           -- 创建时间
    NOW()                            -- 更新时间
);

-- 插入Grok 2 Latest的API密钥（备选模型）
INSERT INTO ai_api_keys (
    id,
    provider_code,
    model_code,
    api_key,
    base_url,
    proxy_url,
    status,
    weight,
    daily_limit,
    monthly_limit,
    total_used,
    daily_used,
    monthly_used,
    last_used_time,
    health_check_time,
    health_status,
    created_at,
    updated_at
) VALUES (
    101,
    'grok',
    'grok-2-latest',
    'YOUR-QINIU-API-KEY-HERE',      -- 可以使用同一个API密钥
    'https://openai.qiniu.com/v1',  -- 七牛云API基础URL
    NULL,
    1,
    1,
    10000,
    300000,
    0,
    0,
    0,
    NULL,
    NULL,
    1,
    NOW(),
    NOW()
);

-- 插入GPT OSS 20B的API密钥（如果也使用七牛云，同样配置七牛云URL）
INSERT INTO ai_api_keys (
    id,
    provider_code,
    model_code,
    api_key,
    base_url,
    proxy_url,
    status,
    weight,
    daily_limit,
    monthly_limit,
    total_used,
    daily_used,
    monthly_used,
    last_used_time,
    health_check_time,
    health_status,
    created_at,
    updated_at
) VALUES (
    102,
    'openai',
    'gpt-oss-20b',
    'sk-YOUR-OPENAI-API-KEY-HERE',  -- 请替换为您的实际OpenAI API密钥
    'https://api.openai.com/v1',     -- 或者您的自定义端点
    NULL,
    1,
    1,
    10000,
    300000,
    0,
    0,
    0,
    NULL,
    NULL,
    1,
    NOW(),
    NOW()
);

-- 注意事项：
-- 1. 七牛云API密钥：按七牛云提供的格式填写
-- 2. OpenAI API密钥：通常以 'sk-' 开头
-- 3. 模型名称自动转换：
--    - 七牛云API（base_url包含qiniu.com）：grok-4-fast -> x-ai/grok-4-fast
--    - 官方API（https://api.x.ai/v1）：直接使用 grok-4-fast
-- 4. 可以为同一个模型配置多个API密钥，通过weight字段实现负载均衡
-- 5. daily_limit和monthly_limit用于控制使用限额，设置为NULL表示不限制

-- 如果使用官方xAI API，请修改为：
-- UPDATE ai_api_keys SET 
--   api_key = 'xai-YOUR-KEY', 
--   base_url = 'https://api.x.ai/v1' 
-- WHERE id = 100;
