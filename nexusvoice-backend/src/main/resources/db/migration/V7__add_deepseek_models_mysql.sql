-- 添加DeepSeek模型配置
-- 作者: NexusVoice
-- 日期: 2025-10-19
-- 说明: 添加DeepSeek V3.1模型支持

-- 插入DeepSeek V3.1模型配置
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
    -- DeepSeek V3.1 主模型
    (13, 'deepseek', 'deepseek-v3.1', 'DeepSeek V3.1', 
     'DeepSeek V3.1 - 支持显式推理(Think)、动态搜索(Search)、工具调用(Tool)，128K上下文窗口', 
     'OpenAiChatModel', 'https://api.deepseek.com/v1', 0.7, 8000, 
     120, 131072, 0.004, 0.012, 1, 90),
    
    -- DeepSeek V3 备选版本（如果需要）
    (14, 'deepseek', 'deepseek-v3', 'DeepSeek V3', 
     'DeepSeek V3 - 上一代版本，稳定可靠', 
     'OpenAiChatModel', 'https://api.deepseek.com/v1', 0.7, 8000, 
     120, 131072, 0.004, 0.012, 1, 95);

-- 插入DeepSeek API密钥（七牛云代理）
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
    103,
    'deepseek',
    'deepseek-v3.1',
    'YOUR-QINIU-API-KEY-HERE',      -- 请替换为您的七牛云API密钥（可与Grok使用同一个）
    'https://openai.qiniu.com/v1',  -- 七牛云API基础URL
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

-- 备用：如果需要使用七牛云的备用URL
-- INSERT INTO ai_api_keys (
--     id, provider_code, model_code, api_key, base_url, ...
-- ) VALUES (
--     104,
--     'deepseek',
--     'deepseek-v3.1',
--     'YOUR-QINIU-API-KEY-HERE',
--     'https://api.qnaigc.com/v1',  -- 七牛云备用URL
--     ...
-- );

-- 备用：如果需要使用DeepSeek官方API
-- INSERT INTO ai_api_keys (
--     id, provider_code, model_code, api_key, base_url, ...
-- ) VALUES (
--     105,
--     'deepseek',
--     'deepseek-v3.1',
--     'sk-YOUR-DEEPSEEK-KEY-HERE',
--     'https://api.deepseek.com/v1',  -- DeepSeek官方API
--     ...
-- );

-- 注意事项：
-- 1. DeepSeek V3.1 特性：
--    - 显式推理（Think）：深度思考模式，适合复杂推理任务
--    - 动态搜索（Search）：实时搜索外部信息
--    - 工具调用（Tool）：高效调用外部工具和API
--    - 128K上下文窗口：支持处理超长文本
--    - 多模态支持：支持文本输入输出
-- 
-- 2. 价格说明（七牛云代理价格）：
--    - 非思考模式输入：0.004 元 / K tokens
--    - 非思考模式输出：0.012 元 / K tokens
--    - 思考模式输入：0.004 元 / K tokens（相同价格）
--    - 思考模式输出：0.012 元 / K tokens（相同价格）
--
-- 3. 七牛云代理的DeepSeek不需要模型名称转换
--    直接使用 deepseek-v3.1，不需要添加前缀
--
-- 4. API密钥可以与Grok共用（如果使用同一个七牛云账号）
--
-- 5. 推荐配置更大的timeout（120秒）以支持深度思考模式
