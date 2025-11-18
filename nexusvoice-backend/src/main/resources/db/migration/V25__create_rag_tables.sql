-- =====================================================
-- NexusVoice PostgreSQL RAG 系统表
-- 版本: 25.0
-- 说明: 创建知识库、文档、向量存储等RAG相关表
-- =====================================================

-- 1. 知识库表 (knowledge_bases)
CREATE TABLE IF NOT EXISTS knowledge_bases (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(500),
    labels TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    file_count INTEGER DEFAULT 0,
    total_size BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_kb_user_id ON knowledge_bases(user_id);
CREATE INDEX IF NOT EXISTS idx_kb_status ON knowledge_bases(status);
CREATE INDEX IF NOT EXISTS idx_kb_created_at ON knowledge_bases(created_at DESC);

COMMENT ON TABLE knowledge_bases IS '知识库表';
COMMENT ON COLUMN knowledge_bases.id IS '知识库ID（雪花ID）';
COMMENT ON COLUMN knowledge_bases.user_id IS '所属用户ID';
COMMENT ON COLUMN knowledge_bases.name IS '知识库名称';
COMMENT ON COLUMN knowledge_bases.description IS '知识库描述';
COMMENT ON COLUMN knowledge_bases.icon IS '图标标识';
COMMENT ON COLUMN knowledge_bases.labels IS '标签（JSON字符串格式）';
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
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_file_user_id ON file_details(user_id);
CREATE INDEX IF NOT EXISTS idx_file_kb_id ON file_details(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_file_status ON file_details(status);
CREATE INDEX IF NOT EXISTS idx_file_type ON file_details(file_type);
CREATE INDEX IF NOT EXISTS idx_file_created_at ON file_details(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_file_hash ON file_details(file_hash);

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
    content TEXT NOT NULL,
    page INTEGER,
    is_ocr BOOLEAN DEFAULT false,
    is_vector BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_unit_file_id ON document_units(file_id);
CREATE INDEX IF NOT EXISTS idx_unit_is_vector ON document_units(is_vector);
CREATE INDEX IF NOT EXISTS idx_unit_page ON document_units(file_id, page);

COMMENT ON TABLE document_units IS '文档单元表';
COMMENT ON COLUMN document_units.id IS '文档单元ID（雪花ID）';
COMMENT ON COLUMN document_units.file_id IS '关联文件ID';
COMMENT ON COLUMN document_units.content IS '文本内容';
COMMENT ON COLUMN document_units.page IS '页码（从1开始）';
COMMENT ON COLUMN document_units.is_ocr IS '是否OCR处理';
COMMENT ON COLUMN document_units.is_vector IS '是否已向量化';
COMMENT ON COLUMN document_units.created_at IS '创建时间';
COMMENT ON COLUMN document_units.updated_at IS '更新时间';
COMMENT ON COLUMN document_units.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 4. 向量存储表 (vector_store)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS vector_store (
    embedding_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    embedding vector(768),
    text TEXT,
    metadata JSONB,
    document_unit_id BIGINT,
    embedding_model VARCHAR(100),
    embedding_dimension INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_vector_store_document_unit_id ON vector_store(document_unit_id);
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding_model ON vector_store(embedding_model);
CREATE INDEX IF NOT EXISTS idx_vector_store_created_at ON vector_store(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vector_store_deleted ON vector_store(deleted);

COMMENT ON TABLE vector_store IS '向量存储表';
COMMENT ON COLUMN vector_store.embedding_id IS '向量ID（UUID）';
COMMENT ON COLUMN vector_store.embedding IS '向量数据（768维）';
COMMENT ON COLUMN vector_store.text IS '文本内容';
COMMENT ON COLUMN vector_store.metadata IS '元数据';
COMMENT ON COLUMN vector_store.document_unit_id IS '关联文档单元ID';
COMMENT ON COLUMN vector_store.embedding_model IS '使用的向量模型';
COMMENT ON COLUMN vector_store.embedding_dimension IS '向量维度';
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
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_task_file_id ON document_process_tasks(file_id);
CREATE INDEX IF NOT EXISTS idx_task_status ON document_process_tasks(status);
CREATE INDEX IF NOT EXISTS idx_task_type ON document_process_tasks(task_type);
CREATE INDEX IF NOT EXISTS idx_task_scheduled ON document_process_tasks(scheduled_at);

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

-- 6. RAG版本表 (rag_versions)
CREATE TABLE IF NOT EXISTS rag_versions (
    id BIGINT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    icon VARCHAR(500),
    description TEXT,
    version VARCHAR(50) NOT NULL,
    change_log TEXT,
    labels TEXT,
    file_count INTEGER DEFAULT 0,
    total_size BIGINT DEFAULT 0,
    document_count INTEGER DEFAULT 0,
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_rag_versions_knowledge_base_id ON rag_versions(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_rag_versions_user_id ON rag_versions(user_id);
CREATE INDEX IF NOT EXISTS idx_rag_versions_version ON rag_versions(version);
CREATE INDEX IF NOT EXISTS idx_rag_versions_published_at ON rag_versions(published_at);
CREATE INDEX IF NOT EXISTS idx_rag_versions_created_at ON rag_versions(created_at);
CREATE INDEX IF NOT EXISTS idx_rag_versions_deleted ON rag_versions(deleted);
CREATE UNIQUE INDEX IF NOT EXISTS uk_rag_versions_kb_version ON rag_versions(knowledge_base_id, version);

COMMENT ON TABLE rag_versions IS 'RAG版本表';
COMMENT ON COLUMN rag_versions.id IS '版本ID（雪花ID）';
COMMENT ON COLUMN rag_versions.knowledge_base_id IS '原始知识库ID';
COMMENT ON COLUMN rag_versions.user_id IS '创建者用户ID';
COMMENT ON COLUMN rag_versions.name IS '版本名称';
COMMENT ON COLUMN rag_versions.icon IS '图标URL';
COMMENT ON COLUMN rag_versions.description IS '版本描述';
COMMENT ON COLUMN rag_versions.version IS '语义化版本号';
COMMENT ON COLUMN rag_versions.change_log IS '更新日志';
COMMENT ON COLUMN rag_versions.labels IS '标签（JSON字符串格式）';
COMMENT ON COLUMN rag_versions.file_count IS '文件数量';
COMMENT ON COLUMN rag_versions.total_size IS '总大小（字节）';
COMMENT ON COLUMN rag_versions.document_count IS '文档单元数量';
COMMENT ON COLUMN rag_versions.published_at IS '发布时间';
COMMENT ON COLUMN rag_versions.created_at IS '创建时间';
COMMENT ON COLUMN rag_versions.updated_at IS '更新时间';
COMMENT ON COLUMN rag_versions.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 7. RAG版本文件表 (rag_version_files)
CREATE TABLE IF NOT EXISTS rag_version_files (
    id BIGINT PRIMARY KEY,
    rag_version_id BIGINT NOT NULL,
    original_file_id BIGINT,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT DEFAULT 0,
    file_page_size INTEGER,
    file_type VARCHAR(50),
    file_path VARCHAR(500),
    process_status INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_rag_version_files_rag_version_id ON rag_version_files(rag_version_id);
CREATE INDEX IF NOT EXISTS idx_rag_version_files_original_file_id ON rag_version_files(original_file_id);
CREATE INDEX IF NOT EXISTS idx_rag_version_files_file_name ON rag_version_files(file_name);
CREATE INDEX IF NOT EXISTS idx_rag_version_files_created_at ON rag_version_files(created_at);
CREATE INDEX IF NOT EXISTS idx_rag_version_files_deleted ON rag_version_files(deleted);

COMMENT ON TABLE rag_version_files IS 'RAG版本文件表';
COMMENT ON COLUMN rag_version_files.id IS '文件ID（雪花ID）';
COMMENT ON COLUMN rag_version_files.rag_version_id IS '关联的RAG版本ID';
COMMENT ON COLUMN rag_version_files.original_file_id IS '原始文件ID（仅标识）';
COMMENT ON COLUMN rag_version_files.file_name IS '文件名';
COMMENT ON COLUMN rag_version_files.file_size IS '文件大小（字节）';
COMMENT ON COLUMN rag_version_files.file_page_size IS '文件页数';
COMMENT ON COLUMN rag_version_files.file_type IS '文件类型';
COMMENT ON COLUMN rag_version_files.file_path IS '文件存储路径';
COMMENT ON COLUMN rag_version_files.process_status IS '处理状态';
COMMENT ON COLUMN rag_version_files.created_at IS '创建时间';
COMMENT ON COLUMN rag_version_files.updated_at IS '更新时间';
COMMENT ON COLUMN rag_version_files.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 8. RAG版本文档表 (rag_version_documents)
CREATE TABLE IF NOT EXISTS rag_version_documents (
    id BIGINT PRIMARY KEY,
    rag_version_id BIGINT NOT NULL,
    rag_version_file_id BIGINT,
    original_document_id BIGINT,
    content TEXT,
    page INTEGER,
    vector_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_rag_version_documents_rag_version_id ON rag_version_documents(rag_version_id);
CREATE INDEX IF NOT EXISTS idx_rag_version_documents_rag_version_file_id ON rag_version_documents(rag_version_file_id);
CREATE INDEX IF NOT EXISTS idx_rag_version_documents_original_document_id ON rag_version_documents(original_document_id);
CREATE INDEX IF NOT EXISTS idx_rag_version_documents_page ON rag_version_documents(page);
CREATE INDEX IF NOT EXISTS idx_rag_version_documents_vector_id ON rag_version_documents(vector_id);
CREATE INDEX IF NOT EXISTS idx_rag_version_documents_created_at ON rag_version_documents(created_at);
CREATE INDEX IF NOT EXISTS idx_rag_version_documents_deleted ON rag_version_documents(deleted);

COMMENT ON TABLE rag_version_documents IS 'RAG版本文档表';
COMMENT ON COLUMN rag_version_documents.id IS '文档ID（雪花ID）';
COMMENT ON COLUMN rag_version_documents.rag_version_id IS '关联的RAG版本ID';
COMMENT ON COLUMN rag_version_documents.rag_version_file_id IS '关联的版本文件ID';
COMMENT ON COLUMN rag_version_documents.original_document_id IS '原始文档单元ID（仅标识）';
COMMENT ON COLUMN rag_version_documents.content IS '文档内容';
COMMENT ON COLUMN rag_version_documents.page IS '页码';
COMMENT ON COLUMN rag_version_documents.vector_id IS '向量ID（在pgvector中的ID）';
COMMENT ON COLUMN rag_version_documents.created_at IS '创建时间';
COMMENT ON COLUMN rag_version_documents.updated_at IS '更新时间';
COMMENT ON COLUMN rag_version_documents.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 9. 用户RAG表 (user_rags)
CREATE TABLE IF NOT EXISTS user_rags (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    rag_version_id BIGINT NOT NULL,
    original_knowledge_base_id BIGINT NOT NULL,
    name VARCHAR(255),
    icon VARCHAR(500),
    description TEXT,
    version VARCHAR(50),
    install_type VARCHAR(20) DEFAULT 'SNAPSHOT',
    installed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_user_rags_user_id ON user_rags(user_id);
CREATE INDEX IF NOT EXISTS idx_user_rags_rag_version_id ON user_rags(rag_version_id);
CREATE INDEX IF NOT EXISTS idx_user_rags_original_knowledge_base_id ON user_rags(original_knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_user_rags_install_type ON user_rags(install_type);
CREATE INDEX IF NOT EXISTS idx_user_rags_installed_at ON user_rags(installed_at);
CREATE INDEX IF NOT EXISTS idx_user_rags_created_at ON user_rags(created_at);
CREATE INDEX IF NOT EXISTS idx_user_rags_deleted ON user_rags(deleted);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_rags_user_version ON user_rags(user_id, rag_version_id);

COMMENT ON TABLE user_rags IS '用户RAG表';
COMMENT ON COLUMN user_rags.id IS 'RAG安装ID（雪花ID）';
COMMENT ON COLUMN user_rags.user_id IS '安装用户ID';
COMMENT ON COLUMN user_rags.rag_version_id IS '关联的版本快照ID';
COMMENT ON COLUMN user_rags.original_knowledge_base_id IS '原始知识库ID';
COMMENT ON COLUMN user_rags.name IS '安装时的名称';
COMMENT ON COLUMN user_rags.icon IS '安装时的图标URL';
COMMENT ON COLUMN user_rags.description IS '安装时的描述';
COMMENT ON COLUMN user_rags.version IS '版本号';
COMMENT ON COLUMN user_rags.install_type IS '安装类型：REFERENCE-引用（动态），SNAPSHOT-快照（静态）';
COMMENT ON COLUMN user_rags.installed_at IS '安装时间';
COMMENT ON COLUMN user_rags.created_at IS '创建时间';
COMMENT ON COLUMN user_rags.updated_at IS '更新时间';
COMMENT ON COLUMN user_rags.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 10. 用户RAG文件表 (user_rag_files)
CREATE TABLE IF NOT EXISTS user_rag_files (
    id BIGINT PRIMARY KEY,
    user_rag_id BIGINT NOT NULL,
    original_file_id BIGINT,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT DEFAULT 0,
    file_page_size INTEGER,
    file_type VARCHAR(50),
    file_path VARCHAR(500),
    process_status INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_user_rag_files_user_rag_id ON user_rag_files(user_rag_id);
CREATE INDEX IF NOT EXISTS idx_user_rag_files_original_file_id ON user_rag_files(original_file_id);
CREATE INDEX IF NOT EXISTS idx_user_rag_files_file_name ON user_rag_files(file_name);
CREATE INDEX IF NOT EXISTS idx_user_rag_files_created_at ON user_rag_files(created_at);
CREATE INDEX IF NOT EXISTS idx_user_rag_files_deleted ON user_rag_files(deleted);

COMMENT ON TABLE user_rag_files IS '用户RAG文件表';
COMMENT ON COLUMN user_rag_files.id IS '文件ID（雪花ID）';
COMMENT ON COLUMN user_rag_files.user_rag_id IS '关联的用户RAG ID';
COMMENT ON COLUMN user_rag_files.original_file_id IS '原始文件ID（仅标识）';
COMMENT ON COLUMN user_rag_files.file_name IS '文件名';
COMMENT ON COLUMN user_rag_files.file_size IS '文件大小（字节）';
COMMENT ON COLUMN user_rag_files.file_page_size IS '文件页数';
COMMENT ON COLUMN user_rag_files.file_type IS '文件类型';
COMMENT ON COLUMN user_rag_files.file_path IS '文件存储路径';
COMMENT ON COLUMN user_rag_files.process_status IS '处理状态';
COMMENT ON COLUMN user_rag_files.created_at IS '创建时间';
COMMENT ON COLUMN user_rag_files.updated_at IS '更新时间';
COMMENT ON COLUMN user_rag_files.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 11. 用户RAG文档表 (user_rag_documents)
CREATE TABLE IF NOT EXISTS user_rag_documents (
    id BIGINT PRIMARY KEY,
    user_rag_id BIGINT NOT NULL,
    user_rag_file_id BIGINT,
    original_document_id BIGINT,
    content TEXT,
    page INTEGER,
    vector_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_user_rag_documents_user_rag_id ON user_rag_documents(user_rag_id);
CREATE INDEX IF NOT EXISTS idx_user_rag_documents_user_rag_file_id ON user_rag_documents(user_rag_file_id);
CREATE INDEX IF NOT EXISTS idx_user_rag_documents_original_document_id ON user_rag_documents(original_document_id);
CREATE INDEX IF NOT EXISTS idx_user_rag_documents_page ON user_rag_documents(page);
CREATE INDEX IF NOT EXISTS idx_user_rag_documents_vector_id ON user_rag_documents(vector_id);
CREATE INDEX IF NOT EXISTS idx_user_rag_documents_created_at ON user_rag_documents(created_at);
CREATE INDEX IF NOT EXISTS idx_user_rag_documents_deleted ON user_rag_documents(deleted);

COMMENT ON TABLE user_rag_documents IS '用户RAG文档表';
COMMENT ON COLUMN user_rag_documents.id IS '文档ID（雪花ID）';
COMMENT ON COLUMN user_rag_documents.user_rag_id IS '关联的用户RAG ID';
COMMENT ON COLUMN user_rag_documents.user_rag_file_id IS '关联的用户RAG文件ID';
COMMENT ON COLUMN user_rag_documents.original_document_id IS '原始文档单元ID（仅标识）';
COMMENT ON COLUMN user_rag_documents.content IS '文档内容';
COMMENT ON COLUMN user_rag_documents.page IS '页码';
COMMENT ON COLUMN user_rag_documents.vector_id IS '向量ID（在pgvector中的ID）';
COMMENT ON COLUMN user_rag_documents.created_at IS '创建时间';
COMMENT ON COLUMN user_rag_documents.updated_at IS '更新时间';
COMMENT ON COLUMN user_rag_documents.deleted IS '逻辑删除：0-未删除，1-已删除';

-- =====================================================
-- 创建完成提示
-- =====================================================
-- RAG系统所有表创建完成
