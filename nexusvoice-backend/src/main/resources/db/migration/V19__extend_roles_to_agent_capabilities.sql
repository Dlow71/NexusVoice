-- V19: 扩展 roles 表，添加 Agent 核心能力
-- 将角色系统升级为 Agent 系统，支持工具调用、知识库集成、多模态等能力

-- 扩展 roles 表，添加 Agent 核心字段
ALTER TABLE roles 
ADD COLUMN IF NOT EXISTS tool_ids JSONB,
ADD COLUMN IF NOT EXISTS tool_preset_params JSONB,
ADD COLUMN IF NOT EXISTS knowledge_base_ids JSONB,
ADD COLUMN IF NOT EXISTS multi_modal BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS tags JSONB,
ADD COLUMN IF NOT EXISTS config_params JSONB;

-- 更新表注释
COMMENT ON TABLE roles IS 'AI角色/Agent表 - 支持角色扮演对话、工具调用、知识库集成';

-- 新增字段注释
COMMENT ON COLUMN roles.tool_ids IS '可用工具ID列表（JSON数组），关联tools表';
COMMENT ON COLUMN roles.tool_preset_params IS '工具预设参数（JSON对象），为每个工具预设默认参数';
COMMENT ON COLUMN roles.knowledge_base_ids IS '关联的知识库ID列表（JSON数组），用于RAG增强对话';
COMMENT ON COLUMN roles.multi_modal IS '是否支持多模态输入（图像、语音等）';
COMMENT ON COLUMN roles.enabled IS '是否启用：TRUE-可用，FALSE-禁用（软删除的补充）';
COMMENT ON COLUMN roles.tags IS '角色标签（JSON数组），便于分类、筛选和推荐';
COMMENT ON COLUMN roles.config_params IS '扩展配置参数（JSON对象），存储其他自定义配置';

-- 更新现有记录，设置默认值
UPDATE roles
SET 
    multi_modal = FALSE,
    enabled = TRUE
WHERE multi_modal IS NULL OR enabled IS NULL;

-- 创建索引优化查询性能
CREATE INDEX IF NOT EXISTS idx_roles_enabled 
    ON roles(enabled) 
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_roles_tags 
    ON roles 
    USING GIN(tags) 
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_roles_multi_modal 
    ON roles(multi_modal) 
    WHERE deleted = 0 AND multi_modal = TRUE;
