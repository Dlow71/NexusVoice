-- ============================================================================
-- V20: 添加AI服务提供商表（Provider层）
-- 功能：引入服务商抽象层，支持用户自定义服务商
-- 作者：NexusVoice
-- 日期：2025-01-11
-- ============================================================================

-- ============================================================================
-- 第一步：创建 ai_providers 表
-- ============================================================================
CREATE TABLE IF NOT EXISTS ai_providers (
    id BIGINT PRIMARY KEY,
    provider_code VARCHAR(50) NOT NULL UNIQUE,
    provider_name VARCHAR(100) NOT NULL,
    protocol VARCHAR(50) NOT NULL DEFAULT 'openai_compatible',
    description TEXT,
    
    -- 厂商级配置
    default_base_url VARCHAR(500),
    config_json TEXT,
    
    -- 官方/用户标识
    is_official BOOLEAN DEFAULT TRUE,
    user_id BIGINT,
    
    -- 状态管理
    status SMALLINT DEFAULT 1,
    priority INTEGER DEFAULT 100,
    
    -- 审计字段
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0,
    
    -- 约束：官方服务商user_id为NULL，用户自定义服务商必须有user_id
    CONSTRAINT chk_official_or_user CHECK (
        (is_official = TRUE AND user_id IS NULL) OR 
        (is_official = FALSE AND user_id IS NOT NULL)
    )
);

-- 添加注释
COMMENT ON TABLE ai_providers IS 'AI服务提供商表';
COMMENT ON COLUMN ai_providers.id IS '主键ID';
COMMENT ON COLUMN ai_providers.provider_code IS '厂商代码：openai/grok/deepseek等（唯一）';
COMMENT ON COLUMN ai_providers.provider_name IS '厂商显示名称';
COMMENT ON COLUMN ai_providers.protocol IS '协议类型：openai_compatible/anthropic/dashscope/custom';
COMMENT ON COLUMN ai_providers.description IS '厂商描述';
COMMENT ON COLUMN ai_providers.default_base_url IS '默认API端点URL';
COMMENT ON COLUMN ai_providers.config_json IS '厂商特定配置JSON（可加密存储）';
COMMENT ON COLUMN ai_providers.is_official IS '是否官方内置服务商';
COMMENT ON COLUMN ai_providers.user_id IS '用户自定义服务商的创建者ID';
COMMENT ON COLUMN ai_providers.status IS '状态：0-禁用 1-启用';
COMMENT ON COLUMN ai_providers.priority IS '优先级（越小越优先）';

