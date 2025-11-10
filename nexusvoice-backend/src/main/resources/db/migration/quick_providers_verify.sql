-- ============================================================================
-- 快速验证和管理AI服务提供商
-- 使用方法：在psql或DBeaver中直接执行
-- ============================================================================

-- ============================================================================
-- 1. 查看所有服务商及其模型统计
-- ============================================================================
SELECT * FROM fn_count_models_by_provider();

-- ============================================================================
-- 2. 查看服务商详细信息
-- ============================================================================
SELECT 
    id,
    provider_code,
    provider_name,
    protocol,
    default_base_url,
    is_official,
    status,
    priority,
    created_at
FROM ai_providers
ORDER BY priority;

-- ============================================================================
-- 3. 查看模型与服务商的关联情况（前20条）
-- ============================================================================
SELECT 
    model_key,
    model_name,
    model_type,
    provider_name,
    protocol,
    effective_base_url,
    model_status,
    provider_status
FROM v_ai_models_with_provider
ORDER BY provider_code, model_code
LIMIT 20;

-- ============================================================================
-- 4. 检查未关联的模型（应该为0）
-- ============================================================================
SELECT 
    id,
    provider_code,
    model_code,
    model_name,
    'ai_models表中未关联provider_id' AS issue
FROM ai_models
WHERE provider_id IS NULL AND deleted = 0;

-- ============================================================================
-- 5. 检查未关联的API密钥（应该为0）
-- ============================================================================
SELECT 
    id,
    provider_code,
    model_code,
    'ai_api_keys表中未关联provider_id' AS issue
FROM ai_api_keys
WHERE provider_id IS NULL AND deleted = 0;

-- ============================================================================
-- 6. 用户添加自定义服务商示例（例如：私有部署的Ollama）
-- ============================================================================
-- 注意：user_id需要替换为实际的用户ID

-- 示例1：添加Ollama本地服务
/*
INSERT INTO ai_providers (
    id, 
    provider_code, 
    provider_name, 
    protocol, 
    description, 
    default_base_url, 
    is_official, 
    user_id, 
    status, 
    priority, 
    created_at, 
    updated_at
) VALUES (
    10001,  -- 用户自定义ID从10001开始
    'ollama_local',
    'Ollama本地服务',
    'openai_compatible',
    '本地部署的Ollama大模型服务',
    'http://localhost:11434/v1',
    FALSE,
    1,  -- 替换为实际user_id
    1,
    1000,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
*/

-- 示例2：添加自定义的OpenAI兼容API
/*
INSERT INTO ai_providers (
    id, 
    provider_code, 
    provider_name, 
    protocol, 
    description, 
    default_base_url, 
    config_json,
    is_official, 
    user_id, 
    status, 
    priority, 
    created_at, 
    updated_at
) VALUES (
    10002,
    'custom_openai_api',
    '企业内部LLM服务',
    'openai_compatible',
    '公司内部部署的大模型API服务',
    'https://internal-llm.company.com/v1',
    '{"proxy_required": false, "timeout": 120}',
    FALSE,
    1,  -- 替换为实际user_id
    1,
    1001,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
*/

-- ============================================================================
-- 7. 为自定义服务商添加模型配置
-- ============================================================================
-- 前提：先执行上面的INSERT ai_providers语句

-- 为Ollama添加llama3.3模型
/*
INSERT INTO ai_models (
    id,
    provider_id,
    provider_code,
    model_code,
    model_name,
    model_type,
    description,
    default_base_url,
    context_window,
    default_temperature,
    default_max_tokens,
    input_token_price,
    output_token_price,
    is_official,
    user_id,
    status,
    priority,
    created_at,
    updated_at
) VALUES (
    20001,
    10001,  -- provider_id对应上面创建的Ollama
    'ollama_local',
    'llama3.3',
    'Llama 3.3 70B (本地)',
    'chat',
    '本地部署的Llama 3.3 70B模型',
    'http://localhost:11434/v1',
    131072,
    0.7,
    4096,
    0,  -- 本地模型免费
    0,
    FALSE,
    1,  -- 替换为实际user_id
    1,
    100,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
*/

-- ============================================================================
-- 8. 为自定义模型添加API密钥（本地服务一般不需要）
-- ============================================================================
/*
INSERT INTO ai_api_keys (
    id,
    provider_id,
    provider_code,
    model_code,
    api_key,
    base_url,
    weight,
    status,
    created_at,
    updated_at
) VALUES (
    30001,
    10001,
    'ollama_local',
    'llama3.3',
    'not-required',  -- Ollama不需要API Key
    'http://localhost:11434/v1',
    1,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
*/

-- ============================================================================
-- 9. 管理服务商状态
-- ============================================================================

-- 禁用某个服务商
-- UPDATE ai_providers SET status = 0 WHERE provider_code = 'claude';

-- 启用某个服务商
-- UPDATE ai_providers SET status = 1 WHERE provider_code = 'claude';

-- 删除用户自定义服务商（逻辑删除）
-- UPDATE ai_providers SET deleted = 1 WHERE id = 10001 AND is_official = FALSE;

-- ============================================================================
-- 10. 查询特定服务商的所有模型
-- ============================================================================
SELECT 
    m.id,
    m.model_code,
    m.model_name,
    m.model_type,
    m.status,
    m.is_official,
    m.default_base_url
FROM ai_models m
INNER JOIN ai_providers p ON m.provider_id = p.id
WHERE p.provider_code = 'siliconflow'  -- 可替换为其他provider_code
ORDER BY m.model_type, m.priority;

-- ============================================================================
-- 11. 统计各协议类型的服务商数量
-- ============================================================================
SELECT 
    protocol,
    COUNT(*) AS provider_count,
    ARRAY_AGG(provider_name ORDER BY priority) AS providers
FROM ai_providers
WHERE deleted = 0 AND status = 1
GROUP BY protocol
ORDER BY provider_count DESC;

-- ============================================================================
-- 12. 查询可用的服务商和模型组合（用于前端展示）
-- ============================================================================
SELECT 
    p.provider_code,
    p.provider_name,
    p.protocol,
    m.model_code,
    m.model_name,
    m.model_type,
    CONCAT(p.provider_code, ':', m.model_code) AS model_key,
    COALESCE(m.default_base_url, p.default_base_url) AS api_endpoint
FROM ai_providers p
INNER JOIN ai_models m ON p.id = m.provider_id
WHERE p.deleted = 0 
  AND p.status = 1
  AND m.deleted = 0
  AND m.status = 1
ORDER BY 
    p.priority,
    m.model_type,
    m.priority;

-- ============================================================================
-- 13. 清理测试数据（谨慎使用）
-- ============================================================================
-- 如果需要回滚，可以删除provider_id字段
-- 注意：这会删除外键约束和索引

/*
-- 警告：执行前请备份数据！
ALTER TABLE ai_models DROP CONSTRAINT IF EXISTS fk_ai_models_provider;
ALTER TABLE ai_api_keys DROP CONSTRAINT IF EXISTS fk_ai_api_keys_provider;

ALTER TABLE ai_models DROP COLUMN IF EXISTS provider_id;
ALTER TABLE ai_models DROP COLUMN IF EXISTS is_official;
ALTER TABLE ai_models DROP COLUMN IF EXISTS user_id;

ALTER TABLE ai_api_keys DROP COLUMN IF EXISTS provider_id;
ALTER TABLE ai_api_call_logs DROP COLUMN IF EXISTS provider_id;

DROP VIEW IF EXISTS v_ai_models_with_provider;
DROP FUNCTION IF EXISTS fn_count_models_by_provider();
DROP TABLE IF EXISTS ai_providers;
*/
