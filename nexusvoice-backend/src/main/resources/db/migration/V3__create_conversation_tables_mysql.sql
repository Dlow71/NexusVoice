-- 创建对话相关表 (MySQL版本)
-- 版本: V3
-- 描述: 创建对话表和对话消息表
-- 作者: NexusVoice
-- 日期: 2025-09-26

-- 创建对话表
CREATE TABLE conversations (
    -- 主键ID，使用BIGINT存储雪花ID
    id BIGINT NOT NULL PRIMARY KEY,
    
    -- 基本信息
    title VARCHAR(200) NOT NULL COMMENT '对话标题',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    model_name VARCHAR(100) NOT NULL COMMENT 'AI模型名称',
    
    -- 对话状态和配置
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '对话状态 (ACTIVE-活跃, ARCHIVED-归档)',
    system_prompt TEXT COMMENT '系统提示词',
    config_params JSON COMMENT '对话配置参数 (JSON格式)',
    
    -- 时间字段
    last_active_at DATETIME COMMENT '最后活跃时间',
    
    -- 基础字段（继承自BaseEntity）
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识 (0-未删除, 1-已删除)',
    
    -- 约束
    CONSTRAINT chk_conversation_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_conversation_deleted CHECK (deleted IN (0, 1)),
    CONSTRAINT fk_conversation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话表';

-- 创建对话消息表
CREATE TABLE conversation_messages (
    -- 主键ID，使用BIGINT存储雪花ID
    id BIGINT NOT NULL PRIMARY KEY,
    
    -- 关联信息
    conversation_id BIGINT NOT NULL COMMENT '对话ID',
    
    -- 消息内容
    role VARCHAR(20) NOT NULL COMMENT '消息角色 (USER-用户, ASSISTANT-助手, SYSTEM-系统)',
    content TEXT NOT NULL COMMENT '消息内容',
    sequence_num INT NOT NULL COMMENT '消息序号（在对话中的顺序）',
    
    -- 消息状态和元数据
    status VARCHAR(20) NOT NULL DEFAULT 'sent' COMMENT '消息状态 (sending-发送中, sent-已发送, failed-失败)',
    token_count INT COMMENT '令牌数量',
    error_message TEXT COMMENT '错误信息（如果发送失败）',
    metadata JSON COMMENT '消息元数据（JSON格式）',
    
    -- 时间字段
    sent_at DATETIME COMMENT '消息发送时间',
    
    -- 基础字段（继承自BaseEntity）
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识 (0-未删除, 1-已删除)',
    
    -- 约束
    CONSTRAINT chk_message_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    CONSTRAINT chk_message_status CHECK (status IN ('sending', 'sent', 'failed')),
    CONSTRAINT chk_message_deleted CHECK (deleted IN (0, 1)),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    
    -- 唯一约束：同一对话中的消息序号不能重复
    UNIQUE KEY uk_conversation_sequence (conversation_id, sequence_num, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息表';

-- 创建索引
-- 对话表索引
CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_conversations_status ON conversations(status);
CREATE INDEX idx_conversations_last_active ON conversations(last_active_at DESC);
CREATE INDEX idx_conversations_created_at ON conversations(created_at DESC);
CREATE INDEX idx_conversations_deleted ON conversations(deleted);

-- 对话消息表索引
CREATE INDEX idx_messages_conversation_id ON conversation_messages(conversation_id);
CREATE INDEX idx_messages_role ON conversation_messages(role);
CREATE INDEX idx_messages_status ON conversation_messages(status);
CREATE INDEX idx_messages_sequence ON conversation_messages(conversation_id, sequence_num);
CREATE INDEX idx_messages_sent_at ON conversation_messages(sent_at DESC);
CREATE INDEX idx_messages_deleted ON conversation_messages(deleted);

-- 创建复合索引（用于分页查询）
CREATE INDEX idx_conversations_user_status ON conversations(user_id, status, deleted);
CREATE INDEX idx_conversations_user_active ON conversations(user_id, last_active_at DESC, deleted);
CREATE INDEX idx_messages_conv_sequence ON conversation_messages(conversation_id, sequence_num, deleted);
CREATE INDEX idx_messages_conv_role ON conversation_messages(conversation_id, role, deleted);