-- 创建索引
CREATE INDEX idx_ai_providers_code ON ai_providers(provider_code);
CREATE INDEX idx_ai_providers_user ON ai_providers(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_ai_providers_status ON ai_providers(status) WHERE status = 1;

-- ============================================================================
-- 第二步：插入官方服务商数据
-- ============================================================================
INSERT INTO ai_providers (id, provider_code, provider_name, protocol, description, default_base_url, is_official, status, priority, created_at, updated_at) VALUES
(1, 'openai', 'OpenAI', 'openai_compatible', 'OpenAI官方API服务，支持GPT系列模型', 'https://api.openai.com/v1', TRUE, 1, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'grok', 'xAI Grok', 'openai_compatible', 'xAI Grok API服务（通过七牛云代理）', 'https://openai.qiniu.com/v1', TRUE, 1, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'deepseek', 'DeepSeek', 'openai_compatible', 'DeepSeek深度思考模型API服务', 'https://api.deepseek.com/v1', TRUE, 1, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'doubao', '豆包（字节跳动）', 'openai_compatible', '字节跳动豆包多模态大模型API服务', 'https://openai.qiniu.com/v1', TRUE, 1, 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'siliconflow', '硅基流动', 'openai_compatible', '硅基流动API服务，支持Embedding和Rerank', 'https://api.siliconflow.cn/v1', TRUE, 1, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'qwen', '通义千问（阿里云）', 'dashscope', '阿里云通义千问大模型服务', 'https://dashscope.aliyuncs.com/compatible-mode/v1', TRUE, 1, 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'zhipu', '智谱AI', 'openai_compatible', '智谱AI大模型服务（GLM系列）', 'https://open.bigmodel.cn/api/paas/v4', TRUE, 1, 70, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'claude', 'Claude (Anthropic)', 'anthropic', 'Anthropic Claude系列模型API服务', 'https://api.anthropic.com', TRUE, 0, 80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'qiniu', '七牛云AI', 'openai_compatible', '七牛云AI服务代理平台', 'https://openai.qiniu.com/v1', TRUE, 1, 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================================
-- 第三步：修改 ai_models 表，添加 provider_id 和相关字段
-- ============================================================================

-- 添加新字段
ALTER TABLE ai_models ADD COLUMN IF NOT EXISTS provider_id BIGINT;
ALTER TABLE ai_models ADD COLUMN IF NOT EXISTS is_official BOOLEAN DEFAULT TRUE;
ALTER TABLE ai_models ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- 添加注释
COMMENT ON COLUMN ai_models.provider_id IS '服务商ID，外键关联ai_providers.id';
COMMENT ON COLUMN ai_models.is_official IS '是否官方内置模型';
COMMENT ON COLUMN ai_models.user_id IS '用户自定义模型的创建者ID';

-- ============================================================================
-- 第四步：数据迁移 - 根据 provider_code 关联 provider_id
-- ============================================================================
UPDATE ai_models m 
SET provider_id = p.id
FROM ai_providers p
WHERE m.provider_code = p.provider_code;

-- 验证迁移结果（查询未关联的记录）
DO $$
DECLARE
    unmapped_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO unmapped_count
    FROM ai_models
    WHERE provider_id IS NULL;
    
    IF unmapped_count > 0 THEN
        RAISE NOTICE '警告：有 % 条ai_models记录未能关联到provider_id，请检查provider_code', unmapped_count;
    ELSE
        RAISE NOTICE '成功：所有ai_models记录已关联到provider_id';
    END IF;
END $$;

-- ============================================================================
-- 第五步：添加外键约束和索引
-- ============================================================================

-- 添加外键约束（如果provider_id全部关联成功）
ALTER TABLE ai_models 
ADD CONSTRAINT fk_ai_models_provider 
FOREIGN KEY (provider_id) REFERENCES ai_providers(id)
ON DELETE RESTRICT
ON UPDATE CASCADE;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_ai_models_provider_id ON ai_models(provider_id);
CREATE INDEX IF NOT EXISTS idx_ai_models_user_id ON ai_models(user_id) WHERE user_id IS NOT NULL;

-- ============================================================================
-- 第六步：修改 ai_api_keys 表，添加 provider_id
-- ============================================================================

-- 添加新字段
ALTER TABLE ai_api_keys ADD COLUMN IF NOT EXISTS provider_id BIGINT;

-- 添加注释
COMMENT ON COLUMN ai_api_keys.provider_id IS '服务商ID，外键关联ai_providers.id';

-- 数据迁移：根据 provider_code 关联 provider_id
UPDATE ai_api_keys k 
SET provider_id = p.id
FROM ai_providers p
WHERE k.provider_code = p.provider_code;

-- 验证迁移结果
DO $$
DECLARE
    unmapped_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO unmapped_count
    FROM ai_api_keys
    WHERE provider_id IS NULL;
    
    IF unmapped_count > 0 THEN
        RAISE NOTICE '警告：有 % 条ai_api_keys记录未能关联到provider_id，请检查provider_code', unmapped_count;
    ELSE
        RAISE NOTICE '成功：所有ai_api_keys记录已关联到provider_id';
    END IF;
END $$;

-- 添加外键约束
ALTER TABLE ai_api_keys 
ADD CONSTRAINT fk_ai_api_keys_provider 
FOREIGN KEY (provider_id) REFERENCES ai_providers(id)
ON DELETE RESTRICT
ON UPDATE CASCADE;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_ai_api_keys_provider_id ON ai_api_keys(provider_id);

-- ============================================================================
-- 第七步：修改 ai_api_call_logs 表，添加 provider_id（可选）
-- ============================================================================

ALTER TABLE ai_api_call_logs ADD COLUMN IF NOT EXISTS provider_id BIGINT;

COMMENT ON COLUMN ai_api_call_logs.provider_id IS '服务商ID，外键关联ai_providers.id（冗余字段，便于统计）';

-- 数据迁移
UPDATE ai_api_call_logs l 
SET provider_id = p.id
FROM ai_providers p
WHERE l.provider_code = p.provider_code;

-- 创建索引（用于统计分析）
CREATE INDEX IF NOT EXISTS idx_ai_api_call_logs_provider_id ON ai_api_call_logs(provider_id);

-- ============================================================================
-- 第八步：创建视图 - 方便查询带服务商信息的模型
-- ============================================================================
CREATE OR REPLACE VIEW v_ai_models_with_provider AS
SELECT 
    m.id,
    m.provider_code,
    m.model_code,
    m.model_name,
    m.model_type,
    m.status AS model_status,
    m.priority AS model_priority,
    m.is_official AS model_is_official,
    m.user_id AS model_user_id,
    -- 服务商信息
    p.id AS provider_id,
    p.provider_name,
    p.protocol,
    p.default_base_url AS provider_base_url,
    p.is_official AS provider_is_official,
    p.status AS provider_status,
    -- 组合信息
    CONCAT(m.provider_code, ':', m.model_code) AS model_key,
    m.default_base_url AS model_custom_base_url,
    COALESCE(m.default_base_url, p.default_base_url) AS effective_base_url,
    -- 审计字段
    m.created_at,
    m.updated_at
FROM ai_models m
INNER JOIN ai_providers p ON m.provider_id = p.id
WHERE m.deleted = 0 AND p.deleted = 0;

COMMENT ON VIEW v_ai_models_with_provider IS '带服务商信息的AI模型视图';

-- ============================================================================
-- 第九步：创建统计函数 - 查询服务商模型数量
-- ============================================================================
CREATE OR REPLACE FUNCTION fn_count_models_by_provider()
RETURNS TABLE (
    provider_code VARCHAR(50),
    provider_name VARCHAR(100),
    total_models BIGINT,
    enabled_models BIGINT,
    chat_models BIGINT,
    embedding_models BIGINT,
    rerank_models BIGINT,
    image_models BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.provider_code,
        p.provider_name,
        COUNT(*) AS total_models,
        COUNT(*) FILTER (WHERE m.status = 1) AS enabled_models,
        COUNT(*) FILTER (WHERE m.model_type = 'chat') AS chat_models,
        COUNT(*) FILTER (WHERE m.model_type = 'embedding') AS embedding_models,
        COUNT(*) FILTER (WHERE m.model_type = 'rerank') AS rerank_models,
        COUNT(*) FILTER (WHERE m.model_type = 'image') AS image_models
    FROM ai_providers p
    LEFT JOIN ai_models m ON p.id = m.provider_id AND m.deleted = 0
    WHERE p.deleted = 0
    GROUP BY p.provider_code, p.provider_name, p.priority
    ORDER BY p.priority, p.provider_code;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_count_models_by_provider() IS '统计每个服务商的模型数量';

-- ============================================================================
-- 第十步：数据验证和输出统计信息
-- ============================================================================

-- 输出统计信息
DO $$
DECLARE
    provider_count INTEGER;
    model_count INTEGER;
    apikey_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO provider_count FROM ai_providers WHERE deleted = 0;
    SELECT COUNT(*) INTO model_count FROM ai_models WHERE deleted = 0;
    SELECT COUNT(*) INTO apikey_count FROM ai_api_keys WHERE deleted = 0;
    
    RAISE NOTICE '==========================================================';
    RAISE NOTICE 'AI服务提供商表迁移完成！';
    RAISE NOTICE '==========================================================';
    RAISE NOTICE '服务商数量: %', provider_count;
    RAISE NOTICE '模型数量: %', model_count;
    RAISE NOTICE 'API密钥数量: %', apikey_count;
    RAISE NOTICE '==========================================================';
    RAISE NOTICE '请检查以下查询结果：';
    RAISE NOTICE '1. SELECT * FROM fn_count_models_by_provider();';
    RAISE NOTICE '2. SELECT * FROM v_ai_models_with_provider LIMIT 5;';
    RAISE NOTICE '==========================================================';
END $$;
