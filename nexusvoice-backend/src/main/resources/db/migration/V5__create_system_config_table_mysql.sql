-- 创建系统配置表（MySQL版本）
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID，雪花ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value VARCHAR(1000) NOT NULL COMMENT '配置值',
    description VARCHAR(200) NOT NULL COMMENT '配置描述',
    config_group VARCHAR(50) DEFAULT NULL COMMENT '配置分组',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用：1-启用，0-禁用',
    readonly TINYINT DEFAULT 0 COMMENT '是否只读：1-只读，0-可修改',
    sort_order INT DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标识：0-未删除，非0-已删除',
    
    UNIQUE KEY uk_config_key (config_key, deleted),
    INDEX idx_config_group (config_group),
    INDEX idx_enabled (enabled),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 插入默认配置数据
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, remark) VALUES
(1, 'system.name', 'NexusVoice', '系统名称', 'system', 1, 0, 1, '系统基础配置'),
(2, 'system.version', '1.0.0', '系统版本', 'system', 1, 1, 2, '系统版本信息，只读'),
(3, 'system.description', 'AI智能对话系统', '系统描述', 'system', 1, 0, 3, '系统描述信息'),

-- AI模型相关配置
(10, 'ai.model.default', 'openai:gpt-oss-20b', '默认AI模型', 'ai', 1, 0, 10, '系统默认使用的AI模型，格式：provider:model'),
(11, 'ai.model.default.provider', 'openai', '默认模型厂商', 'ai', 1, 0, 11, '默认模型的厂商代码'),
(12, 'ai.model.default.code', 'gpt-oss-20b', '默认模型代码', 'ai', 1, 0, 12, '默认模型的代码'),
(13, 'ai.temperature.default', '0.7', 'AI默认温度参数', 'ai', 1, 0, 13, '控制AI回答的随机性，范围0-2'),
(14, 'ai.max_tokens.default', '2000', 'AI默认最大令牌数', 'ai', 1, 0, 14, '单次对话默认最大令牌数'),
(15, 'ai.system_prompt.default', '你是一个有用的AI助手', '默认系统提示词', 'ai', 1, 0, 15, '创建新对话时的默认系统提示词'),

-- 对话相关配置
(20, 'conversation.title.default', '新对话', '默认对话标题', 'conversation', 1, 0, 20, '创建新对话时的默认标题'),
(21, 'conversation.max_history', '20', '对话历史最大条数', 'conversation', 1, 0, 21, '保留的对话历史记录数量'),
(22, 'conversation.timeout', '300', '对话超时时间（秒）', 'conversation', 1, 0, 22, '对话会话超时时间'),
(23, 'conversation.max_messages', '100', '单个对话最大消息数', 'conversation', 1, 0, 23, '单个对话允许的最大消息条数'),
(24, 'conversation.max_tokens', '50000', '单个对话最大令牌数', 'conversation', 1, 0, 24, '单个对话累计最大令牌数'),

-- 文件上传配置
(30, 'file.upload.max_size', '10485760', '文件上传最大大小（字节）', 'file', 1, 0, 30, '10MB文件上传限制'),
(31, 'file.upload.allowed_types', 'jpg,jpeg,png,gif,pdf,txt,doc,docx', '允许上传的文件类型', 'file', 1, 0, 31, '文件类型白名单'),

-- 安全相关配置
(40, 'security.jwt.expire_time', '86400', 'JWT过期时间（秒）', 'security', 1, 0, 40, '24小时JWT有效期'),
(41, 'security.password.min_length', '6', '密码最小长度', 'security', 1, 0, 41, '用户密码最小长度要求'),

-- 缓存配置
(50, 'cache.redis.expire_time', '3600', 'Redis缓存过期时间（秒）', 'cache', 1, 0, 50, '1小时缓存过期时间'),
(51, 'cache.system_config.expire_time', '300', '系统配置缓存过期时间（秒）', 'cache', 1, 0, 51, '5分钟系统配置缓存'),

-- 搜索功能配置
(60, 'search.enabled', 'true', '是否启用搜索功能', 'search', 1, 0, 60, '控制联网搜索功能开关'),
(61, 'search.provider', 'duckduckgo', '搜索提供商', 'search', 1, 0, 61, '默认搜索引擎提供商'),

-- TTS语音合成配置
(70, 'tts.voice.default', 'qiniu_zh_female_wwxkjx', '默认语音类型', 'tts', 1, 0, 70, '默认TTS语音类型'),
(71, 'tts.speed.default', '1.0', '默认语速', 'tts', 1, 0, 71, '默认语速比例'),
(72, 'tts.encoding.default', 'mp3', '默认音频编码', 'tts', 1, 0, 72, '默认音频编码格式');
