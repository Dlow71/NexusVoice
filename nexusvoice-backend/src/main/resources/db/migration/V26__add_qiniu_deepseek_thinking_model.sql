-- 新增七牛云 DeepSeek V3.2 思考模型，并切换为默认聊天模型

INSERT INTO ai_models (
    id,
    provider_id,
    provider_code,
    model_code,
    is_official,
    user_id,
    model_type,
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
    config_json,
    capabilities,
    input_types,
    output_types,
    status,
    priority,
    created_at,
    updated_at,
    deleted
)
SELECT
    1971856543655461236,
    p.id,
    'qiniu',
    'deepseek/deepseek-v3.2-251201',
    TRUE,
    NULL,
    'chat',
    'DeepSeek V3.2 Thinking',
    '七牛云代理的 DeepSeek V3.2 思考模型，支持 reasoning_content 流式输出',
    'OpenAiChatModel',
    'https://api.qnaigc.com/v1',
    0.70,
    8192,
    120,
    128000,
    0.0008,
    0.0020,
    '{"nativeThinkingProtocol":true,"thinkingModes":["disabled","auto","enabled"],"supportsReasoningContent":true,"reasoningResponseField":"reasoning_content","backupBaseUrl":"https://openai.sufy.com/v1"}',
    ARRAY['thinking']::text[],
    ARRAY['text']::text[],
    ARRAY['text']::text[],
    1,
    15,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM ai_providers p
WHERE p.provider_code = 'qiniu'
  AND NOT EXISTS (
    SELECT 1
    FROM ai_models m
    WHERE m.provider_code = 'qiniu'
      AND m.model_code = 'deepseek/deepseek-v3.2-251201'
      AND m.model_type = 'chat'
      AND m.deleted = 0
  );

INSERT INTO ai_api_keys (
    id,
    provider_id,
    provider_code,
    model_code,
    api_key,
    base_url,
    weight,
    status,
    fail_count,
    total_requests,
    total_tokens_used,
    total_cost,
    monthly_requests,
    monthly_tokens_used,
    monthly_cost,
    daily_tokens_used,
    created_at,
    updated_at,
    deleted
)
SELECT
    1971856543655461237,
    p.id,
    'qiniu',
    'deepseek/deepseek-v3.2-251201',
    source.api_key,
    'https://api.qnaigc.com/v1',
    COALESCE(source.weight, 1),
    1,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM ai_api_keys source
JOIN ai_providers p ON p.provider_code = 'qiniu'
WHERE source.provider_code = 'deepseek'
  AND source.model_code = 'deepseek-v3.1'
  AND source.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM ai_api_keys target
    WHERE target.provider_code = 'qiniu'
      AND target.model_code = 'deepseek/deepseek-v3.2-251201'
      AND target.deleted = 0
  )
LIMIT 1;

UPDATE system_config
SET config_value = 'qiniu:deepseek/deepseek-v3.2-251201',
    updated_at = CURRENT_TIMESTAMP
WHERE config_key = 'ai.model.default';

UPDATE system_config
SET config_value = 'qiniu',
    updated_at = CURRENT_TIMESTAMP
WHERE config_key = 'ai.model.default.provider';

UPDATE system_config
SET config_value = 'deepseek/deepseek-v3.2-251201',
    updated_at = CURRENT_TIMESTAMP
WHERE config_key = 'ai.model.default.code';
