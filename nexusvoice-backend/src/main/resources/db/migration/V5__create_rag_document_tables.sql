-- =====================================================
-- RAG文档处理系统表结构
-- 版本: 5.0
-- 功能: 知识库、文档处理、向量存储
-- =====================================================

-- 1. 知识库表 (knowledge_bases)
CREATE TABLE IF NOT EXISTS knowledge_bases (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    file_count INTEGER DEFAULT 0,
    total_size BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT fk_kb_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 知识库索引
CREATE INDEX idx_kb_user_id ON knowledge_bases(user_id);
CREATE INDEX idx_kb_status ON knowledge_bases(status);
CREATE INDEX idx_kb_created_at ON knowledge_bases(created_at DESC);

-- 知识库注释
COMMENT ON TABLE knowledge_bases IS '知识库表';
COMMENT ON COLUMN knowledge_bases.id IS '知识库ID（雪花ID）';
COMMENT ON COLUMN knowledge_bases.user_id IS '所属用户ID';
COMMENT ON COLUMN knowledge_bases.name IS '知识库名称';
COMMENT ON COLUMN knowledge_bases.description IS '知识库描述';
COMMENT ON COLUMN knowledge_bases.icon IS '图标标识';
COMMENT ON COLUMN knowledge_bases.status IS '状态：ACTIVE-活跃，ARCHIVED-归档，PROCESSING-处理中';
COMMENT ON COLUMN knowledge_bases.file_count IS '文件数量';
COMMENT ON COLUMN knowledge_bases.total_size IS '总大小（字节）';
COMMENT ON COLUMN knowledge_bases.created_at IS '创建时间';
COMMENT ON COLUMN knowledge_bases.updated_at IS '更新时间';
COMMENT ON COLUMN knowledge_bases.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 2. 文件详情表 (file_details)
CREATE TABLE IF NOT EXISTS file_details (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    knowledge_base_id BIGINT,
    filename VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT,
    storage_provider VARCHAR(20),
    storage_key VARCHAR(500) NOT NULL,
    storage_url TEXT NOT NULL,
    file_hash VARCHAR(64),
    file_page_count INTEGER,
    current_process_page INTEGER DEFAULT 0,
    process_progress NUMERIC(5,2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    error_code VARCHAR(50),
    error_message TEXT,
    parse_strategy VARCHAR(50),
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT fk_file_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_file_kb FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id)
);

-- 文件详情索引
CREATE INDEX idx_file_user_id ON file_details(user_id);
CREATE INDEX idx_file_kb_id ON file_details(knowledge_base_id);
CREATE INDEX idx_file_status ON file_details(status);
CREATE INDEX idx_file_type ON file_details(file_type);
CREATE INDEX idx_file_created_at ON file_details(created_at DESC);
CREATE INDEX idx_file_hash ON file_details(file_hash);

-- 文件详情注释
COMMENT ON TABLE file_details IS '文件详情表';
COMMENT ON COLUMN file_details.id IS '文件ID（雪花ID）';
COMMENT ON COLUMN file_details.user_id IS '上传用户ID';
COMMENT ON COLUMN file_details.knowledge_base_id IS '所属知识库ID（可选）';
COMMENT ON COLUMN file_details.filename IS '存储文件名（UUID命名）';
COMMENT ON COLUMN file_details.original_name IS '原始文件名';
COMMENT ON COLUMN file_details.file_type IS '文件类型：PDF/DOCX/DOC/TXT/MD/PPTX/PPT/XLSX/XLS/HTML';
COMMENT ON COLUMN file_details.mime_type IS 'MIME类型';
COMMENT ON COLUMN file_details.file_size IS '文件大小（字节）';
COMMENT ON COLUMN file_details.storage_provider IS '存储提供商：QINIU/MINIO';
COMMENT ON COLUMN file_details.storage_key IS '存储key';
COMMENT ON COLUMN file_details.storage_url IS '访问URL';
COMMENT ON COLUMN file_details.file_hash IS '文件MD5哈希值';
COMMENT ON COLUMN file_details.file_page_count IS '总页数/段落数';
COMMENT ON COLUMN file_details.current_process_page IS '当前处理页数';
COMMENT ON COLUMN file_details.process_progress IS '处理进度（%）';
COMMENT ON COLUMN file_details.status IS '状态：PENDING-待处理，UPLOADING-上传中，PARSING-解析中，SPLITTING-分割中，VECTORIZING-向量化中，COMPLETED-完成，FAILED-失败';
COMMENT ON COLUMN file_details.error_code IS '错误码';
COMMENT ON COLUMN file_details.error_message IS '错误信息';
COMMENT ON COLUMN file_details.parse_strategy IS '解析策略：PDF_TEXT/PDF_OCR/WORD_POI/EXCEL_POI/PPT_POI/TEXT_PLAIN/MARKDOWN/HTML';
COMMENT ON COLUMN file_details.processed_at IS '处理完成时间';
COMMENT ON COLUMN file_details.created_at IS '创建时间';
COMMENT ON COLUMN file_details.updated_at IS '更新时间';
COMMENT ON COLUMN file_details.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 3. 文档单元表 (document_units)
CREATE TABLE IF NOT EXISTS document_units (
    id BIGINT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    unit_type VARCHAR(20) DEFAULT 'TEXT',
    content TEXT NOT NULL,
    page_number INTEGER,
    paragraph_index INTEGER,
    chunk_index INTEGER,
    start_position INTEGER,
    end_position INTEGER,
    char_count INTEGER,
    token_count INTEGER,
    is_ocr BOOLEAN DEFAULT false,
    ocr_confidence NUMERIC(3,2),
    is_vectorized BOOLEAN DEFAULT false,
    language VARCHAR(10),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT fk_unit_file FOREIGN KEY (file_id) REFERENCES file_details(id) ON DELETE CASCADE
);

-- 文档单元索引
CREATE INDEX idx_unit_file_id ON document_units(file_id);
CREATE INDEX idx_unit_vectorized ON document_units(is_vectorized);
CREATE INDEX idx_unit_page ON document_units(file_id, page_number);
CREATE INDEX idx_unit_chunk ON document_units(file_id, chunk_index);

-- 文档单元注释
COMMENT ON TABLE document_units IS '文档单元表';
COMMENT ON COLUMN document_units.id IS '文档单元ID（雪花ID）';
COMMENT ON COLUMN document_units.file_id IS '关联文件ID';
COMMENT ON COLUMN document_units.unit_type IS '单元类型：TEXT-文本，TABLE-表格，IMAGE-图片描述';
COMMENT ON COLUMN document_units.content IS '文本内容';
COMMENT ON COLUMN document_units.page_number IS '页码（从1开始）';
COMMENT ON COLUMN document_units.paragraph_index IS '段落索引';
COMMENT ON COLUMN document_units.chunk_index IS '分块索引（同一页可能多个块）';
COMMENT ON COLUMN document_units.start_position IS '在原文中的起始位置';
COMMENT ON COLUMN document_units.end_position IS '在原文中的结束位置';
COMMENT ON COLUMN document_units.char_count IS '字符数';
COMMENT ON COLUMN document_units.token_count IS 'Token数量';
COMMENT ON COLUMN document_units.is_ocr IS '是否OCR处理';
COMMENT ON COLUMN document_units.ocr_confidence IS 'OCR识别置信度';
COMMENT ON COLUMN document_units.is_vectorized IS '是否已向量化';
COMMENT ON COLUMN document_units.language IS '语言代码：zh/en/ja等';
COMMENT ON COLUMN document_units.metadata IS '元数据：标题、作者、关键词等';
COMMENT ON COLUMN document_units.created_at IS '创建时间';
COMMENT ON COLUMN document_units.updated_at IS '更新时间';
COMMENT ON COLUMN document_units.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 4. 向量存储表（使用开源项目相同结构）
CREATE TABLE vector_store (
    embedding_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    embedding vector(1024),
    text TEXT,
    metadata JSONB,
    -- 添加我们需要的额外字段
    document_unit_id BIGINT,
    embedding_model VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_vector_store_document_unit_id ON vector_store(document_unit_id);
CREATE INDEX idx_vector_store_embedding_model ON vector_store(embedding_model);
CREATE INDEX idx_vector_store_created_at ON vector_store(created_at);
CREATE INDEX idx_vector_store_deleted ON vector_store(deleted);
-- 向量相似度搜索使用pgvector的索引
CREATE INDEX idx_vector_store_embedding ON vector_store USING hnsw (embedding vector_cosine_ops);

-- 向量注释
COMMENT ON TABLE vector_store IS '向量存储表';
COMMENT ON COLUMN vector_store.embedding_id IS '向量ID（UUID）';
COMMENT ON COLUMN vector_store.embedding IS '向量数据';
COMMENT ON COLUMN vector_store.text IS '文本内容';
COMMENT ON COLUMN vector_store.metadata IS '元数据';
COMMENT ON COLUMN vector_store.document_unit_id IS '关联文档单元ID';
COMMENT ON COLUMN vector_store.embedding_model IS '使用的向量模型：bce-embedding-base_v1等';
COMMENT ON COLUMN vector_store.created_at IS '创建时间';
COMMENT ON COLUMN vector_store.updated_at IS '更新时间';
COMMENT ON COLUMN vector_store.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 5. 文档处理任务表 (document_process_tasks)
CREATE TABLE IF NOT EXISTS document_process_tasks (
    id BIGINT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    task_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    priority INTEGER DEFAULT 0,
    retry_count INTEGER DEFAULT 0,
    max_retry INTEGER DEFAULT 3,
    error_message TEXT,
    scheduled_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT fk_task_file FOREIGN KEY (file_id) REFERENCES file_details(id)
);

-- 任务表索引
CREATE INDEX idx_task_file_id ON document_process_tasks(file_id);
CREATE INDEX idx_task_status ON document_process_tasks(status);
CREATE INDEX idx_task_type ON document_process_tasks(task_type);
CREATE INDEX idx_task_scheduled ON document_process_tasks(scheduled_at);

-- 任务表注释
COMMENT ON TABLE document_process_tasks IS '文档处理任务表';
COMMENT ON COLUMN document_process_tasks.id IS '任务ID（雪花ID）';
COMMENT ON COLUMN document_process_tasks.file_id IS '关联文件ID';
COMMENT ON COLUMN document_process_tasks.task_type IS '任务类型：PARSE-解析，OCR-OCR识别，SPLIT-分割，VECTORIZE-向量化';
COMMENT ON COLUMN document_process_tasks.status IS '状态：PENDING-待处理，RUNNING-执行中，SUCCESS-成功，FAILED-失败，CANCELLED-取消';
COMMENT ON COLUMN document_process_tasks.priority IS '优先级：数值越大优先级越高';
COMMENT ON COLUMN document_process_tasks.retry_count IS '重试次数';
COMMENT ON COLUMN document_process_tasks.max_retry IS '最大重试次数';
COMMENT ON COLUMN document_process_tasks.error_message IS '错误信息';
COMMENT ON COLUMN document_process_tasks.scheduled_at IS '计划执行时间';
COMMENT ON COLUMN document_process_tasks.started_at IS '开始执行时间';
COMMENT ON COLUMN document_process_tasks.completed_at IS '完成时间';
COMMENT ON COLUMN document_process_tasks.created_at IS '创建时间';
COMMENT ON COLUMN document_process_tasks.updated_at IS '更新时间';
COMMENT ON COLUMN document_process_tasks.deleted IS '逻辑删除：0-未删除，1-已删除';
