-- 创建用户表
-- 版本: V2
-- 描述: 创建用户表，支持管理员和普通用户
-- 作者: NexusVoice
-- 日期: 2025-09-23

CREATE TABLE users (
    -- 主键ID，使用UUID
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    
    -- 基本信息
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(255),
    phone VARCHAR(20) UNIQUE,
    
    -- 用户类型和状态
    user_type VARCHAR(20) NOT NULL DEFAULT 'USER',
    status INTEGER NOT NULL DEFAULT 1,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- 扩展信息
    last_login_at TIMESTAMP,
    profile_bio TEXT,
    
    -- 基础字段（继承自BaseEntity）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    
    -- 约束
    CONSTRAINT chk_user_type CHECK (user_type IN ('USER', 'ADMIN')),
    CONSTRAINT chk_status CHECK (status IN (1, 2, 3)),
    CONSTRAINT chk_deleted CHECK (deleted IN (0, 1))
);

-- 创建索引
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_deleted ON users(deleted);

-- 创建复合索引（用于分页查询）
CREATE INDEX idx_users_status_type ON users(status, user_type);
CREATE INDEX idx_users_deleted_created ON users(deleted, created_at DESC);

-- 添加表注释
COMMENT ON TABLE users IS '用户表';

-- 添加字段注释
COMMENT ON COLUMN users.id IS '用户唯一ID (UUID)';
COMMENT ON COLUMN users.email IS '用户登录邮箱';
COMMENT ON COLUMN users.password_hash IS '加密后的用户密码';
COMMENT ON COLUMN users.nickname IS '用户昵称';
COMMENT ON COLUMN users.avatar_url IS '用户头像图片的URL';
COMMENT ON COLUMN users.phone IS '手机号码';
COMMENT ON COLUMN users.user_type IS '用户类型 (USER-普通用户, ADMIN-管理员)';
COMMENT ON COLUMN users.status IS '账户状态 (1-正常, 2-封禁, 3-待激活)';
COMMENT ON COLUMN users.email_verified IS '邮箱是否已验证';
COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
COMMENT ON COLUMN users.profile_bio IS '用户简介';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';
COMMENT ON COLUMN users.deleted IS '逻辑删除标识 (0-未删除, 1-已删除)';

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 创建触发器，自动更新updated_at字段
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- 插入默认管理员用户（密码为：admin123，使用BCrypt加密）
INSERT INTO users (
    id, 
    email, 
    password_hash, 
    nickname, 
    user_type, 
    status, 
    email_verified,
    created_at,
    updated_at
) VALUES (
    'admin-uuid-0000-0000-000000000001',
    'admin@nexusvoice.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFVMLVZqpjn/6M.ltU6Td4e', -- admin123
    '系统管理员',
    'ADMIN',
    1,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;
