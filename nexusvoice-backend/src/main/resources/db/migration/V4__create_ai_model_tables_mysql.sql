-- AI模型配置表
CREATE TABLE IF NOT EXISTS ai_models (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '雪花ID',
    provider_code VARCHAR(50) NOT NULL COMMENT '厂商代码：openai/claude/qwen等',
    model_code VARCHAR(100) NOT NULL COMMENT '模型代码：gpt-4o-mini/claude-3-opus等',
    model_name VARCHAR(200) NOT NULL COMMENT '模型显示名称',
    description TEXT COMMENT '模型描述',
    
    -- LangChain4j适配字段
    model_class VARCHAR(200) COMMENT 'LangChain4j模型类',
    default_base_url VARCHAR(500) COMMENT '默认API端点',
    
    -- 模型参数配置
    default_temperature DECIMAL(3,2) DEFAULT 0.7 COMMENT '默认温度',
    default_max_tokens INT DEFAULT 2000 COMMENT '默认最大tokens',
    default_timeout_seconds INT DEFAULT 60 COMMENT '默认超时时间',
    context_window INT DEFAULT 4096 COMMENT '上下文窗口大小',
    
    -- 费用相关
    input_token_price DECIMAL(10,6) COMMENT '输入token单价（元/千tokens）',
    output_token_price DECIMAL(10,6) COMMENT '输出token单价（元/千tokens）',
    
    -- 配置JSON（扩展配置）
    config_json TEXT COMMENT '额外配置JSON',
    
    -- 管理字段
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    priority INT DEFAULT 100 COMMENT '优先级（越小越优先）',
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    UNIQUE KEY uk_provider_model (provider_code, model_code),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- API密钥池表
CREATE TABLE IF NOT EXISTS ai_api_keys (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '雪花ID',
    provider_code VARCHAR(50) NOT NULL COMMENT '厂商代码',
    model_code VARCHAR(100) NOT NULL COMMENT '模型代码',
    
    -- API配置
    api_key VARCHAR(500) NOT NULL COMMENT 'API密钥（加密存储）',
    api_secret VARCHAR(500) COMMENT 'API密钥对应的secret',
    base_url VARCHAR(500) COMMENT '自定义端点URL',
    proxy_url VARCHAR(500) COMMENT '代理URL',
    
    -- 池化管理
    weight INT DEFAULT 1 COMMENT '权重（用于加权轮询）',
    rate_limit INT COMMENT '每分钟请求数限制',
    concurrent_limit INT COMMENT '并发请求数限制',
    
    -- 状态监控
    status TINYINT DEFAULT 1 COMMENT '状态：0-异常 1-正常 2-禁用',
    fail_count INT DEFAULT 0 COMMENT '连续失败次数',
    last_fail_time DATETIME COMMENT '最后失败时间',
    last_success_time DATETIME COMMENT '最后成功时间',
    health_check_time DATETIME COMMENT '最后健康检查时间',
    
    -- 使用统计
    total_requests BIGINT DEFAULT 0 COMMENT '总请求数',
    total_tokens_used BIGINT DEFAULT 0 COMMENT '总token使用量',
    total_cost DECIMAL(15,6) DEFAULT 0 COMMENT '总费用（元）',
    last_used_at DATETIME COMMENT '最后使用时间',
    
    -- 费用统计（按月）
    monthly_requests INT DEFAULT 0 COMMENT '当月请求数',
    monthly_tokens_used BIGINT DEFAULT 0 COMMENT '当月token使用量',
    monthly_cost DECIMAL(12,6) DEFAULT 0 COMMENT '当月费用（元）',
    monthly_reset_date DATE COMMENT '月度重置日期',
    
    -- 配额限制
    daily_quota_limit BIGINT COMMENT '每日配额限制（tokens）',
    monthly_quota_limit BIGINT COMMENT '月度配额限制（tokens）',
    daily_tokens_used BIGINT DEFAULT 0 COMMENT '今日已用tokens',
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    INDEX idx_provider_model (provider_code, model_code),
    INDEX idx_status (status),
    INDEX idx_last_used (last_used_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI API密钥池表';

-- API调用日志表
CREATE TABLE IF NOT EXISTS ai_api_call_logs (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '雪花ID',
    
    -- 关联信息
    api_key_id BIGINT NOT NULL COMMENT 'API密钥ID',
    provider_code VARCHAR(50) NOT NULL COMMENT '厂商代码',
    model_code VARCHAR(100) NOT NULL COMMENT '模型代码',
    user_id BIGINT COMMENT '用户ID',
    conversation_id BIGINT COMMENT '对话ID',
    
    -- 请求信息
    request_id VARCHAR(100) COMMENT '请求ID',
    request_time DATETIME NOT NULL COMMENT '请求时间',
    request_params TEXT COMMENT '请求参数',
    
    -- 响应信息
    response_time DATETIME COMMENT '响应时间',
    response_time_ms INT COMMENT '响应耗时（毫秒）',
    status TINYINT COMMENT '状态：0-失败 1-成功',
    error_message TEXT COMMENT '错误信息',
    
    -- Token统计
    prompt_tokens INT COMMENT '输入tokens',
    completion_tokens INT COMMENT '输出tokens',
    total_tokens INT COMMENT '总tokens',
    
    -- 费用计算
    input_cost DECIMAL(10,6) COMMENT '输入费用（元）',
    output_cost DECIMAL(10,6) COMMENT '输出费用（元）',
    total_cost DECIMAL(10,6) COMMENT '总费用（元）',
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_api_key (api_key_id),
    INDEX idx_user (user_id),
    INDEX idx_conversation (conversation_id),
    INDEX idx_request_time (request_time),
    INDEX idx_provider_model (provider_code, model_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI API调用日志表';

-- 插入默认模型配置数据
INSERT INTO ai_models (id, provider_code, model_code, model_name, description, 
    model_class, default_base_url, default_temperature, default_max_tokens, 
    default_timeout_seconds, context_window, input_token_price, output_token_price, 
    status, priority) 
VALUES 
    -- OpenAI模型
    (1, 'openai', 'gpt-4o-mini', 'GPT-4o Mini', 'OpenAI GPT-4o迷你版，性价比最高', 
     'OpenAiChatModel', 'https://api.openai.com/v1', 0.7, 2000, 
     60, 128000, 0.00015, 0.0006, 1, 10),
    
    (2, 'openai', 'gpt-4o', 'GPT-4o', 'OpenAI GPT-4o标准版，能力更强', 
     'OpenAiChatModel', 'https://api.openai.com/v1', 0.7, 4000, 
     90, 128000, 0.0025, 0.01, 1, 20),
    
    (3, 'openai', 'gpt-3.5-turbo', 'GPT-3.5 Turbo', 'OpenAI GPT-3.5 Turbo，经典模型', 
     'OpenAiChatModel', 'https://api.openai.com/v1', 0.7, 2000, 
     60, 16385, 0.0005, 0.0015, 1, 30),
    
    -- Claude模型
    (4, 'claude', 'claude-3-opus', 'Claude 3 Opus', 'Anthropic Claude 3最强版本', 
     'ClaudeChatModel', 'https://api.anthropic.com/v1', 0.7, 4000, 
     90, 200000, 0.015, 0.075, 0, 40),
    
    (5, 'claude', 'claude-3-sonnet', 'Claude 3 Sonnet', 'Anthropic Claude 3平衡版本', 
     'ClaudeChatModel', 'https://api.anthropic.com/v1', 0.7, 4000, 
     90, 200000, 0.003, 0.015, 0, 50),
    
    -- 通义千问模型
    (6, 'qwen', 'qwen-max', '通义千问Max', '阿里通义千问最强版', 
     'QwenChatModel', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 0.7, 4000, 
     90, 32000, 0.002, 0.006, 0, 60),
    
    (7, 'qwen', 'qwen-plus', '通义千问Plus', '阿里通义千问增强版', 
     'QwenChatModel', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 0.7, 3000, 
     60, 32000, 0.0008, 0.002, 0, 70),
    
    -- 文心一言模型
    (8, 'wenxin', 'ernie-4.0', '文心一言4.0', '百度文心一言4.0', 
     'WenxinChatModel', 'https://aip.baidubce.com', 0.7, 3000, 
     60, 8192, 0.012, 0.012, 0, 80),
    
    -- 智谱AI模型
    (9, 'zhipu', 'glm-4', 'GLM-4', '智谱GLM-4', 
     'ZhipuChatModel', 'https://open.bigmodel.cn/api/paas/v4', 0.7, 3000, 
     60, 128000, 0.001, 0.001, 0, 90);

-- 插入示例API密钥（需要实际配置）
-- INSERT INTO ai_api_keys (id, provider_code, model_code, api_key, status)
-- VALUES 
--     (1, 'openai', 'gpt-4o-mini', 'sk-xxx', 1),
--     (2, 'openai', 'gpt-4o', 'sk-xxx', 1);
