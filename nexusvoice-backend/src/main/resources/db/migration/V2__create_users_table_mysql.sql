-- 创建用户表 (MySQL版本)
-- 版本: V2
-- 描述: 创建用户表，支持管理员和普通用户
-- 作者: NexusVoice
-- 日期: 2025-09-23

CREATE TABLE users (
    -- 主键ID，使用VARCHAR存储UUID
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    
    -- 基本信息
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(255),
    phone VARCHAR(20) UNIQUE,
    
    -- 用户类型和状态
    user_type VARCHAR(20) NOT NULL DEFAULT 'USER',
    status INT NOT NULL DEFAULT 1,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- 扩展信息
    last_login_at DATETIME,
    profile_bio TEXT,
    
    -- 基础字段（继承自BaseEntity）
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    
    -- 约束
    CONSTRAINT chk_user_type CHECK (user_type IN ('USER', 'ADMIN')),
    CONSTRAINT chk_status CHECK (status IN (1, 2, 3)),
    CONSTRAINT chk_deleted CHECK (deleted IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
ALTER TABLE users COMMENT = '用户表';

-- 添加字段注释
ALTER TABLE users 
MODIFY COLUMN id VARCHAR(36) NOT NULL COMMENT '用户唯一ID (UUID)',
MODIFY COLUMN email VARCHAR(100) NOT NULL COMMENT '用户登录邮箱',
MODIFY COLUMN password_hash VARCHAR(255) NOT NULL COMMENT '加密后的用户密码',
MODIFY COLUMN nickname VARCHAR(50) NOT NULL COMMENT '用户昵称',
MODIFY COLUMN avatar_url VARCHAR(255) COMMENT '用户头像图片的URL',
MODIFY COLUMN phone VARCHAR(20) COMMENT '手机号码',
MODIFY COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '用户类型 (USER-普通用户, ADMIN-管理员)',
MODIFY COLUMN status INT NOT NULL DEFAULT 1 COMMENT '账户状态 (1-正常, 2-封禁, 3-待激活)',
MODIFY COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT '邮箱是否已验证',
MODIFY COLUMN last_login_at DATETIME COMMENT '最后登录时间',
MODIFY COLUMN profile_bio TEXT COMMENT '用户简介',
MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
MODIFY COLUMN deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识 (0-未删除, 1-已删除)';

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
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE id=id;
