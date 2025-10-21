-- =====================================================
-- NexusVoice初始化数据脚本
-- 版本: 1.0
-- 包含AI模型配置和系统配置初始数据
-- =====================================================

SET search_path TO nexusvoice;

-- =====================================================
-- 1. AI模型初始数据
-- =====================================================

INSERT INTO ai_models (id, provider_code, model_code, model_name, description, model_class, default_base_url, default_temperature, default_max_tokens, default_timeout_seconds, context_window, input_token_price, output_token_price, status, priority, created_at, updated_at, deleted)
VALUES
(1, 'openai', 'gpt-oss-20b', 'GPT OSS 20B', '高性价比开源大模型', 'OpenAiChatModel', 'https://openai.qiniu.com/v1', 0.70, 2000, 60, 8192, 0.000014, 0.000014, 1, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'grok', 'grok-4-fast', 'Grok 4 Fast', 'xAI Grok快速响应模型', 'OpenAiChatModel', 'https://openai.qiniu.com/v1', 0.70, 2000, 60, 32000, 0.001000, 0.002000, 1, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(13, 'deepseek', 'deepseek-v3.1', 'DeepSeek V3.1', 'DeepSeek V3.1 高性能推理模型', 'OpenAiChatModel', 'https://openai.qiniu.com/v1', 0.70, 8000, 120, 131072, 0.004000, 0.012000, 1, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 2. 系统配置初始数据
-- =====================================================

INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, remark, version, created_at, updated_at, deleted)
VALUES
-- 系统基础配置
(1, 'system.name', 'NexusVoice', '系统名称', 'system', 1, 0, 1, '系统显示名称', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'system.version', '1.0.0', '系统版本', 'system', 1, 0, 2, '当前系统版本号', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'system.description', 'AI智能对话系统', '系统描述', 'system', 1, 0, 3, '系统简介', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- AI模型配置
(4, 'ai.model.default', 'openai:gpt-oss-20b', '默认AI模型', 'ai', 1, 0, 10, '系统默认使用的AI模型', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, 'ai.temperature', '0.7', '默认温度参数', 'ai', 1, 0, 11, 'AI生成的随机性，0-2之间', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(6, 'ai.max_tokens', '2000', '默认最大tokens', 'ai', 1, 0, 12, 'AI生成的最大token数', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(16, 'ai.model.default.provider', 'openai', '默认AI厂商', 'ai', 1, 0, 13, '默认AI服务提供商', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(17, 'ai.model.default.code', 'gpt-oss-20b', '默认模型代码', 'ai', 1, 0, 14, '默认使用的模型代码', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(18, 'ai.temperature.default', '0.7', '默认温度', 'ai', 1, 0, 15, '温度参数默认值', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(19, 'ai.max_tokens.default', '2000', '默认最大tokens', 'ai', 1, 0, 16, 'tokens默认值', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(20, 'ai.system_prompt.default', '你是一个有用的AI助手', '默认系统提示词', 'ai', 1, 0, 17, '默认system prompt', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 对话配置
(7, 'conversation.max_history', '20', '对话历史最大数', 'conversation', 1, 0, 20, '保存的对话历史消息数量', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(8, 'conversation.timeout', '300', '对话超时时间', 'conversation', 1, 0, 21, '对话超时秒数', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(21, 'conversation.title.default', '新对话', '默认对话标题', 'conversation', 1, 0, 22, '新建对话的默认标题', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(22, 'conversation.max_messages', '100', '对话最大消息数', 'conversation', 1, 0, 23, '单个对话最多消息数', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(23, 'conversation.max_tokens', '50000', '对话最大tokens', 'conversation', 1, 0, 24, '单个对话最多token数', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 文件上传配置
(9, 'file.upload.max_size', '104857600', '文件上传最大大小', 'file', 1, 0, 30, '文件上传限制（字节）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(10, 'file.upload.allowed_types', 'jpg,jpeg,png,gif,pdf,txt,doc,docx', '允许的文件类型', 'file', 1, 0, 31, '允许上传的文件扩展名', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 安全配置
(11, 'security.jwt.expire_time', '86400', 'JWT过期时间', 'security', 1, 0, 40, 'JWT令牌有效期（秒）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(12, 'security.password.min_length', '6', '密码最小长度', 'security', 1, 0, 41, '用户密码最小字符数', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 缓存配置
(13, 'cache.redis.expire_time', '3600', 'Redis缓存过期时间', 'cache', 1, 0, 50, 'Redis缓存默认过期秒数', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(24, 'cache.system_config.expire_time', '300', '系统配置缓存过期时间', 'cache', 1, 0, 51, '系统配置缓存秒数', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 搜索配置
(14, 'search.enabled', 'true', '启用搜索功能', 'search', 1, 0, 60, '是否启用联网搜索', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(15, 'search.provider', 'tavily', '搜索提供商', 'search', 1, 0, 61, '搜索服务提供商', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- TTS配置
(25, 'tts.voice.default', 'qiniu_zh_female_wwxkjx', '默认TTS语音', 'tts', 1, 0, 70, '默认文本转语音声音类型', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(26, 'tts.speed.default', '1.0', '默认语速', 'tts', 1, 0, 71, '默认语音播放速度', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(27, 'tts.encoding.default', 'mp3', '默认音频编码', 'tts', 1, 0, 72, '默认音频格式', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 存储配置
(80, 'storage.provider', 'qiniu', '存储服务提供商', 'storage', 1, 0, 80, '当前使用的存储服务', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(81, 'storage.fallback.enabled', 'false', '启用备用存储', 'storage', 1, 0, 81, '是否启用备用存储服务', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(82, 'storage.fallback.provider', 'minio', '备用存储提供商', 'storage', 1, 0, 82, '备用存储服务', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(83, 'storage.migration.enabled', 'false', '启用文件迁移', 'storage', 1, 0, 83, '是否启用存储迁移功能', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(84, 'storage.health.check.enabled', 'true', '启用健康检查', 'storage', 1, 0, 84, '是否启用存储健康检查', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(85, 'storage.health.check.interval', '300', '健康检查间隔', 'storage', 1, 0, 85, '健康检查间隔秒数', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 七牛云存储配置
(90, 'storage.qiniu.enabled', 'true', '启用七牛云存储', 'storage.qiniu', 1, 0, 90, '是否启用七牛云存储', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(91, 'storage.qiniu.access_key', '', '七牛云AccessKey', 'storage.qiniu', 1, 0, 91, '七牛云访问密钥', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(92, 'storage.qiniu.secret_key', '', '七牛云SecretKey', 'storage.qiniu', 1, 0, 92, '七牛云密钥', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(93, 'storage.qiniu.bucket', 'nexusvoice', '七牛云Bucket', 'storage.qiniu', 1, 0, 93, '七牛云存储空间名称', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(94, 'storage.qiniu.domain', '', '七牛云访问域名', 'storage.qiniu', 1, 0, 94, '七牛云CDN域名', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(95, 'storage.qiniu.region', 'z2', '七牛云区域', 'storage.qiniu', 1, 0, 95, '七牛云存储区域：z0/z1/z2等', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(96, 'storage.qiniu.use_https', 'false', '使用HTTPS', 'storage.qiniu', 1, 0, 96, '是否使用HTTPS协议', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(97, 'storage.qiniu.dir.audio', 'audio/', '音频文件目录', 'storage.qiniu', 1, 0, 97, '音频文件存储路径', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(98, 'storage.qiniu.dir.image', 'image/', '图片文件目录', 'storage.qiniu', 1, 0, 98, '图片文件存储路径', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(99, 'storage.qiniu.dir.video', 'video/', '视频文件目录', 'storage.qiniu', 1, 0, 99, '视频文件存储路径', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(100, 'storage.qiniu.dir.other', 'other/', '其他文件目录', 'storage.qiniu', 1, 0, 100, '其他文件存储路径', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- MinIO存储配置
(110, 'storage.minio.enabled', 'true', '启用MinIO存储', 'storage.minio', 1, 0, 110, '是否启用MinIO存储', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(111, 'storage.minio.endpoint', '', 'MinIO端点', 'storage.minio', 1, 0, 111, 'MinIO服务地址', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(112, 'storage.minio.access_key', '', 'MinIO AccessKey', 'storage.minio', 1, 0, 112, 'MinIO访问密钥', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(113, 'storage.minio.secret_key', '', 'MinIO SecretKey', 'storage.minio', 1, 0, 113, 'MinIO密钥', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(114, 'storage.minio.bucket', 'nexusvoice', 'MinIO Bucket', 'storage.minio', 1, 0, 114, 'MinIO存储桶名称', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(115, 'storage.minio.region', 'us-east-1', 'MinIO区域', 'storage.minio', 1, 0, 115, 'MinIO区域配置', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;
