-- 回填 AI 服务商关联字段，确保 provider_id / provider_code 一致

UPDATE ai_models m
SET provider_id = p.id,
    provider_code = p.provider_code,
    updated_at = NOW()
FROM ai_providers p
WHERE m.deleted = 0
  AND p.deleted = 0
  AND (
    (m.provider_id IS NULL AND m.provider_code = p.provider_code)
    OR (m.provider_id = p.id AND m.provider_code IS DISTINCT FROM p.provider_code)
  );

UPDATE ai_api_keys k
SET provider_id = p.id,
    provider_code = p.provider_code,
    updated_at = NOW()
FROM ai_providers p
WHERE k.deleted = 0
  AND p.deleted = 0
  AND (
    (k.provider_id IS NULL AND k.provider_code = p.provider_code)
    OR (k.provider_id = p.id AND k.provider_code IS DISTINCT FROM p.provider_code)
  );

UPDATE ai_api_call_logs l
SET provider_id = p.id,
    provider_code = p.provider_code,
    updated_at = NOW()
FROM ai_providers p
WHERE p.deleted = 0
  AND (
    (l.provider_id IS NULL AND l.provider_code = p.provider_code)
    OR (l.provider_id = p.id AND l.provider_code IS DISTINCT FROM p.provider_code)
  );
