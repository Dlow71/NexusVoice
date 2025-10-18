-- 添加存储服务相关配置（MySQL版本）
-- 作者：NexusVoice Team
-- 日期：2025-10-18

-- ====================存储服务基础配置====================
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, remark) VALUES
(80, 'storage.provider', 'qiniu', '存储服务提供商（qiniu/minio）', 'storage', 1, 0, 80, '当前使用的存储服务提供商，支持qiniu和minio'),
(81, 'storage.fallback.enabled', 'false', '是否启用备用存储', 'storage', 1, 0, 81, '当主存储服务不可用时是否自动切换到备用存储'),
(82, 'storage.fallback.provider', 'minio', '备用存储服务提供商', 'storage', 1, 0, 82, '备用存储服务提供商，主存储不可用时使用'),
(83, 'storage.migration.enabled', 'false', '是否启用文件迁移', 'storage', 1, 0, 83, '是否启用文件迁移功能'),
(84, 'storage.health.check.enabled', 'true', '是否启用健康检查', 'storage', 1, 0, 84, '是否启用存储服务健康检查'),
(85, 'storage.health.check.interval', '300', '健康检查间隔（秒）', 'storage', 1, 0, 85, '存储服务健康检查间隔时间');

-- ====================七牛云存储配置====================
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, remark) VALUES
(90, 'storage.qiniu.enabled', 'true', '是否启用七牛云存储', 'storage.qiniu', 1, 0, 90, '控制七牛云存储服务开关'),
(91, 'storage.qiniu.access_key', '', '七牛云Access Key', 'storage.qiniu', 1, 0, 91, '七牛云访问密钥，请在七牛云控制台获取'),
(92, 'storage.qiniu.secret_key', '', '七牛云Secret Key', 'storage.qiniu', 1, 0, 92, '七牛云密钥，请在七牛云控制台获取'),
(93, 'storage.qiniu.bucket', 'nexusvoice', '七牛云存储空间名称', 'storage.qiniu', 1, 0, 93, '七牛云存储空间（Bucket）名称'),
(94, 'storage.qiniu.domain', '', '七牛云访问域名', 'storage.qiniu', 1, 0, 94, '七牛云CDN访问域名，格式：https://cdn.example.com'),
(95, 'storage.qiniu.region', 'auto', '七牛云存储区域', 'storage.qiniu', 1, 0, 95, '存储区域：auto/华东/华北/华南/北美/东南亚'),
(96, 'storage.qiniu.use_https', 'true', '是否使用HTTPS', 'storage.qiniu', 1, 0, 96, '是否使用HTTPS协议访问'),
(97, 'storage.qiniu.dir.audio', 'audio/', '音频文件目录', 'storage.qiniu', 1, 0, 97, '音频文件存储目录路径'),
(98, 'storage.qiniu.dir.image', 'image/', '图片文件目录', 'storage.qiniu', 1, 0, 98, '图片文件存储目录路径'),
(99, 'storage.qiniu.dir.video', 'video/', '视频文件目录', 'storage.qiniu', 1, 0, 99, '视频文件存储目录路径'),
(100, 'storage.qiniu.dir.other', 'other/', '其他文件目录', 'storage.qiniu', 1, 0, 100, '其他类型文件存储目录路径');

-- ====================MinIO存储配置====================
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, remark) VALUES
(110, 'storage.minio.enabled', 'false', '是否启用MinIO存储', 'storage.minio', 1, 0, 110, '控制MinIO存储服务开关'),
(111, 'storage.minio.endpoint', 'http://localhost:9000', 'MinIO服务端点', 'storage.minio', 1, 0, 111, 'MinIO服务地址，包含协议和端口'),
(112, 'storage.minio.access_key', 'minioadmin', 'MinIO Access Key', 'storage.minio', 1, 0, 112, 'MinIO访问密钥'),
(113, 'storage.minio.secret_key', 'minioadmin', 'MinIO Secret Key', 'storage.minio', 1, 0, 113, 'MinIO密钥'),
(114, 'storage.minio.bucket', 'nexusvoice', 'MinIO存储桶名称', 'storage.minio', 1, 0, 114, 'MinIO存储桶（Bucket）名称'),
(115, 'storage.minio.region', 'us-east-1', 'MinIO区域', 'storage.minio', 1, 0, 115, 'MinIO区域设置，默认us-east-1'),
(116, 'storage.minio.use_ssl', 'false', '是否使用SSL', 'storage.minio', 1, 0, 116, '是否使用SSL/TLS加密连接'),
(117, 'storage.minio.dir.audio', 'audio/', '音频文件目录', 'storage.minio', 1, 0, 117, '音频文件存储目录路径'),
(118, 'storage.minio.dir.image', 'image/', '图片文件目录', 'storage.minio', 1, 0, 118, '图片文件存储目录路径'),
(119, 'storage.minio.dir.video', 'video/', '视频文件目录', 'storage.minio', 1, 0, 119, '视频文件存储目录路径'),
(120, 'storage.minio.dir.other', 'other/', '其他文件目录', 'storage.minio', 1, 0, 120, '其他类型文件存储目录路径'),
(121, 'storage.minio.public_url', '', 'MinIO公网访问地址', 'storage.minio', 1, 0, 121, '用于生成公网访问URL的地址，留空则使用endpoint');

-- ====================文件上传配置增强====================
UPDATE system_config SET 
  config_value = '104857600',
  description = '文件上传最大大小（字节）',
  remark = '100MB文件上传限制，适用于所有存储服务'
WHERE config_key = 'file.upload.max_size';

INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, remark) VALUES
(130, 'file.upload.chunk.enabled', 'true', '是否启用分片上传', 'file', 1, 0, 130, '大文件是否启用分片上传'),
(131, 'file.upload.chunk.size', '5242880', '分片大小（字节）', 'file', 1, 0, 131, '5MB每个分片，用于大文件上传'),
(132, 'file.upload.concurrent', '3', '并发上传数', 'file', 1, 0, 132, '同时并发上传的文件数量限制');

-- ====================文件迁移配置====================
INSERT INTO system_config (id, config_key, config_value, description, config_group, enabled, readonly, sort_order, remark) VALUES
(140, 'storage.migration.batch_size', '100', '批量迁移大小', 'storage.migration', 1, 0, 140, '每批次迁移的文件数量'),
(141, 'storage.migration.concurrent', '3', '并发迁移数', 'storage.migration', 1, 0, 141, '同时并发迁移的线程数'),
(142, 'storage.migration.retry_times', '3', '迁移失败重试次数', 'storage.migration', 1, 0, 142, '单个文件迁移失败后的重试次数'),
(143, 'storage.migration.delete_source', 'false', '迁移后删除源文件', 'storage.migration', 1, 0, 143, '迁移成功后是否删除源存储的文件');

-- 添加索引提升查询性能
CREATE INDEX IF NOT EXISTS idx_config_key_group ON system_config(config_key, config_group);
