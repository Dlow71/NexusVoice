-- =====================================================
-- 开发者API密钥系统数据库表
-- 版本: V18
-- 作者: NexusVoice
-- 日期: 2025-10-29
-- 描述: 创建developer_api_keys表，支持开发者API Key认证
-- =====================================================

-- 设置schema
SET search_path TO nexusvoice;

-- =====================================================
-- 1. 创建开发者API密钥表
-- =====================================================
CREATE TABLE IF NOT EXISTS developer_api_keys (
    -- 主键
    id BIGINT PRIMARY KEY,
    
    -- 基本信息
    key_name VARCHAR(100) NOT NULL,
    key_value_hash VARCHAR(64) NOT NULL,
    key_prefix VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    app_name VARCHAR(100),
    
    -- 权限范围（JSONB数组）
    scopes JSONB,
    allowed_models JSONB,
    ip_whitelist JSONB,
    
    -- 请求限流配置
    rate_limit_per_minute INT DEFAULT 60,
    rate_limit_per_day INT DEFAULT 10000,
    
    -- 请求统计
    total_request_count BIGINT DEFAULT 0,
    today_request_count INT DEFAULT 0,
    last_request_at TIMESTAMP WITH TIME ZONE,
    
    -- Token统计
    total_input_tokens BIGINT DEFAULT 0,
    total_output_tokens BIGINT DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    today_tokens BIGINT DEFAULT 0,
    
    -- 费用统计
    daily_cost_limit DECIMAL(10,2),
    monthly_cost_limit DECIMAL(10,2),
    total_cost DECIMAL(10,2) DEFAULT 0,
    
    -- Token限额
    daily_token_limit BIGINT,
    monthly_token_limit BIGINT,
    
    -- 状态管理
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expire_at TIMESTAMP WITH TIME ZONE,
    last_used_ip VARCHAR(50),
    
    -- 审计字段
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

-- =====================================================
-- 2. 创建索引
-- =====================================================

-- 唯一索引：key_value_hash（未删除的记录唯一）
CREATE UNIQUE INDEX IF NOT EXISTS idx_developer_api_keys_hash 
    ON developer_api_keys(key_value_hash) 
    WHERE deleted = 0;

-- 唯一索引：user_id + key_name（同一用户的密钥名称唯一）
CREATE UNIQUE INDEX IF NOT EXISTS idx_developer_api_keys_user_name 
    ON developer_api_keys(user_id, key_name) 
    WHERE deleted = 0;

-- 普通索引：user_id（查询用户的所有密钥）
CREATE INDEX IF NOT EXISTS idx_developer_api_keys_user_id 
    ON developer_api_keys(user_id);

-- 普通索引：status（按状态查询）
CREATE INDEX IF NOT EXISTS idx_developer_api_keys_status 
    ON developer_api_keys(status);

-- 普通索引：deleted（查询已删除记录）
CREATE INDEX IF NOT EXISTS idx_developer_api_keys_deleted 
    ON developer_api_keys(deleted);

-- GIN索引：scopes（JSONB字段，支持权限范围查询）
CREATE INDEX IF NOT EXISTS idx_developer_api_keys_scopes 
    ON developer_api_keys USING GIN (scopes);

-- =====================================================
-- 3. 扩展ai_api_call_logs表（支持按SK统计）
-- =====================================================

-- 添加字段：developer_api_key_id（关联到具体SK）
ALTER TABLE ai_api_call_logs 
ADD COLUMN IF NOT EXISTS developer_api_key_id BIGINT;

-- 添加字段：auth_type（认证类型）
ALTER TABLE ai_api_call_logs 
ADD COLUMN IF NOT EXISTS auth_type VARCHAR(20);

-- 创建索引：developer_api_key_id（按SK查询调用日志）
CREATE INDEX IF NOT EXISTS idx_ai_api_call_logs_dev_api_key 
    ON ai_api_call_logs(developer_api_key_id);

-- 创建索引：auth_type（按认证类型查询）
CREATE INDEX IF NOT EXISTS idx_ai_api_call_logs_auth_type 
    ON ai_api_call_logs(auth_type);

-- =====================================================
-- 4. 添加注释
-- =====================================================

-- 表注释
COMMENT ON TABLE developer_api_keys IS '开发者API密钥表';

-- 字段注释
COMMENT ON COLUMN developer_api_keys.key_name IS '密钥名称（便于用户识别）';
COMMENT ON COLUMN developer_api_keys.key_value_hash IS 'API Key的SHA256哈希值（存储用）';
COMMENT ON COLUMN developer_api_keys.key_prefix IS 'API Key前缀（显示用，如sk-nv-abc123...)';
COMMENT ON COLUMN developer_api_keys.user_id IS '绑定的用户ID';
COMMENT ON COLUMN developer_api_keys.app_name IS '应用名称（可选）';
COMMENT ON COLUMN developer_api_keys.scopes IS '权限范围，JSON数组，如["chat","image","tts"]';
COMMENT ON COLUMN developer_api_keys.allowed_models IS '允许的模型列表，JSON数组，如["openai:gpt-4","doubao:seed-1.6"]';
COMMENT ON COLUMN developer_api_keys.ip_whitelist IS 'IP白名单，JSON数组，如["192.168.1.1","10.0.0.0/24"]';
COMMENT ON COLUMN developer_api_keys.rate_limit_per_minute IS '每分钟请求次数限制';
COMMENT ON COLUMN developer_api_keys.rate_limit_per_day IS '每日请求次数限制';
COMMENT ON COLUMN developer_api_keys.total_request_count IS '总请求次数';
COMMENT ON COLUMN developer_api_keys.today_request_count IS '今日请求次数';
COMMENT ON COLUMN developer_api_keys.last_request_at IS '最后请求时间';
COMMENT ON COLUMN developer_api_keys.total_input_tokens IS '总输入Token数';
COMMENT ON COLUMN developer_api_keys.total_output_tokens IS '总输出Token数';
COMMENT ON COLUMN developer_api_keys.total_tokens IS '总Token数（输入+输出）';
COMMENT ON COLUMN developer_api_keys.today_tokens IS '今日Token数';
COMMENT ON COLUMN developer_api_keys.daily_cost_limit IS '每日费用限额（元）';
COMMENT ON COLUMN developer_api_keys.monthly_cost_limit IS '每月费用限额（元）';
COMMENT ON COLUMN developer_api_keys.total_cost IS '总费用（元）';
COMMENT ON COLUMN developer_api_keys.daily_token_limit IS '每日Token限额';
COMMENT ON COLUMN developer_api_keys.monthly_token_limit IS '每月Token限额';
COMMENT ON COLUMN developer_api_keys.status IS '状态：ACTIVE-正常，DISABLED-禁用，EXPIRED-过期';
COMMENT ON COLUMN developer_api_keys.expire_at IS '过期时间（为空表示永不过期）';
COMMENT ON COLUMN developer_api_keys.last_used_ip IS '最后使用的IP地址';
COMMENT ON COLUMN developer_api_keys.created_at IS '创建时间';
COMMENT ON COLUMN developer_api_keys.updated_at IS '更新时间';
COMMENT ON COLUMN developer_api_keys.deleted IS '逻辑删除标识（0-未删除，1-已删除）';

-- ai_api_call_logs扩展字段注释
COMMENT ON COLUMN ai_api_call_logs.developer_api_key_id IS '开发者API Key ID（为null则为JWT用户直接调用）';
COMMENT ON COLUMN ai_api_call_logs.auth_type IS '认证类型：JWT/API_KEY';

-- =====================================================
-- 5. 完成提示
-- =====================================================

-- 打印成功信息（PostgreSQL使用RAISE NOTICE）
DO $$
BEGIN
    RAISE NOTICE '✅ V18迁移脚本执行成功！';
    RAISE NOTICE '✅ 已创建developer_api_keys表';
    RAISE NOTICE '✅ 已创建7个索引';
    RAISE NOTICE '✅ 已扩展ai_api_call_logs表（2个字段+2个索引）';
    RAISE NOTICE '⚠️  请在ai_api_keys表中配置真实的API密钥';
END $$;
