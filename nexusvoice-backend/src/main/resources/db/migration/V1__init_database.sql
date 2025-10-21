-- =====================================================
-- NexusVoice PostgreSQL数据库初始化脚本
-- 版本: 1.0
-- 基于实际数据库表结构生成
-- =====================================================

-- 设置schema
SET search_path TO nexusvoice;

-- 1. 用户表 (users)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY,
    email CHARACTER VARYING(100) NOT NULL,
    password_hash CHARACTER VARYING(255),
    nickname CHARACTER VARYING(50) NOT NULL,
    avatar_url CHARACTER VARYING(255),
    phone CHARACTER VARYING(20),
    user_type CHARACTER VARYING(20) NOT NULL DEFAULT 'USER',
    status SMALLINT NOT NULL DEFAULT 1,
    email_verified SMALLINT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP WITH TIME ZONE,
    profile_bio TEXT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted INTEGER NOT NULL DEFAULT 0,
    oauth_provider CHARACTER VARYING(50),
    oauth_id CHARACTER VARYING(255),
    oauth_username CHARACTER VARYING(100),
    oauth_avatar_url CHARACTER VARYING(500),
    oauth_access_token TEXT,
    oauth_refresh_token TEXT,
    oauth_token_expires_at TIMESTAMP WITHOUT TIME ZONE,
    oauth_bind_time TIMESTAMP WITHOUT TIME ZONE,
    oauth_raw_data JSONB,
    CONSTRAINT chk_auth_method CHECK (password_hash IS NOT NULL OR (oauth_provider IS NOT NULL AND oauth_id IS NOT NULL))
);

-- 用户表索引
CREATE INDEX IF NOT EXISTS idx_users_oauth_provider ON users(oauth_provider) WHERE oauth_provider IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_oauth_unique ON users(oauth_provider, oauth_id) WHERE oauth_provider IS NOT NULL AND oauth_id IS NOT NULL;

-- 用户表注释
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户唯一ID';
COMMENT ON COLUMN users.email IS '用户登录邮箱';
COMMENT ON COLUMN users.password_hash IS '加密后的用户密码';
COMMENT ON COLUMN users.nickname IS '用户昵称';
COMMENT ON COLUMN users.avatar_url IS '用户头像图片的URL';
COMMENT ON COLUMN users.phone IS '手机号码';
COMMENT ON COLUMN users.user_type IS '用户类型 (ADMIN-管理员, USER-普通用户)';
COMMENT ON COLUMN users.status IS '账户状态 (1-正常, 2-封禁, 3-待激活)';
COMMENT ON COLUMN users.email_verified IS '邮箱是否已验证 (0-未验证, 1-已验证)';
COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
COMMENT ON COLUMN users.profile_bio IS '用户简介';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';
COMMENT ON COLUMN users.deleted IS '逻辑删除标识 (0-未删除, 1-已删除)';
COMMENT ON COLUMN users.oauth_provider IS 'OAuth提供商标识：github/google/wechat等';
COMMENT ON COLUMN users.oauth_id IS 'OAuth用户在第三方平台的唯一标识';
COMMENT ON COLUMN users.oauth_username IS 'OAuth用户在第三方平台的用户名';
COMMENT ON COLUMN users.oauth_avatar_url IS 'OAuth用户在第三方平台的头像URL';
COMMENT ON COLUMN users.oauth_access_token IS 'OAuth访问令牌（加密存储）';
COMMENT ON COLUMN users.oauth_refresh_token IS 'OAuth刷新令牌（加密存储）';
COMMENT ON COLUMN users.oauth_token_expires_at IS 'OAuth令牌过期时间';
COMMENT ON COLUMN users.oauth_bind_time IS 'OAuth账号绑定时间';
COMMENT ON COLUMN users.oauth_raw_data IS 'OAuth原始用户数据，JSON格式存储';

-- 2. 角色表 (roles)
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY,
    name CHARACTER VARYING(50) NOT NULL,
    description TEXT,
    persona_prompt TEXT NOT NULL,
    greeting_message CHARACTER VARYING(255),
    greeting_audio_url CHARACTER VARYING(255),
    avatar_url CHARACTER VARYING(255),
    voicetype CHARACTER VARYING(50) NOT NULL,
    is_public SMALLINT NOT NULL DEFAULT 0,
    user_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted SMALLINT NOT NULL DEFAULT 0
);

