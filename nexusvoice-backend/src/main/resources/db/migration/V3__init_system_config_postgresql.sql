-- =====================================================
-- 初始化系统配置数据
-- PostgreSQL版本
-- =====================================================

-- AI相关配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (1001, 'ai.default.model', 'openai:gpt-4o-mini', '默认AI模型（格式：provider:model）', 'ai', TRUE, FALSE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (1002, 'ai.default.temperature', '0.7', '默认温度参数（0.0-2.0）', 'ai', TRUE, FALSE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (1003, 'ai.default.max_tokens', '2000', '默认最大token数', 'ai', TRUE, FALSE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (1004, 'ai.enhancement.search.enabled', 'true', '是否启用联网搜索增强', 'ai', TRUE, FALSE, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (1005, 'ai.enhancement.rag.enabled', 'false', '是否启用RAG增强', 'ai', TRUE, FALSE, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- 对话相关配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (2001, 'conversation.max.history', '50', '对话保留的最大历史消息数', 'conversation', TRUE, FALSE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2002, 'conversation.title.auto_generate', 'true', '是否自动生成对话标题', 'conversation', TRUE, FALSE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2003, 'conversation.title.max_length', '50', '对话标题最大长度', 'conversation', TRUE, FALSE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2004, 'conversation.system_prompt.default', 'You are a helpful AI assistant.', '默认系统提示词', 'conversation', TRUE, FALSE, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- TTS语音合成配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (3001, 'tts.enabled', 'true', '是否启用TTS功能', 'tts', TRUE, FALSE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (3002, 'tts.default.voice', 'qiniu_zh_female_wwxkjx', '默认TTS声音类型', 'tts', TRUE, FALSE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (3003, 'tts.default.speed', '1.0', '默认语速（0.5-2.0）', 'tts', TRUE, FALSE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (3004, 'tts.chunk.enabled', 'true', '是否启用TTS分段处理', 'tts', TRUE, FALSE, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (3005, 'tts.chunk.max_chars', '300', 'TTS分段最大字符数', 'tts', TRUE, FALSE, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (3006, 'tts.chunk.max_concurrency', '4', 'TTS并发处理最大数', 'tts', TRUE, FALSE, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- 文件存储配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (4001, 'storage.provider', 'qiniu', '当前存储提供商：qiniu/minio', 'storage', TRUE, FALSE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4002, 'storage.fallback.enabled', 'false', '是否启用备用存储', 'storage', TRUE, FALSE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4003, 'storage.fallback.provider', 'minio', '备用存储提供商', 'storage', TRUE, FALSE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4004, 'storage.health_check.enabled', 'true', '是否启用存储健康检查', 'storage', TRUE, FALSE, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4005, 'storage.health_check.interval', '300', '健康检查间隔（秒）', 'storage', TRUE, FALSE, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- 七牛云存储配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (4101, 'storage.qiniu.enabled', 'true', '是否启用七牛云存储', 'storage', TRUE, FALSE, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4102, 'storage.qiniu.access_key', '', '七牛云Access Key', 'storage', TRUE, FALSE, 21, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4103, 'storage.qiniu.secret_key', '', '七牛云Secret Key', 'storage', TRUE, FALSE, 22, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4104, 'storage.qiniu.bucket', '', '七牛云存储桶名称', 'storage', TRUE, FALSE, 23, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4105, 'storage.qiniu.domain', '', '七牛云CDN域名', 'storage', TRUE, FALSE, 24, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4106, 'storage.qiniu.region', 'z0', '七牛云存储区域（z0/z1/z2等）', 'storage', TRUE, FALSE, 25, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- MinIO存储配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (4201, 'storage.minio.enabled', 'false', '是否启用MinIO存储', 'storage', TRUE, FALSE, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4202, 'storage.minio.endpoint', '', 'MinIO端点地址', 'storage', TRUE, FALSE, 31, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4203, 'storage.minio.access_key', '', 'MinIO Access Key', 'storage', TRUE, FALSE, 32, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4204, 'storage.minio.secret_key', '', 'MinIO Secret Key', 'storage', TRUE, FALSE, 33, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4205, 'storage.minio.bucket', '', 'MinIO存储桶名称', 'storage', TRUE, FALSE, 34, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- 文件迁移配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (4301, 'storage.migration.batch_size', '100', '文件迁移批次大小', 'storage', TRUE, FALSE, 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4302, 'storage.migration.concurrent', '5', '文件迁移并发数', 'storage', TRUE, FALSE, 41, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4303, 'storage.migration.retry_times', '3', '文件迁移重试次数', 'storage', TRUE, FALSE, 42, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- WebSocket相关配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (5001, 'websocket.enabled', 'true', '是否启用WebSocket', 'websocket', TRUE, FALSE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (5002, 'websocket.single_flight.enabled', 'true', '是否启用单flight保护', 'websocket', TRUE, FALSE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (5003, 'websocket.heartbeat.interval', '5000', '心跳间隔（毫秒）', 'websocket', TRUE, FALSE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (5004, 'websocket.message.max_size', '1048576', '消息最大大小（字节）', 'websocket', TRUE, FALSE, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- 搜索相关配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (6001, 'search.enabled', 'true', '是否启用搜索功能', 'search', TRUE, FALSE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (6002, 'search.provider', 'duckduckgo', '搜索提供商：duckduckgo/bing等', 'search', TRUE, FALSE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (6003, 'search.max_results', '5', '搜索结果最大数量', 'search', TRUE, FALSE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (6004, 'search.cache.enabled', 'true', '是否启用搜索结果缓存', 'search', TRUE, FALSE, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (6005, 'search.cache.ttl', '3600', '搜索缓存TTL（秒）', 'search', TRUE, FALSE, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- 图像生成配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (7001, 'image.generation.enabled', 'true', '是否启用图像生成功能', 'image', TRUE, FALSE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (7002, 'image.generation.provider', 'siliconflow', '图像生成提供商', 'image', TRUE, FALSE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (7003, 'image.generation.default_model', 'Qwen/QwenVL-v1-chat', '默认图像生成模型', 'image', TRUE, FALSE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (7004, 'image.generation.max_batch', '4', '批量生成最大数量', 'image', TRUE, FALSE, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- 角色助手配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (8001, 'role.assistant.enabled', 'true', '是否启用角色助手功能', 'role', TRUE, FALSE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (8002, 'role.assistant.auto_tts', 'true', '是否自动生成开场白TTS', 'role', TRUE, FALSE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (8003, 'role.max_private', '100', '用户最多创建私人角色数', 'role', TRUE, FALSE, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- 缓存配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (9001, 'cache.local.enabled', 'true', '是否启用本地缓存（Caffeine）', 'cache', TRUE, FALSE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (9002, 'cache.local.ttl', '30', '本地缓存TTL（秒）', 'cache', TRUE, FALSE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (9003, 'cache.redis.enabled', 'true', '是否启用Redis缓存', 'cache', TRUE, FALSE, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (9004, 'cache.redis.ttl', '3600', 'Redis缓存TTL（秒）', 'cache', TRUE, FALSE, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- 系统配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, created_at, updated_at, deleted)
VALUES
    (10001, 'system.name', 'NexusVoice', '系统名称', 'system', TRUE, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (10002, 'system.version', '1.0.0', '系统版本', 'system', TRUE, TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (10003, 'system.maintenance.enabled', 'false', '是否启用维护模式', 'system', TRUE, FALSE, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (config_key) DO NOTHING;

-- 创建序列
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = 'public' AND sequencename = 'system_config_id_seq') THEN
        CREATE SEQUENCE system_config_id_seq START WITH 20000;
    END IF;
END $$;
