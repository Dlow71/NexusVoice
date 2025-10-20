-- =====================================================
-- NexusVoice PostgreSQL数据库初始化脚本
-- 基于Java实体类生成
-- =====================================================

-- 1. 用户表 (users)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(500) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    user_type VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP,
    profile_bio TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_deleted ON users(deleted);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '主键ID（雪花ID）';
COMMENT ON COLUMN users.email IS '用户登录邮箱';
COMMENT ON COLUMN users.password_hash IS '加密后的用户密码';
COMMENT ON COLUMN users.nickname IS '用户昵称';
COMMENT ON COLUMN users.avatar_url IS '用户头像URL';
COMMENT ON COLUMN users.phone IS '手机号码';
COMMENT ON COLUMN users.user_type IS '用户类型：USER-普通用户, ADMIN-管理员';
COMMENT ON COLUMN users.status IS '账户状态：NORMAL-正常, BANNED-封禁, PENDING_ACTIVATION-待激活';
COMMENT ON COLUMN users.email_verified IS '邮箱是否已验证';
COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
COMMENT ON COLUMN users.profile_bio IS '用户简介';
COMMENT ON COLUMN users.deleted IS '逻辑删除标识：0-未删除, 1-已删除';

-- 2. 角色表 (roles)
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    persona_prompt TEXT NOT NULL,
    greeting_message TEXT,
    greeting_audio_url VARCHAR(500),
    avatar_url VARCHAR(500),
    voiceType VARCHAR(50),
    is_public SMALLINT NOT NULL DEFAULT 0,
    user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_roles_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_roles_is_public ON roles(is_public);
CREATE INDEX idx_roles_user_id ON roles(user_id);
CREATE INDEX idx_roles_deleted ON roles(deleted);

COMMENT ON TABLE roles IS 'AI角色表';
COMMENT ON COLUMN roles.id IS '主键ID（雪花ID）';
COMMENT ON COLUMN roles.name IS '角色名称';
COMMENT ON COLUMN roles.description IS '角色描述';
COMMENT ON COLUMN roles.persona_prompt IS '角色人设提示词';
COMMENT ON COLUMN roles.greeting_message IS '开场白文本';
COMMENT ON COLUMN roles.greeting_audio_url IS '开场白音频URL';
COMMENT ON COLUMN roles.avatar_url IS '头像URL';
COMMENT ON COLUMN roles.voiceType IS 'TTS声音类型';
COMMENT ON COLUMN roles.is_public IS '是否公共角色：0-私有, 1-公共';
COMMENT ON COLUMN roles.user_id IS '创建者用户ID（私人角色）';

-- 3. 对话表 (conversations)
CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL DEFAULT '新对话',
    user_id BIGINT NOT NULL,
    role_id BIGINT,
    model_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    system_prompt TEXT,
    config_params TEXT,
    last_active_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_conversations_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_conversations_role_id ON conversations(role_id);
CREATE INDEX idx_conversations_status ON conversations(status);
CREATE INDEX idx_conversations_last_active ON conversations(last_active_at DESC);
CREATE INDEX idx_conversations_deleted ON conversations(deleted);

COMMENT ON TABLE conversations IS '对话会话表';
COMMENT ON COLUMN conversations.id IS '主键ID（雪花ID）';
COMMENT ON COLUMN conversations.title IS '对话标题';
COMMENT ON COLUMN conversations.user_id IS '用户ID';
COMMENT ON COLUMN conversations.role_id IS '角色ID（可为空）';
COMMENT ON COLUMN conversations.model_name IS 'AI模型名称（provider:model格式）';
COMMENT ON COLUMN conversations.status IS '对话状态：ACTIVE-活跃, ARCHIVED-归档';
COMMENT ON COLUMN conversations.system_prompt IS '系统提示词';
COMMENT ON COLUMN conversations.config_params IS '对话配置参数（JSON格式）';
COMMENT ON COLUMN conversations.last_active_at IS '最后活跃时间';