-- 角色表注释
COMMENT ON TABLE roles IS 'AI角色表';
COMMENT ON COLUMN roles.id IS '角色的唯一标识符，主键';
COMMENT ON COLUMN roles.name IS '角色的名字，例如"哈利波特"';
COMMENT ON COLUMN roles.description IS '对角色的简短描述，用于在列表中向用户展示';
COMMENT ON COLUMN roles.persona_prompt IS '角色的核心人设定义，这是指导LLM进行角色扮演的最关键信息';
COMMENT ON COLUMN roles.greeting_message IS '开场白的文本内容，用于界面显示或重新生成语音';
COMMENT ON COLUMN roles.greeting_audio_url IS '开场白预生成语音文件在对象存储服务中的URL路径';
COMMENT ON COLUMN roles.avatar_url IS '角色头像图片的URL';
COMMENT ON COLUMN roles.voicetype IS '关联到TTS（文本转语音）服务的特定声音类型或声音标识符';
COMMENT ON COLUMN roles.is_public IS '标记角色是否为公共角色。1 为管理员创建的公共角色，0 为用户创建的私人角色';
COMMENT ON COLUMN roles.user_id IS '关联的用户ID，外键，指向users表。对于公共角色，此字段为NULL';
COMMENT ON COLUMN roles.created_at IS '记录的创建时间';
COMMENT ON COLUMN roles.updated_at IS '记录的最后更新时间';
COMMENT ON COLUMN roles.deleted IS '0：未删除；1：已删除';

-- 3. 对话表 (conversations)
CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT PRIMARY KEY,
    title CHARACTER VARYING(255) NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT,
    model_name CHARACTER VARYING(100) NOT NULL,
    status CHARACTER VARYING(20) NOT NULL DEFAULT 'ACTIVE',
    system_prompt TEXT,
    config_params JSONB,
    last_active_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted SMALLINT NOT NULL DEFAULT 0
);

-- 对话表注释
COMMENT ON TABLE conversations IS '对话会话表';
COMMENT ON COLUMN conversations.id IS '主键ID';
COMMENT ON COLUMN conversations.title IS '对话标题';
COMMENT ON COLUMN conversations.user_id IS '用户ID';
COMMENT ON COLUMN conversations.role_id IS '角色ID';
COMMENT ON COLUMN conversations.model_name IS 'AI模型名称';
COMMENT ON COLUMN conversations.status IS '对话状态: ACTIVE-活跃, ARCHIVED-归档, DELETED-删除';
COMMENT ON COLUMN conversations.system_prompt IS '系统提示词';
COMMENT ON COLUMN conversations.config_params IS '对话配置参数(JSON格式，包含temperature、maxTokens等)';
COMMENT ON COLUMN conversations.last_active_at IS '最后活跃时间';
COMMENT ON COLUMN conversations.created_at IS '创建时间';
COMMENT ON COLUMN conversations.updated_at IS '更新时间';
COMMENT ON COLUMN conversations.deleted IS '删除标识: 0-未删除, 1-已删除';

-- 4. 对话消息表 (conversation_messages)
CREATE TABLE IF NOT EXISTS conversation_messages (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role CHARACTER VARYING(20) NOT NULL,
    content TEXT NOT NULL,
    audio_url CHARACTER VARYING(255),
    sequence INTEGER NOT NULL,
    token_count INTEGER,
    status CHARACTER VARYING(20) NOT NULL DEFAULT 'sent',
    error_message TEXT,
    metadata JSONB,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted SMALLINT NOT NULL DEFAULT 0
);

-- 对话消息表注释
COMMENT ON TABLE conversation_messages IS '对话消息表';
COMMENT ON COLUMN conversation_messages.id IS '主键ID';
COMMENT ON COLUMN conversation_messages.conversation_id IS '对话ID';
COMMENT ON COLUMN conversation_messages.role IS '消息角色: SYSTEM-系统, USER-用户, ASSISTANT-助手, FUNCTION-功能, TOOL-工具';
COMMENT ON COLUMN conversation_messages.content IS '消息内容';
COMMENT ON COLUMN conversation_messages.audio_url IS 'AI回复语音地址';
COMMENT ON COLUMN conversation_messages.sequence IS '消息序号(在对话中的顺序)';
COMMENT ON COLUMN conversation_messages.token_count IS '令牌数量';
COMMENT ON COLUMN conversation_messages.status IS '消息状态: sending-发送中, sent-已发送, failed-失败';
COMMENT ON COLUMN conversation_messages.error_message IS '错误信息(发送失败时)';
COMMENT ON COLUMN conversation_messages.metadata IS '消息元数据(JSON格式，包含模型响应的其他信息)';
COMMENT ON COLUMN conversation_messages.sent_at IS '消息发送时间';
COMMENT ON COLUMN conversation_messages.created_at IS '创建时间';
COMMENT ON COLUMN conversation_messages.updated_at IS '更新时间';
COMMENT ON COLUMN conversation_messages.deleted IS '删除标识: 0-未删除, 1-已删除';

