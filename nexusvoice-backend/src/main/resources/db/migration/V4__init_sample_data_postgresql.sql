-- =====================================================
-- 初始化示例数据（可选）
-- PostgreSQL版本
-- 包含：测试用户、公共角色
-- =====================================================

-- 1. 创建测试用户（密码：123456，需要在代码中加密）
-- 注意：实际部署时应删除或修改此测试数据
INSERT INTO users (id, email, password_hash, nickname, avatar_url, user_type, status, email_verified, created_at, updated_at, deleted)
VALUES
    (1000000000000000001, 'admin@nexusvoice.ai', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 
     'Admin', 'https://avatar.example.com/admin.png', 'ADMIN', 'NORMAL', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (1000000000000000002, 'test@nexusvoice.ai', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 
     '测试用户', 'https://avatar.example.com/test.png', 'USER', 'NORMAL', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 2. 创建公共AI角色
INSERT INTO roles (id, name, description, persona_prompt, greeting_message, greeting_audio_url, avatar_url, voiceType, is_public, user_id, created_at, updated_at, deleted)
VALUES
    -- 默认助手
    (2000000000000000001, '智能助手', '通用AI助手，可以回答各种问题', 
     'You are a helpful AI assistant. You are knowledgeable, friendly, and always ready to help users with their questions and tasks.', 
     '你好！我是智能助手，有什么可以帮助你的吗？', 
     NULL, 'https://avatar.example.com/assistant.png', 'qiniu_zh_female_wwxkjx', 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    
    -- 编程助手
    (2000000000000000002, '编程助手', '专业的编程和技术助手', 
     'You are an expert programming assistant. You are proficient in multiple programming languages and frameworks. You can help users with coding problems, debugging, code review, and technical architecture design.', 
     '你好！我是编程助手，我可以帮你解决编程问题、调试代码、进行代码审查等。', 
     NULL, 'https://avatar.example.com/programmer.png', 'qiniu_zh_male_qn_jiaran', 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    
    -- 写作助手
    (2000000000000000003, '写作助手', '帮助你进行文字创作', 
     'You are a professional writing assistant. You can help users with creative writing, content creation, copywriting, editing, and proofreading. You have a good command of language and writing techniques.', 
     '你好！我是写作助手，无论是文章创作、文案编写还是内容润色，我都可以帮到你。', 
     NULL, 'https://avatar.example.com/writer.png', 'qiniu_zh_female_aibao', 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    
    -- 学习导师
    (2000000000000000004, '学习导师', '帮助你学习新知识', 
     'You are a patient and knowledgeable learning tutor. You can explain complex concepts in simple terms, provide step-by-step guidance, and adapt your teaching style to the learner''s needs.', 
     '你好！我是学习导师，我会用简单易懂的方式帮你理解新知识。', 
     NULL, 'https://avatar.example.com/tutor.png', 'qiniu_zh_female_aiya', 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    
    -- 生活顾问
    (2000000000000000005, '生活顾问', '提供生活建议和帮助', 
     'You are a friendly life advisor. You can provide practical advice on daily life, health, relationships, personal development, and general well-being. You are empathetic and supportive.', 
     '你好！我是生活顾问，在生活中遇到什么困惑都可以和我聊聊。', 
     NULL, 'https://avatar.example.com/advisor.png', 'qiniu_zh_female_aixia', 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 3. 创建索引优化查询
-- 这些索引在V1已创建，这里仅作为注释说明
-- CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
-- CREATE INDEX IF NOT EXISTS idx_roles_is_public ON roles(is_public);

-- 4. 添加注释
COMMENT ON TABLE users IS '用户表 - 存储系统用户信息';
COMMENT ON TABLE roles IS 'AI角色表 - 存储AI角色配置，支持公共和私人角色';

-- 5. 输出统计信息
DO $$
DECLARE
    user_count INTEGER;
    role_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO user_count FROM users WHERE deleted = 0;
    SELECT COUNT(*) INTO role_count FROM roles WHERE deleted = 0;
    
    RAISE NOTICE '===========================================';
    RAISE NOTICE 'Sample Data Initialization Complete';
    RAISE NOTICE 'Users created: %', user_count;
    RAISE NOTICE 'Public roles created: %', role_count;
    RAISE NOTICE '===========================================';
END $$;
