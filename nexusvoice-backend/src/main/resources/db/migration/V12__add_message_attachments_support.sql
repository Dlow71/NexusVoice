-- =====================================================
-- 为conversation_messages表添加附件支持
-- 版本: 12
-- 说明: 支持图片、文档、音频、视频等附件类型
-- =====================================================

-- 1. 添加附件相关字段
ALTER TABLE conversation_messages
    ADD COLUMN IF NOT EXISTS attachment_urls JSONB,
    ADD COLUMN IF NOT EXISTS attachment_count INTEGER DEFAULT 0;

-- 2. 创建索引（提升查询性能）
CREATE INDEX IF NOT EXISTS idx_messages_attachment_count 
ON conversation_messages(attachment_count) 
WHERE attachment_count > 0;

-- 3. 更新字段注释
COMMENT ON COLUMN conversation_messages.attachment_urls IS '附件URL列表，JSON格式存储，支持多种类型：[{"type":"image|document|audio|video","url":"CDN地址","name":"文件名","size":文件大小字节,"mimeType":"MIME类型"}]';
COMMENT ON COLUMN conversation_messages.attachment_count IS '附件数量，用于快速筛选包含附件的消息';

-- 4. 创建附件类型枚举说明（注释形式）
-- attachment_type枚举值：
-- - image: 图片文件（jpg, png, gif, webp等）
-- - document: 文档文件（pdf, doc, docx, xls, xlsx, txt等）
-- - audio: 音频文件（mp3, wav, ogg等）
-- - video: 视频文件（mp4, avi, mov等）

-- 示例数据格式：
-- attachment_urls: [
--   {
--     "type": "image",
--     "url": "https://cdn.nexusvoice.com/attachments/2025/01/image_123.jpg",
--     "name": "screenshot.jpg",
--     "size": 102400,
--     "mimeType": "image/jpeg",
--     "width": 1920,
--     "height": 1080
--   },
--   {
--     "type": "document",
--     "url": "https://cdn.nexusvoice.com/attachments/2025/01/doc_456.pdf",
--     "name": "report.pdf",
--     "size": 2048000,
--     "mimeType": "application/pdf"
--   }
-- ]