-- 4. 对话消息表 (conversation_messages)
CREATE TABLE IF NOT EXISTS conversation_messages (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    audio_url VARCHAR(500),
    sequence INT NOT NULL,
    token_count INT,
    status VARCHAR(20) NOT NULL DEFAULT 'sent',
    error_message TEXT,
    metadata TEXT,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_conversation_id ON conversation_messages(conversation_id);
CREATE INDEX idx_messages_conversation_sequence ON conversation_messages(conversation_id, sequence);
CREATE INDEX idx_messages_role ON conversation_messages(role);
CREATE INDEX idx_messages_sent_at ON conversation_messages(sent_at DESC);
CREATE INDEX idx_messages_deleted ON conversation_messages(deleted);

COMMENT ON TABLE conversation_messages IS '对话消息表';
COMMENT ON COLUMN conversation_messages.id IS '主键ID（雪花ID）';
COMMENT ON COLUMN conversation_messages.conversation_id IS '对话ID';
COMMENT ON COLUMN conversation_messages.role IS '消息角色：USER-用户, ASSISTANT-AI助手, SYSTEM-系统';
COMMENT ON COLUMN conversation_messages.content IS '消息内容';
COMMENT ON COLUMN conversation_messages.audio_url IS 'AI回复语音地址';
COMMENT ON COLUMN conversation_messages.sequence IS '消息序号（在对话中的顺序）';
COMMENT ON COLUMN conversation_messages.token_count IS '令牌数量';
COMMENT ON COLUMN conversation_messages.status IS '消息状态：sending-发送中, sent-已发送, failed-失败';
COMMENT ON COLUMN conversation_messages.error_message IS '错误信息（如果发送失败）';
COMMENT ON COLUMN conversation_messages.metadata IS '消息元数据（JSON格式）';
COMMENT ON COLUMN conversation_messages.sent_at IS '消息发送时间';

-- 5. AI模型配置表 (ai_models)
CREATE TABLE IF NOT EXISTS ai_models (
    id BIGINT PRIMARY KEY,
    provider_code VARCHAR(50) NOT NULL,
    model_code VARCHAR(100) NOT NULL,
    model_name VARCHAR(200) NOT NULL,
    description TEXT,
    model_class VARCHAR(200),
    default_base_url VARCHAR(500),
    default_temperature DECIMAL(3,2) DEFAULT 0.7,
    default_max_tokens INT DEFAULT 2000,
    default_timeout_seconds INT DEFAULT 60,
    context_window INT DEFAULT 4096,
    input_token_price DECIMAL(10,6),
    output_token_price DECIMAL(10,6),
    config_json TEXT,
    status SMALLINT DEFAULT 1,
    priority INT DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_ai_models_provider_model UNIQUE (provider_code, model_code)
);

CREATE INDEX idx_ai_models_status ON ai_models(status);
CREATE INDEX idx_ai_models_priority ON ai_models(priority);
CREATE INDEX idx_ai_models_deleted ON ai_models(deleted);

COMMENT ON TABLE ai_models IS 'AI模型配置表';
COMMENT ON COLUMN ai_models.id IS '主键ID（雪花ID）';
COMMENT ON COLUMN ai_models.provider_code IS '厂商代码：openai/claude/grok/deepseek等';
COMMENT ON COLUMN ai_models.model_code IS '模型代码：gpt-4o-mini/claude-3-opus等';
COMMENT ON COLUMN ai_models.model_name IS '模型显示名称';
COMMENT ON COLUMN ai_models.description IS '模型描述';
COMMENT ON COLUMN ai_models.model_class IS 'LangChain4j模型类名';
COMMENT ON COLUMN ai_models.default_base_url IS '默认API端点';
COMMENT ON COLUMN ai_models.default_temperature IS '默认温度参数';
COMMENT ON COLUMN ai_models.default_max_tokens IS '默认最大tokens';
COMMENT ON COLUMN ai_models.default_timeout_seconds IS '默认超时时间（秒）';
COMMENT ON COLUMN ai_models.context_window IS '上下文窗口大小';
COMMENT ON COLUMN ai_models.input_token_price IS '输入token单价（元/千tokens）';
COMMENT ON COLUMN ai_models.output_token_price IS '输出token单价（元/千tokens）';
COMMENT ON COLUMN ai_models.config_json IS '额外配置JSON';
COMMENT ON COLUMN ai_models.status IS '状态：0-禁用, 1-启用';
COMMENT ON COLUMN ai_models.priority IS '优先级（越小越优先）';

-- 6. AI API密钥池表 (ai_api_keys)
CREATE TABLE IF NOT EXISTS ai_api_keys (
    id BIGINT PRIMARY KEY,
    provider_code VARCHAR(50) NOT NULL,
    model_code VARCHAR(100) NOT NULL,
    api_key VARCHAR(500) NOT NULL,
    api_secret VARCHAR(500),
    base_url VARCHAR(500),
    proxy_url VARCHAR(500),
    weight INT DEFAULT 1,
    rate_limit INT,
    concurrent_limit INT,
    status SMALLINT DEFAULT 1,
    fail_count INT DEFAULT 0,
    last_fail_time TIMESTAMP,
    last_success_time TIMESTAMP,
    health_check_time TIMESTAMP,
    total_requests BIGINT DEFAULT 0,
    total_tokens_used BIGINT DEFAULT 0,
    total_cost DECIMAL(15,6) DEFAULT 0,
    last_used_at TIMESTAMP,
    monthly_requests INT DEFAULT 0,
    monthly_tokens_used BIGINT DEFAULT 0,
    monthly_cost DECIMAL(12,6) DEFAULT 0,
    monthly_reset_date DATE,
    daily_quota_limit BIGINT,
    monthly_quota_limit BIGINT,
    daily_tokens_used BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_api_keys_provider_model ON ai_api_keys(provider_code, model_code);
CREATE INDEX idx_api_keys_status ON ai_api_keys(status);
CREATE INDEX idx_api_keys_last_used ON ai_api_keys(last_used_at);
CREATE INDEX idx_api_keys_deleted ON ai_api_keys(deleted);

COMMENT ON TABLE ai_api_keys IS 'AI API密钥池表';
COMMENT ON COLUMN ai_api_keys.id IS '主键ID（雪花ID）';
COMMENT ON COLUMN ai_api_keys.provider_code IS '厂商代码';
COMMENT ON COLUMN ai_api_keys.model_code IS '模型代码';
COMMENT ON COLUMN ai_api_keys.api_key IS 'API密钥（加密存储）';
COMMENT ON COLUMN ai_api_keys.api_secret IS 'API密钥对应的secret';
COMMENT ON COLUMN ai_api_keys.base_url IS '自定义端点URL';
COMMENT ON COLUMN ai_api_keys.proxy_url IS '代理URL';
COMMENT ON COLUMN ai_api_keys.weight IS '权重（用于加权轮询）';
COMMENT ON COLUMN ai_api_keys.rate_limit IS '每分钟请求数限制';
COMMENT ON COLUMN ai_api_keys.concurrent_limit IS '并发请求数限制';
COMMENT ON COLUMN ai_api_keys.status IS '状态：0-异常, 1-正常, 2-禁用';
COMMENT ON COLUMN ai_api_keys.fail_count IS '连续失败次数';
COMMENT ON COLUMN ai_api_keys.last_fail_time IS '最后失败时间';
COMMENT ON COLUMN ai_api_keys.last_success_time IS '最后成功时间';
COMMENT ON COLUMN ai_api_keys.health_check_time IS '最后健康检查时间';
COMMENT ON COLUMN ai_api_keys.total_requests IS '总请求数';
COMMENT ON COLUMN ai_api_keys.total_tokens_used IS '总token使用量';
COMMENT ON COLUMN ai_api_keys.total_cost IS '总费用（元）';
COMMENT ON COLUMN ai_api_keys.last_used_at IS '最后使用时间';
COMMENT ON COLUMN ai_api_keys.monthly_requests IS '当月请求数';
COMMENT ON COLUMN ai_api_keys.monthly_tokens_used IS '当月token使用量';
COMMENT ON COLUMN ai_api_keys.monthly_cost IS '当月费用（元）';
COMMENT ON COLUMN ai_api_keys.monthly_reset_date IS '月度重置日期';
COMMENT ON COLUMN ai_api_keys.daily_quota_limit IS '每日配额限制（tokens）';
COMMENT ON COLUMN ai_api_keys.monthly_quota_limit IS '月度配额限制（tokens）';
COMMENT ON COLUMN ai_api_keys.daily_tokens_used IS '今日已用tokens';

-- 7. AI API调用日志表 (ai_api_call_logs)
CREATE TABLE IF NOT EXISTS ai_api_call_logs (
    id BIGINT PRIMARY KEY,
    api_key_id BIGINT NOT NULL,
    provider_code VARCHAR(50) NOT NULL,
    model_code VARCHAR(100) NOT NULL,
    user_id BIGINT,
    conversation_id BIGINT,
    request_id VARCHAR(100),
    request_time TIMESTAMP NOT NULL,
    request_params TEXT,
    response_time TIMESTAMP,
    response_time_ms INT,
    status SMALLINT NOT NULL,
    error_message TEXT,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    input_cost DECIMAL(10,6),
    output_cost DECIMAL(10,6),
    total_cost DECIMAL(10,6),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_call_logs_api_key ON ai_api_call_logs(api_key_id);
CREATE INDEX idx_call_logs_provider_model ON ai_api_call_logs(provider_code, model_code);
CREATE INDEX idx_call_logs_user ON ai_api_call_logs(user_id);
CREATE INDEX idx_call_logs_conversation ON ai_api_call_logs(conversation_id);
CREATE INDEX idx_call_logs_request_time ON ai_api_call_logs(request_time DESC);
CREATE INDEX idx_call_logs_status ON ai_api_call_logs(status);
CREATE INDEX idx_call_logs_deleted ON ai_api_call_logs(deleted);

COMMENT ON TABLE ai_api_call_logs IS 'AI API调用日志表';
COMMENT ON COLUMN ai_api_call_logs.id IS '主键ID（雪花ID）';
COMMENT ON COLUMN ai_api_call_logs.api_key_id IS 'API密钥ID';
COMMENT ON COLUMN ai_api_call_logs.provider_code IS '厂商代码';
COMMENT ON COLUMN ai_api_call_logs.model_code IS '模型代码';
COMMENT ON COLUMN ai_api_call_logs.user_id IS '用户ID';
COMMENT ON COLUMN ai_api_call_logs.conversation_id IS '对话ID';
COMMENT ON COLUMN ai_api_call_logs.request_id IS '请求ID';
COMMENT ON COLUMN ai_api_call_logs.request_time IS '请求时间';
COMMENT ON COLUMN ai_api_call_logs.request_params IS '请求参数JSON';
COMMENT ON COLUMN ai_api_call_logs.response_time IS '响应时间';
COMMENT ON COLUMN ai_api_call_logs.response_time_ms IS '响应耗时（毫秒）';
COMMENT ON COLUMN ai_api_call_logs.status IS '状态：0-失败, 1-成功';
COMMENT ON COLUMN ai_api_call_logs.error_message IS '错误信息';
COMMENT ON COLUMN ai_api_call_logs.prompt_tokens IS '输入tokens';
COMMENT ON COLUMN ai_api_call_logs.completion_tokens IS '输出tokens';
COMMENT ON COLUMN ai_api_call_logs.total_tokens IS '总tokens';
COMMENT ON COLUMN ai_api_call_logs.input_cost IS '输入费用（元）';
COMMENT ON COLUMN ai_api_call_logs.output_cost IS '输出费用（元）';
COMMENT ON COLUMN ai_api_call_logs.total_cost IS '总费用（元）';

-- 8. 系统配置表 (system_config)
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(500),
    config_group VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    readonly BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    remark TEXT,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_system_config_group ON system_config(config_group);
CREATE INDEX idx_system_config_enabled ON system_config(enabled);
CREATE INDEX idx_system_config_deleted ON system_config(deleted);

COMMENT ON TABLE system_config IS '系统配置表';
COMMENT ON COLUMN system_config.id IS '主键ID（雪花ID）';
COMMENT ON COLUMN system_config.config_key IS '配置键';
COMMENT ON COLUMN system_config.config_value IS '配置值';
COMMENT ON COLUMN system_config.description IS '配置描述';
COMMENT ON COLUMN system_config.config_group IS '配置分组';
COMMENT ON COLUMN system_config.enabled IS '是否启用';
COMMENT ON COLUMN system_config.readonly IS '是否只读';
COMMENT ON COLUMN system_config.sort_order IS '排序';
COMMENT ON COLUMN system_config.remark IS '备注';
COMMENT ON COLUMN system_config.version IS '版本号（乐观锁）';

-- 9. 创建updated_at自动更新触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为所有表创建updated_at触发器
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_conversation_messages_updated_at BEFORE UPDATE ON conversation_messages
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ai_models_updated_at BEFORE UPDATE ON ai_models
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ai_api_keys_updated_at BEFORE UPDATE ON ai_api_keys
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ai_api_call_logs_updated_at BEFORE UPDATE ON ai_api_call_logs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_system_config_updated_at BEFORE UPDATE ON system_config
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