-- 5. AI模型表 (ai_models)
CREATE TABLE IF NOT EXISTS ai_models (
    id BIGINT PRIMARY KEY,
    provider_code CHARACTER VARYING(50) NOT NULL,
    model_code CHARACTER VARYING(100) NOT NULL,
    model_name CHARACTER VARYING(200) NOT NULL,
    description TEXT,
    model_class CHARACTER VARYING(200),
    default_base_url CHARACTER VARYING(500),
    default_temperature NUMERIC(3,2) DEFAULT 0.70,
    default_max_tokens INTEGER DEFAULT 2000,
    default_timeout_seconds INTEGER DEFAULT 60,
    context_window INTEGER DEFAULT 4096,
    input_token_price NUMERIC(10,6),
    output_token_price NUMERIC(10,6),
    config_json TEXT,
    status SMALLINT DEFAULT 1,
    priority INTEGER DEFAULT 100,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted SMALLINT DEFAULT 0
);

-- AI模型表注释
COMMENT ON TABLE ai_models IS 'AI模型配置表';
COMMENT ON COLUMN ai_models.id IS '雪花ID';
COMMENT ON COLUMN ai_models.provider_code IS '厂商代码：openai/claude/qwen等';
COMMENT ON COLUMN ai_models.model_code IS '模型代码：gpt-4o-mini/claude-3-opus等';
COMMENT ON COLUMN ai_models.model_name IS '模型显示名称';
COMMENT ON COLUMN ai_models.description IS '模型描述';
COMMENT ON COLUMN ai_models.model_class IS 'LangChain4j模型类：OpenAiChatModel/ClaudeChatModel等';
COMMENT ON COLUMN ai_models.default_base_url IS '默认API端点';
COMMENT ON COLUMN ai_models.default_temperature IS '默认温度';
COMMENT ON COLUMN ai_models.default_max_tokens IS '默认最大tokens';
COMMENT ON COLUMN ai_models.default_timeout_seconds IS '默认超时时间';
COMMENT ON COLUMN ai_models.context_window IS '上下文窗口大小';
COMMENT ON COLUMN ai_models.input_token_price IS '输入token单价（元/千tokens）';
COMMENT ON COLUMN ai_models.output_token_price IS '输出token单价（元/千tokens）';
COMMENT ON COLUMN ai_models.config_json IS '额外配置JSON';
COMMENT ON COLUMN ai_models.status IS '状态：0-禁用 1-启用';
COMMENT ON COLUMN ai_models.priority IS '优先级（越小越优先）';
COMMENT ON COLUMN ai_models.created_at IS '创建时间';
COMMENT ON COLUMN ai_models.updated_at IS '更新时间';
COMMENT ON COLUMN ai_models.deleted IS '逻辑删除标识';

-- 6. AI API密钥表 (ai_api_keys)
CREATE TABLE IF NOT EXISTS ai_api_keys (
    id BIGINT PRIMARY KEY,
    provider_code CHARACTER VARYING(50) NOT NULL,
    model_code CHARACTER VARYING(100) NOT NULL,
    api_key CHARACTER VARYING(500) NOT NULL,
    api_secret CHARACTER VARYING(500),
    base_url CHARACTER VARYING(500),
    proxy_url CHARACTER VARYING(500),
    weight INTEGER DEFAULT 1,
    rate_limit INTEGER,
    concurrent_limit INTEGER,
    status SMALLINT DEFAULT 1,
    fail_count INTEGER DEFAULT 0,
    last_fail_time TIMESTAMP WITH TIME ZONE,
    last_success_time TIMESTAMP WITH TIME ZONE,
    health_check_time TIMESTAMP WITH TIME ZONE,
    total_requests BIGINT DEFAULT 0,
    total_tokens_used BIGINT DEFAULT 0,
    total_cost NUMERIC(15,6) DEFAULT 0.000000,
    last_used_at TIMESTAMP WITH TIME ZONE,
    monthly_requests INTEGER DEFAULT 0,
    monthly_tokens_used BIGINT DEFAULT 0,
    monthly_cost NUMERIC(12,6) DEFAULT 0.000000,
    monthly_reset_date DATE,
    daily_quota_limit BIGINT,
    monthly_quota_limit BIGINT,
    daily_tokens_used BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted SMALLINT DEFAULT 0
);

