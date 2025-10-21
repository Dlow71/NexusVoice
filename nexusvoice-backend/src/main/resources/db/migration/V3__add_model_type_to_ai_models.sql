-- 为ai_models表添加model_type字段，支持Embedding和Rerank模型
-- @author NexusVoice
-- @since 2025-10-21

-- 1. 添加model_type字段
ALTER TABLE ai_models ADD COLUMN model_type VARCHAR(20) NOT NULL DEFAULT 'chat';

-- 2. 添加字段注释
COMMENT ON COLUMN ai_models.model_type IS '模型类型：chat-对话模型 embedding-向量模型 rerank-重排序模型';

-- 3. 创建索引优化查询性能
CREATE INDEX idx_ai_models_type_status ON ai_models(model_type, status, deleted);

-- 4. 创建联合唯一索引（provider_code + model_code + model_type）
-- 注意：先删除旧的唯一索引（如果存在）
DROP INDEX IF EXISTS uk_provider_model;
CREATE UNIQUE INDEX uk_provider_model_type ON ai_models(provider_code, model_code, model_type) WHERE deleted = 0;

-- 5. 添加硅基流动Embedding模型配置
INSERT INTO ai_models (
    id, provider_code, model_code, model_type, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    config_json, status, priority, created_at, updated_at, deleted
) VALUES
-- netease-youdao/bce-embedding-base_v1 (推荐向量模型，768维)
(
    2001, 'siliconflow', 'netease-youdao/bce-embedding-base_v1', 'embedding',
    '硅基流动 BCE-Embedding-Base-V1', '网易有道向量模型，768维，高质量中文支持，性价比最高',
    'OpenAiEmbeddingModel', 'https://api.siliconflow.cn/v1',
    NULL, NULL, 60, 512,
    0.0005, 0.000,
    '{"dimensions": 768, "maxBatchSize": 10}',
    1, 100, NOW(), NOW(), 0
);

-- 6. 添加硅基流动Rerank模型配置
INSERT INTO ai_models (
    id, provider_code, model_code, model_type, model_name, description,
    model_class, default_base_url, default_temperature, default_max_tokens,
    default_timeout_seconds, context_window, input_token_price, output_token_price,
    config_json, status, priority, created_at, updated_at, deleted
) VALUES
-- BAAI/bge-reranker-v2-m3 (推荐重排序模型)
(
    3001, 'siliconflow', 'BAAI/bge-reranker-v2-m3', 'rerank',
    '硅基流动 BGE-Reranker-V2-M3', '高性能重排序模型，支持多语言，优化搜索结果相关性，支持长文档分块',
    'SiliconFlowRerankModel', 'https://api.siliconflow.cn/v1',
    NULL, NULL, 60, 8192,
    0.001, 0.000,
    '{"maxCandidates": 100, "maxChunksPerDoc": 800, "overlapTokens": 80}',
    1, 100, NOW(), NOW(), 0
);
