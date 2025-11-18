-- 创建pgvector索引和优化配置
-- 基于RAG检索实战指南的最佳实践
-- @author NexusVoice
-- @since 2025-01-12

-- ==================== 1. 安装pgvector扩展 ====================

-- 创建pgvector扩展（如果未安装）
CREATE EXTENSION IF NOT EXISTS vector;

-- ==================== 2. 创建中文全文检索配置（需要先创建） ====================

-- 创建中文分词配置（如果未安装zhparser，使用simple配置）
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese_cfg') THEN
        -- 尝试使用zhparser，如果不存在则使用simple
        IF EXISTS (SELECT 1 FROM pg_ts_parser WHERE prsname = 'zhparser') THEN
            CREATE TEXT SEARCH CONFIGURATION chinese_cfg (PARSER = zhparser);
        ELSE
            -- 退化到simple配置
            CREATE TEXT SEARCH CONFIGURATION chinese_cfg (PARSER = simple);
            COMMENT ON TEXT SEARCH CONFIGURATION chinese_cfg IS '简单中文分词配置（未安装zhparser）';
        END IF;
    END IF;
END $$;

-- ==================== 3. 创建向量索引 ====================

-- HNSW索引（推荐生产环境使用，精度高、速度快）
-- m = 16: 每个节点的最大连接数（默认16，范围2-100）
-- ef_construction = 64: 构建索引时的动态候选列表大小（默认64，范围4-1000）
-- 使用余弦相似度操作符（vector_cosine_ops）
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding_hnsw 
ON vector_store 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- 注释：
-- - 索引构建时间：m和ef_construction越大，构建越慢但查询精度越高
-- - 查询性能：可通过 SET hnsw.ef_search = 100 调整查询时的候选列表大小
-- - 余弦相似度：最适合文本向量检索，范围[-1, 1]，越接近1越相似

-- ==================== 4. 创建metadata JSONB索引 ====================

-- GIN索引用于metadata字段查询
-- 支持高效的JSONB字段查询和过滤
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_gin 
ON vector_store 
USING gin (metadata);

-- 注释：
-- - 支持查询：metadata @> '{"key": "value"}'
-- - 支持过滤：(metadata ->> 'DATA_SET_ID') = 'xxx'

-- ==================== 5. 创建全文检索索引 ====================

-- 为document_units表的content字段创建全文检索索引
-- 使用中文分词配置（chinese_cfg）
CREATE INDEX IF NOT EXISTS idx_document_units_content_fts 
ON document_units 
USING gin (to_tsvector('chinese_cfg', content))
WHERE deleted = 0;

-- 注释：
-- - 中文分词：需要PostgreSQL安装中文分词扩展（zhparser或jieba_zh）
-- - 查询示例：to_tsvector('chinese_cfg', content) @@ plainto_tsquery('chinese_cfg', '查询关键词')
-- - 排序：ts_rank(to_tsvector('chinese_cfg', content), plainto_tsquery('chinese_cfg', '查询'))

-- ==================== 6. 创建常用查询索引 ====================

-- document_unit_id索引（用于关联查询）
CREATE INDEX IF NOT EXISTS idx_vector_store_document_unit_id 
ON vector_store (document_unit_id) 
WHERE deleted = 0;

-- embedding_model索引（用于按模型过滤）
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding_model 
ON vector_store (embedding_model) 
WHERE deleted = 0;

-- created_at索引（用于时间排序）
CREATE INDEX IF NOT EXISTS idx_vector_store_created_at 
ON vector_store (created_at DESC) 
WHERE deleted = 0;

-- 联合索引：document_unit_id + deleted（优化存在性检查）
CREATE INDEX IF NOT EXISTS idx_vector_store_doc_unit_deleted 
ON vector_store (document_unit_id, deleted);

-- ==================== 7. 优化配置建议（需要在postgresql.conf中配置） ====================

-- 以下是建议的PostgreSQL配置参数，需要手动修改postgresql.conf文件：

/*
# ========== 内存配置 ==========
shared_buffers = 4GB                    # 共享缓冲区，建议物理内存的25%
effective_cache_size = 12GB             # 告知优化器可用内存，建议物理内存的50-75%
work_mem = 256MB                        # 单个查询操作的内存，排序/聚合使用
maintenance_work_mem = 2GB              # 维护操作内存，CREATE INDEX等使用

# ========== 查询优化 ==========
max_parallel_workers_per_gather = 4     # 并行查询的工作进程数
max_parallel_workers = 8                # 最大并行工作进程数
random_page_cost = 1.1                  # SSD建议设为1.1（默认4.0）

# ========== pgvector配置 ==========
# 设置HNSW索引的查询参数（运行时可通过SET修改）
# SET hnsw.ef_search = 100;            # 查询时的候选列表大小，越大精度越高但速度越慢（默认40）

# ========== 连接池配置 ==========
max_connections = 200                   # 最大连接数
*/

-- ==================== 8. 性能监控查询 ====================

-- 检查索引使用情况
-- SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'public' AND tablename = 'vector_store';

-- 检查表统计信息
-- SELECT schemaname, tablename, n_tup_ins, n_tup_upd, n_tup_del, n_live_tup, n_dead_tup, last_vacuum, last_analyze
-- FROM pg_stat_user_tables
-- WHERE tablename = 'vector_store';

-- 检查向量索引性能
-- EXPLAIN ANALYZE
-- SELECT id, (1 - (embedding <=> '[0.1,0.2,...]'::vector)) AS similarity
-- FROM vector_store
-- WHERE deleted = 0
-- ORDER BY embedding <=> '[0.1,0.2,...]'::vector
-- LIMIT 10;

-- ==================== 9. 维护脚本 ====================

-- 定期执行VACUUM和ANALYZE以保持性能
-- VACUUM ANALYZE vector_store;
-- VACUUM ANALYZE document_units;

-- 重建索引（如果性能下降）
-- REINDEX INDEX CONCURRENTLY idx_vector_store_embedding_hnsw;
-- REINDEX INDEX CONCURRENTLY idx_document_units_content_fts;

-- ==================== 说明 ====================
-- 
-- 1. 索引说明：
--    - HNSW索引：用于向量相似度检索，支持余弦相似度
--    - GIN索引：用于JSONB metadata字段和全文检索
--    - B-tree索引：用于常规字段查询和排序
--
-- 2. 查询优化：
--    - 向量检索使用 <=> 操作符（余弦距离）
--    - 相似度计算：1 - (embedding <=> query) 得到0-1之间的分数
--    - 全文检索使用 @@ 操作符和 ts_rank 函数排序
--
-- 3. 性能调优：
--    - 调整 hnsw.ef_search 参数平衡精度和速度
--    - 定期执行 VACUUM ANALYZE 保持统计信息准确
--    - 使用 EXPLAIN ANALYZE 分析慢查询
--
-- 4. 中文分词：
--    - 推荐安装 zhparser 扩展获得更好的中文分词效果
--    - 安装方法：https://github.com/amutu/zhparser
--    - 如未安装，系统将使用simple配置作为降级方案
--
-- 5. 监控指标：
--    - 索引扫描次数（idx_scan）应该较高
--    - 死元组数（n_dead_tup）应该接近0
--    - 查询延迟应该 < 100ms（有HNSW索引时）
