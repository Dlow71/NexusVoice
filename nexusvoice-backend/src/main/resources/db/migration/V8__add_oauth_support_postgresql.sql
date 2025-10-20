-- V8: OAuth第三方登录支持
-- 描述: 为users表添加OAuth相关字段，支持GitHub等第三方登录
-- 作者: NexusVoice
-- 日期: 2025-01-20

-- 1. 添加OAuth相关字段
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(50),          -- OAuth提供商：github/google/wechat等
ADD COLUMN IF NOT EXISTS oauth_id VARCHAR(255),               -- OAuth用户在第三方平台的唯一ID
ADD COLUMN IF NOT EXISTS oauth_username VARCHAR(100),         -- OAuth用户在第三方平台的用户名
ADD COLUMN IF NOT EXISTS oauth_avatar_url VARCHAR(500),       -- OAuth用户在第三方平台的头像URL
ADD COLUMN IF NOT EXISTS oauth_access_token TEXT,             -- OAuth访问令牌（加密存储，可选）
ADD COLUMN IF NOT EXISTS oauth_refresh_token TEXT,            -- OAuth刷新令牌（加密存储，可选）
ADD COLUMN IF NOT EXISTS oauth_token_expires_at TIMESTAMP,    -- OAuth令牌过期时间
ADD COLUMN IF NOT EXISTS oauth_bind_time TIMESTAMP,           -- OAuth绑定时间
ADD COLUMN IF NOT EXISTS oauth_raw_data JSONB;                -- OAuth原始用户数据（备用）

-- 2. 修改password_hash为可空（OAuth用户可能没有密码）
ALTER TABLE users 
ALTER COLUMN password_hash DROP NOT NULL;

-- 3. 创建唯一索引（同一个OAuth提供商的用户ID只能绑定一个系统账号）
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_oauth_unique 
ON users(oauth_provider, oauth_id) 
WHERE oauth_provider IS NOT NULL AND oauth_id IS NOT NULL;

-- 4. 创建普通索引（用于查询）
CREATE INDEX IF NOT EXISTS idx_users_oauth_provider 
ON users(oauth_provider) 
WHERE oauth_provider IS NOT NULL;

-- 5. 添加约束：确保OAuth用户和密码用户的数据完整性
ALTER TABLE users 
ADD CONSTRAINT chk_auth_method CHECK (
    -- 要么是密码用户（有密码）
    (password_hash IS NOT NULL) OR 
    -- 要么是OAuth用户（有OAuth信息）
    (oauth_provider IS NOT NULL AND oauth_id IS NOT NULL)
);

-- 6. 添加字段注释
COMMENT ON COLUMN users.oauth_provider IS 'OAuth提供商标识：github/google/wechat等';
COMMENT ON COLUMN users.oauth_id IS 'OAuth用户在第三方平台的唯一标识';
COMMENT ON COLUMN users.oauth_username IS 'OAuth用户在第三方平台的用户名';
COMMENT ON COLUMN users.oauth_avatar_url IS 'OAuth用户在第三方平台的头像URL';
COMMENT ON COLUMN users.oauth_access_token IS 'OAuth访问令牌（加密存储）';
COMMENT ON COLUMN users.oauth_refresh_token IS 'OAuth刷新令牌（加密存储）';
COMMENT ON COLUMN users.oauth_token_expires_at IS 'OAuth令牌过期时间';
COMMENT ON COLUMN users.oauth_bind_time IS 'OAuth账号绑定时间';
COMMENT ON COLUMN users.oauth_raw_data IS 'OAuth原始用户数据，JSON格式存储';

-- 7. 创建OAuth提供商枚举类型（可选，用于严格限制provider值）
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'oauth_provider_enum') THEN
        CREATE TYPE oauth_provider_enum AS ENUM ('github', 'google', 'wechat', 'qq', 'weibo', 'microsoft');
    END IF;
END $$;

-- 注意：暂时不使用枚举类型，保持灵活性，使用VARCHAR
-- 如果未来需要严格限制，可以执行：
-- ALTER TABLE users ALTER COLUMN oauth_provider TYPE oauth_provider_enum USING oauth_provider::oauth_provider_enum;