-- AI API密钥表注释
COMMENT ON TABLE ai_api_keys IS 'AI API密钥配置表';
COMMENT ON COLUMN ai_api_keys.id IS '雪花ID';
COMMENT ON COLUMN ai_api_keys.provider_code IS '厂商代码';
COMMENT ON COLUMN ai_api_keys.model_code IS '模型代码';
COMMENT ON COLUMN ai_api_keys.api_key IS 'API密钥（加密存储）';
COMMENT ON COLUMN ai_api_keys.api_secret IS 'API密钥对应的secret（某些厂商需要）';
COMMENT ON COLUMN ai_api_keys.base_url IS '自定义端点URL';
COMMENT ON COLUMN ai_api_keys.proxy_url IS '代理URL';
COMMENT ON COLUMN ai_api_keys.weight IS '权重（用于加权轮询）';
COMMENT ON COLUMN ai_api_keys.rate_limit IS '每分钟请求数限制';
COMMENT ON COLUMN ai_api_keys.concurrent_limit IS '并发请求数限制';
COMMENT ON COLUMN ai_api_keys.status IS '状态：0-异常 1-正常 2-禁用';
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
COMMENT ON COLUMN ai_api_keys.created_at IS '创建时间';
COMMENT ON COLUMN ai_api_keys.updated_at IS '更新时间';
COMMENT ON COLUMN ai_api_keys.deleted IS '逻辑删除标识';

-- 7. AI API调用日志表 (ai_api_call_logs)
CREATE TABLE IF NOT EXISTS ai_api_call_logs (
    id BIGINT PRIMARY KEY,
    api_key_id BIGINT NOT NULL,
    provider_code CHARACTER VARYING(50) NOT NULL,
    model_code CHARACTER VARYING(100) NOT NULL,
    user_id BIGINT,
    conversation_id BIGINT,
    request_id CHARACTER VARYING(100),
    request_time TIMESTAMP WITH TIME ZONE,
    request_params TEXT,
    response_time TIMESTAMP WITH TIME ZONE,
    response_time_ms INTEGER,
    status SMALLINT,
    error_message TEXT,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    input_cost NUMERIC(10,6),
    output_cost NUMERIC(10,6),
    total_cost NUMERIC(10,6),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- AI API调用日志表注释
COMMENT ON TABLE ai_api_call_logs IS 'AI API调用日志表';
COMMENT ON COLUMN ai_api_call_logs.id IS '雪花ID';
COMMENT ON COLUMN ai_api_call_logs.api_key_id IS 'API密钥ID';
COMMENT ON COLUMN ai_api_call_logs.provider_code IS '厂商代码';
COMMENT ON COLUMN ai_api_call_logs.model_code IS '模型代码';
COMMENT ON COLUMN ai_api_call_logs.user_id IS '用户ID';
COMMENT ON COLUMN ai_api_call_logs.conversation_id IS '对话ID';
COMMENT ON COLUMN ai_api_call_logs.request_id IS '请求ID';
COMMENT ON COLUMN ai_api_call_logs.request_time IS '请求时间';
COMMENT ON COLUMN ai_api_call_logs.request_params IS '请求参数';
COMMENT ON COLUMN ai_api_call_logs.response_time IS '响应时间';
COMMENT ON COLUMN ai_api_call_logs.response_time_ms IS '响应耗时（毫秒）';
COMMENT ON COLUMN ai_api_call_logs.status IS '状态：0-失败 1-成功';
COMMENT ON COLUMN ai_api_call_logs.error_message IS '错误信息';
COMMENT ON COLUMN ai_api_call_logs.prompt_tokens IS '输入tokens';
COMMENT ON COLUMN ai_api_call_logs.completion_tokens IS '输出tokens';
COMMENT ON COLUMN ai_api_call_logs.total_tokens IS '总tokens';
COMMENT ON COLUMN ai_api_call_logs.input_cost IS '输入费用（元）';
COMMENT ON COLUMN ai_api_call_logs.output_cost IS '输出费用（元）';
COMMENT ON COLUMN ai_api_call_logs.total_cost IS '总费用（元）';
COMMENT ON COLUMN ai_api_call_logs.created_at IS '创建时间';
COMMENT ON COLUMN ai_api_call_logs.updated_at IS '修改时间';

-- 8. 系统配置表 (system_config)
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT PRIMARY KEY,
    config_key CHARACTER VARYING(100) NOT NULL,
    config_value CHARACTER VARYING(1000) NOT NULL,
    description CHARACTER VARYING(200) NOT NULL,
    config_group CHARACTER VARYING(50),
    enabled SMALLINT DEFAULT 1,
    readonly SMALLINT DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    remark CHARACTER VARYING(500),
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted INTEGER DEFAULT 0
);

-- 系统配置表注释
COMMENT ON TABLE system_config IS '系统配置表';
COMMENT ON COLUMN system_config.version IS '版本号（乐观锁）';
