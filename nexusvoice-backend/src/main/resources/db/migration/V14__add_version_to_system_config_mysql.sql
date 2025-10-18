-- 为system_config表添加version字段用于乐观锁
ALTER TABLE system_config 
ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）' AFTER remark;

-- 为现有记录初始化version
UPDATE system_config SET version = 0 WHERE version IS NULL;

-- 添加索引以优化并发更新性能
CREATE INDEX idx_system_config_key_version ON system_config(config_key, version);
