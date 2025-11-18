-- RAG系统配置和API密钥
-- 添加RAG相关的system_config配置项和embedding/rerank模型的API密钥
-- @author NexusVoice
-- @since 2025-01-12

-- ==================== RAG系统配置 ====================

-- 1. RAG向量化配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, version, created_at, updated_at, deleted)
VALUES 
(150, 'rag.embedding.model', 'siliconflow:netease-youdao/bce-embedding-base_v1', 'RAG向量化默认模型（格式：provider:model）', 'RAG', 0, NOW(), NOW(), 0),
(151, 'rag.embedding.batch_size', '10', 'RAG向量化批次大小', 'RAG', 0, NOW(), NOW(), 0),
(152, 'rag.embedding.timeout', '60', 'RAG向量化超时时间（秒）', 'RAG', 0, NOW(), NOW(), 0);

-- 2. RAG检索配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, version, created_at, updated_at, deleted)
VALUES 
(153, 'rag.rerank.model', 'siliconflow:BAAI/bge-reranker-v2-m3', 'RAG重排序默认模型（格式：provider:model）', 'RAG', 0, NOW(), NOW(), 0),
(154, 'rag.search.topk', '15', 'RAG检索初始返回数量（向量+关键词各取topk）', 'RAG', 0, NOW(), NOW(), 0),
(155, 'rag.search.rerank_topk', '5', 'RAG重排序后最终返回数量', 'RAG', 0, NOW(), NOW(), 0),
(156, 'rag.search.similarity_threshold', '0.7', 'RAG检索相似度阈值（0-1）', 'RAG', 0, NOW(), NOW(), 0),
(157, 'rag.search.rrf_k', '60', 'RRF融合算法常数K', 'RAG', 0, NOW(), NOW(), 0);

-- 3. RAG向量存储配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, version, created_at, updated_at, deleted)
VALUES 
(158, 'rag.vector.dimension', '768', '默认向量维度（BCE模型为768维）', 'RAG', 0, NOW(), NOW(), 0),
(159, 'rag.vector.index_type', 'hnsw', 'pgvector索引类型（hnsw/ivfflat）', 'RAG', 0, NOW(), NOW(), 0);

-- 4. RAG文档处理配置
INSERT INTO system_config (id, config_key, config_value, description, config_group, version, created_at, updated_at, deleted)
VALUES 
(160, 'rag.document.max_chunk_size', '512', '文档分块最大字符数', 'RAG', 0, NOW(), NOW(), 0),
(161, 'rag.document.chunk_overlap', '50', '文档分块重叠字符数', 'RAG', 0, NOW(), NOW(), 0);

-- ==================== API密钥配置 ====================

-- 5. 硅基流动Embedding模型API密钥
-- 注意：请替换 'your_siliconflow_embedding_api_key' 为真实的API密钥
INSERT INTO ai_api_keys (
    id, provider_code, model_code, api_key, 
    base_url, weight, daily_limit, monthly_limit,
    is_enabled, created_at, updated_at, deleted
) VALUES
-- BCE Embedding模型密钥
(
    2001001, 
    'siliconflow', 
    'netease-youdao/bce-embedding-base_v1', 
    'your_siliconflow_embedding_api_key',
    'https://api.siliconflow.cn/v1',
    10,
    1000000,
    30000000,
    1,
    NOW(),
    NOW(),
    0
);

-- 6. 硅基流动Rerank模型API密钥
-- 注意：请替换 'your_siliconflow_rerank_api_key' 为真实的API密钥
INSERT INTO ai_api_keys (
    id, provider_code, model_code, api_key, 
    base_url, weight, daily_limit, monthly_limit,
    is_enabled, created_at, updated_at, deleted
) VALUES
-- BGE Rerank模型密钥
(
    3001001, 
    'siliconflow', 
    'BAAI/bge-reranker-v2-m3', 
    'your_siliconflow_rerank_api_key',
    'https://api.siliconflow.cn/v1',
    10,
    100000,
    3000000,
    1,
    NOW(),
    NOW(),
    0
);

-- ==================== 说明 ====================
-- 
-- 1. 配置项说明：
--    - rag.embedding.model: 默认向量化模型，BCE性价比最高（0.0005元/千tokens）
--    - rag.rerank.model: 默认重排序模型，BGE性能优秀（0.001元/千tokens）
--    - rag.search.topk: 初始检索数量，建议15-20
--    - rag.search.rerank_topk: 最终返回数量，建议3-5
--    - rag.search.similarity_threshold: 相似度阈值，0.7表示70%相似
--
-- 2. API密钥配置：
--    - 需要在硅基流动平台（https://siliconflow.cn）申请API密钥
--    - BCE Embedding和BGE Rerank可以使用同一个密钥
--    - daily_limit和monthly_limit根据实际业务量调整
--
-- 3. 使用方式：
--    - 向量化：DocumentVectorizationServiceImpl自动读取rag.embedding.model
--    - 检索：DocumentRetrievalService自动读取rag.rerank.model
--    - 修改配置后立即生效，无需重启服务（SystemConfigCacheService支持热更新）
--
-- 4. 费用估算（按1M tokens计算）：
--    - BCE Embedding: 0.0005元/千tokens * 1000 = 0.5元
--    - BGE Rerank: 0.001元/千tokens * 1000 = 1元
--    - 总计：1.5元/百万tokens（非常经济）
