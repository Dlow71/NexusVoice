-- 创建系统配置表
CREATE TABLE system_config (
    id BIGINT NOT NULL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(1000) NOT NULL,
    description VARCHAR(200) NOT NULL,
    config_group VARCHAR(50) DEFAULT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    readonly BOOLEAN DEFAULT FALSE,
    sort_order INTEGER DEFAULT 0,
    remark VARCHAR(500) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 添加表注释
COMMENT ON TABLE system_config IS '系统配置表';
COMMENT ON COLUMN system_config.id IS '主键ID，雪花ID';
COMMENT ON COLUMN system_config.config_key IS '配置键';
COMMENT ON COLUMN system_config.config_value IS '配置值';
COMMENT ON COLUMN system_config.description IS '配置描述';
COMMENT ON COLUMN system_config.config_group IS '配置分组';
COMMENT ON COLUMN system_config.enabled IS '是否启用：true-启用，false-禁用';
COMMENT ON COLUMN system_config.readonly IS '是否只读：true-只读，false-可修改';
COMMENT ON COLUMN system_config.sort_order IS '排序';
COMMENT ON COLUMN system_config.remark IS '备注';
COMMENT ON COLUMN system_config.created_at IS '创建时间';
COMMENT ON COLUMN system_config.updated_at IS '更新时间';
COMMENT ON COLUMN system_config.deleted IS '删除标识：0-未删除，非0-已删除';

-- 创建索引
CREATE UNIQUE INDEX uk_system_config_key ON system_config (config_key, deleted);
CREATE INDEX idx_system_config_group ON system_config (config_group);
CREATE INDEX idx_system_config_enabled ON system_config (enabled);
CREATE INDEX idx_system_config_sort ON system_config (sort_order);

-- 插入默认配置数据
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, remark) VALUES
(1, 'system.name', 'NexusVoice', '系统名称', 'system', TRUE, FALSE, 1, '系统基础配置'),
(2, 'system.version', '1.0.0', '系统版本', 'system', TRUE, TRUE, 2, '系统版本信息，只读'),
(3, 'system.description', 'AI智能对话系统', '系统描述', 'system', TRUE, FALSE, 3, '系统描述信息'),
(4, 'ai.model.default', 'gpt-3.5-turbo', '默认AI模型', 'ai', TRUE, FALSE, 10, 'OpenAI默认模型'),
(5, 'ai.temperature', '0.7', 'AI温度参数', 'ai', TRUE, FALSE, 11, '控制AI回答的随机性'),
(6, 'ai.max_tokens', '2000', 'AI最大令牌数', 'ai', TRUE, FALSE, 12, '单次对话最大令牌数'),
(7, 'conversation.max_history', '20', '对话历史最大条数', 'conversation', TRUE, FALSE, 20, '保留的对话历史记录数量'),
(8, 'conversation.timeout', '300', '对话超时时间（秒）', 'conversation', TRUE, FALSE, 21, '对话会话超时时间'),
(9, 'file.upload.max_size', '10485760', '文件上传最大大小（字节）', 'file', TRUE, FALSE, 30, '10MB文件上传限制'),
(10, 'file.upload.allowed_types', 'jpg,jpeg,png,gif,pdf,txt,doc,docx', '允许上传的文件类型', 'file', TRUE, FALSE, 31, '文件类型白名单'),
(11, 'security.jwt.expire_time', '86400', 'JWT过期时间（秒）', 'security', TRUE, FALSE, 40, '24小时JWT有效期'),
(12, 'security.password.min_length', '6', '密码最小长度', 'security', TRUE, FALSE, 41, '用户密码最小长度要求'),
(13, 'cache.redis.expire_time', '3600', 'Redis缓存过期时间（秒）', 'cache', TRUE, FALSE, 50, '1小时缓存过期时间'),
(14, 'search.enabled', 'true', '是否启用搜索功能', 'search', TRUE, FALSE, 60, '控制联网搜索功能开关'),
(15, 'search.provider', 'duckduckgo', '搜索提供商', 'search', TRUE, FALSE, 61, '默认搜索引擎提供商');
