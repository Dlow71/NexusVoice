-- ============================================
-- RAG版本管理和用户安装系统表
-- 创建时间: 2025-10-22
-- 说明: 支持知识库版本管理和用户安装功能
-- ============================================

-- 1. RAG版本表（版本快照）
CREATE TABLE rag_versions (
    id BIGINT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    icon VARCHAR(500),
    description TEXT,
    version VARCHAR(50) NOT NULL COMMENT '语义化版本号（如 1.0.0）',
    change_log TEXT,
    labels TEXT COMMENT '标签（JSON字符串格式）',
    file_count INTEGER DEFAULT 0,
    total_size BIGINT DEFAULT 0,
    document_count INTEGER DEFAULT 0,
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_rag_versions_knowledge_base_id ON rag_versions(knowledge_base_id);
CREATE INDEX idx_rag_versions_user_id ON rag_versions(user_id);
CREATE INDEX idx_rag_versions_version ON rag_versions(version);
CREATE INDEX idx_rag_versions_published_at ON rag_versions(published_at);
CREATE INDEX idx_rag_versions_created_at ON rag_versions(created_at);
CREATE INDEX idx_rag_versions_deleted ON rag_versions(deleted);
CREATE UNIQUE INDEX uk_rag_versions_kb_version ON rag_versions(knowledge_base_id, version);

-- 注释
COMMENT ON TABLE rag_versions IS 'RAG版本表';
COMMENT ON COLUMN rag_versions.id IS '版本ID（雪花ID）';
COMMENT ON COLUMN rag_versions.knowledge_base_id IS '原始知识库ID';
COMMENT ON COLUMN rag_versions.user_id IS '创建者用户ID';
COMMENT ON COLUMN rag_versions.name IS '版本名称';
COMMENT ON COLUMN rag_versions.icon IS '图标URL';
COMMENT ON COLUMN rag_versions.description IS '版本描述';
COMMENT ON COLUMN rag_versions.version IS '语义化版本号（1.0.0、1.1.0等）';
COMMENT ON COLUMN rag_versions.change_log IS '更新日志';
COMMENT ON COLUMN rag_versions.labels IS '标签（JSON字符串格式）';
COMMENT ON COLUMN rag_versions.file_count IS '文件数量';
COMMENT ON COLUMN rag_versions.total_size IS '总大小（字节）';
COMMENT ON COLUMN rag_versions.document_count IS '文档单元数量';
COMMENT ON COLUMN rag_versions.published_at IS '发布时间';
COMMENT ON COLUMN rag_versions.created_at IS '创建时间';
COMMENT ON COLUMN rag_versions.updated_at IS '更新时间';
COMMENT ON COLUMN rag_versions.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 2. RAG版本文件表（文件快照）
CREATE TABLE rag_version_files (
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

CREATE INDEX idx_rag_version_files_rag_version_id ON rag_version_files(rag_version_id);
CREATE INDEX idx_rag_version_files_original_file_id ON rag_version_files(original_file_id);
CREATE INDEX idx_rag_version_files_file_name ON rag_version_files(file_name);
CREATE INDEX idx_rag_version_files_created_at ON rag_version_files(created_at);
CREATE INDEX idx_rag_version_files_deleted ON rag_version_files(deleted);

-- 注释
COMMENT ON TABLE rag_version_files IS 'RAG版本文件表';
COMMENT ON COLUMN rag_version_files.id IS '文件ID（雪花ID）';
COMMENT ON COLUMN rag_version_files.rag_version_id IS '关联的RAG版本ID';
COMMENT ON COLUMN rag_version_files.original_file_id IS '原始文件ID（仅标识）';
COMMENT ON COLUMN rag_version_files.file_name IS '文件名';
COMMENT ON COLUMN rag_version_files.file_size IS '文件大小（字节）';
COMMENT ON COLUMN rag_version_files.file_page_size IS '文件页数';
COMMENT ON COLUMN rag_version_files.file_type IS '文件类型';
COMMENT ON COLUMN rag_version_files.file_path IS '文件存储路径';
COMMENT ON COLUMN rag_version_files.process_status IS '处理状态：0-上传，1-OCR中，2-OCR完成，3-向量化中，4-完成，5-OCR失败，6-向量化失败';
COMMENT ON COLUMN rag_version_files.created_at IS '创建时间';
COMMENT ON COLUMN rag_version_files.updated_at IS '更新时间';
COMMENT ON COLUMN rag_version_files.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 3. RAG版本文档表（文档快照）
CREATE TABLE rag_version_documents (
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

CREATE INDEX idx_rag_version_documents_rag_version_id ON rag_version_documents(rag_version_id);
CREATE INDEX idx_rag_version_documents_rag_version_file_id ON rag_version_documents(rag_version_file_id);
CREATE INDEX idx_rag_version_documents_original_document_id ON rag_version_documents(original_document_id);
CREATE INDEX idx_rag_version_documents_page ON rag_version_documents(page);
CREATE INDEX idx_rag_version_documents_vector_id ON rag_version_documents(vector_id);
CREATE INDEX idx_rag_version_documents_created_at ON rag_version_documents(created_at);
CREATE INDEX idx_rag_version_documents_deleted ON rag_version_documents(deleted);

-- 注释
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

-- 4. 用户RAG表（用户安装的RAG）
CREATE TABLE user_rags (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    rag_version_id BIGINT NOT NULL,
    original_knowledge_base_id BIGINT NOT NULL,
    name VARCHAR(255),
    icon VARCHAR(500),
    description TEXT,
    version VARCHAR(50),
    install_type VARCHAR(20) DEFAULT 'SNAPSHOT' COMMENT 'REFERENCE-引用，SNAPSHOT-快照',
    installed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_user_rags_user_id ON user_rags(user_id);
CREATE INDEX idx_user_rags_rag_version_id ON user_rags(rag_version_id);
CREATE INDEX idx_user_rags_original_knowledge_base_id ON user_rags(original_knowledge_base_id);
CREATE INDEX idx_user_rags_install_type ON user_rags(install_type);
CREATE INDEX idx_user_rags_installed_at ON user_rags(installed_at);
CREATE INDEX idx_user_rags_created_at ON user_rags(created_at);
CREATE INDEX idx_user_rags_deleted ON user_rags(deleted);
CREATE UNIQUE INDEX uk_user_rags_user_version ON user_rags(user_id, rag_version_id);

-- 注释
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

-- 5. 用户RAG文件表（用户RAG文件快照）
CREATE TABLE user_rag_files (
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

CREATE INDEX idx_user_rag_files_user_rag_id ON user_rag_files(user_rag_id);
CREATE INDEX idx_user_rag_files_original_file_id ON user_rag_files(original_file_id);
CREATE INDEX idx_user_rag_files_file_name ON user_rag_files(file_name);
CREATE INDEX idx_user_rag_files_created_at ON user_rag_files(created_at);
CREATE INDEX idx_user_rag_files_deleted ON user_rag_files(deleted);

-- 注释
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

-- 6. 用户RAG文档表（用户RAG文档快照）
CREATE TABLE user_rag_documents (
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

CREATE INDEX idx_user_rag_documents_user_rag_id ON user_rag_documents(user_rag_id);
CREATE INDEX idx_user_rag_documents_user_rag_file_id ON user_rag_documents(user_rag_file_id);
CREATE INDEX idx_user_rag_documents_original_document_id ON user_rag_documents(original_document_id);
CREATE INDEX idx_user_rag_documents_page ON user_rag_documents(page);
CREATE INDEX idx_user_rag_documents_vector_id ON user_rag_documents(vector_id);
CREATE INDEX idx_user_rag_documents_created_at ON user_rag_documents(created_at);
CREATE INDEX idx_user_rag_documents_deleted ON user_rag_documents(deleted);

-- 注释
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

-- 7. 调整知识库表（添加缺失字段）
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS icon VARCHAR(500) COMMENT '图标URL';
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS labels TEXT COMMENT '标签（JSON字符串格式）';

-- 8. 简化文档单元表（调整字段）
-- 注意：如果有数据需要先备份
ALTER TABLE document_units DROP COLUMN IF EXISTS unit_type;
ALTER TABLE document_units DROP COLUMN IF EXISTS page_number;
ALTER TABLE document_units DROP COLUMN IF EXISTS paragraph_index;
ALTER TABLE document_units DROP COLUMN IF EXISTS chunk_index;
ALTER TABLE document_units DROP COLUMN IF EXISTS start_position;
ALTER TABLE document_units DROP COLUMN IF EXISTS end_position;
ALTER TABLE document_units DROP COLUMN IF EXISTS char_count;
ALTER TABLE document_units DROP COLUMN IF EXISTS token_count;
ALTER TABLE document_units DROP COLUMN IF EXISTS ocr_confidence;
ALTER TABLE document_units DROP COLUMN IF EXISTS is_vectorized;
ALTER TABLE document_units DROP COLUMN IF EXISTS language;
ALTER TABLE document_units DROP COLUMN IF EXISTS metadata;

ALTER TABLE document_units ADD COLUMN IF NOT EXISTS page INTEGER COMMENT '页码';
ALTER TABLE document_units ADD COLUMN IF NOT EXISTS is_vector BOOLEAN DEFAULT FALSE COMMENT '是否已向量化';
ALTER TABLE document_units MODIFY COLUMN is_ocr BOOLEAN DEFAULT FALSE COMMENT '是否已OCR处理';

-- ============================================
-- 创建完成提示
-- ============================================
-- 版本管理和用户安装系统表创建完成
