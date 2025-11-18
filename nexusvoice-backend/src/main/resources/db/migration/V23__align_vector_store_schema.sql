-- 对齐 vector_store 表结构到代码期望
-- 1) 将 embedding 列维度调整为 768（与默认 BCE 模型一致）
-- 2) 添加 embedding_dimension 列（若不存在）

-- 确保 pgvector 已安装
CREATE EXTENSION IF NOT EXISTS vector;

-- 尝试将向量列修改为 768 维（如已为 768 则不影响；如失败，打印提示并继续）
DO $$
BEGIN
    BEGIN
        EXECUTE 'ALTER TABLE vector_store ALTER COLUMN embedding TYPE vector(768)';
    EXCEPTION WHEN others THEN
        RAISE NOTICE 'Skip altering vector dimension: %', SQLERRM;
    END;
END $$;

-- 添加嵌入维度列
ALTER TABLE vector_store
    ADD COLUMN IF NOT EXISTS embedding_dimension INTEGER;

