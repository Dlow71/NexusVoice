-- =====================================================
-- RBAC + 菜单管理系统初始数据
-- 版本: V17
-- 说明: 插入系统角色、菜单、关联关系初始数据
-- =====================================================

SET search_path TO nexusvoice;

-- =====================================================
-- 1. 插入系统角色
-- =====================================================
INSERT INTO sys_roles (id, role_code, role_name, description, sort_order, status, is_system, created_at, updated_at, deleted)
VALUES
-- 超级管理员（拥有所有权限）
(1, 'super_admin', '超级管理员', '拥有系统所有权限，包括用户、角色、菜单的完整管理', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 管理员（系统管理员，不包括删除等危险操作）
(2, 'admin', '管理员', '系统管理员，可管理用户、角色、菜单，但某些危险操作受限', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 运营人员（负责AI配置、角色管理）
(3, 'operator', '运营人员', '负责AI模型配置、AI角色管理等运营工作', 3, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 审核员（内容审核）
(4, 'auditor', '审核员', '负责用户内容审核、对话记录审查等', 4, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 普通用户（默认角色）
(5, 'user', '普通用户', '系统默认用户角色，仅有前台功能权限', 5, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 2. 插入菜单数据
-- =====================================================

-- 2.1 一级目录
INSERT INTO menus (id, parent_id, menu_type, path, component, name, title, icon, permission, sort_order, visible, status, created_at, updated_at, deleted)
VALUES
-- 系统管理目录
(1, 0, 1, '/system', NULL, 'System', '系统管理', '&#xe7b9;', NULL, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- AI配置目录
(2, 0, 1, '/ai-config', NULL, 'AiConfig', 'AI配置', '&#xe63c;', NULL, 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 监控中心目录
(3, 0, 1, '/monitor', NULL, 'Monitor', '监控中心', '&#xe8d3;', NULL, 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 2.2 系统管理子菜单
INSERT INTO menus (id, parent_id, menu_type, path, component, name, title, icon, permission, sort_order, visible, status, created_at, updated_at, deleted)
VALUES
-- 用户管理
(10, 1, 2, '/system/user', 'system/user', 'User', '用户管理', '&#xe640;', 'system:user:view', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 角色管理
(11, 1, 2, '/system/role', 'system/role', 'Role', '角色管理', '&#xe63f;', 'system:role:view', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- 菜单管理
(12, 1, 2, '/system/menu', 'system/menu', 'Menu', '菜单管理', '&#xe641;', 'system:menu:view', 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 2.3 用户管理按钮权限
INSERT INTO menus (id, parent_id, menu_type, path, component, name, title, icon, permission, sort_order, visible, status, created_at, updated_at, deleted)
VALUES
(100, 10, 3, NULL, NULL, 'UserAdd', '新增用户', NULL, 'system:user:add', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(101, 10, 3, NULL, NULL, 'UserEdit', '编辑用户', NULL, 'system:user:edit', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(102, 10, 3, NULL, NULL, 'UserDelete', '删除用户', NULL, 'system:user:delete', 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(103, 10, 3, NULL, NULL, 'UserResetPassword', '重置密码', NULL, 'system:user:resetpwd', 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(104, 10, 3, NULL, NULL, 'UserAssignRole', '分配角色', NULL, 'system:user:assign', 5, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 2.4 角色管理按钮权限
INSERT INTO menus (id, parent_id, menu_type, path, component, name, title, icon, permission, sort_order, visible, status, created_at, updated_at, deleted)
VALUES
(110, 11, 3, NULL, NULL, 'RoleAdd', '新增角色', NULL, 'system:role:add', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(111, 11, 3, NULL, NULL, 'RoleEdit', '编辑角色', NULL, 'system:role:edit', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(112, 11, 3, NULL, NULL, 'RoleDelete', '删除角色', NULL, 'system:role:delete', 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(113, 11, 3, NULL, NULL, 'RoleAssignMenu', '分配菜单', NULL, 'system:role:menu', 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 2.5 菜单管理按钮权限
INSERT INTO menus (id, parent_id, menu_type, path, component, name, title, icon, permission, sort_order, visible, status, created_at, updated_at, deleted)
VALUES
(120, 12, 3, NULL, NULL, 'MenuAdd', '新增菜单', NULL, 'system:menu:add', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(121, 12, 3, NULL, NULL, 'MenuEdit', '编辑菜单', NULL, 'system:menu:edit', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(122, 12, 3, NULL, NULL, 'MenuDelete', '删除菜单', NULL, 'system:menu:delete', 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 2.6 AI配置子菜单
INSERT INTO menus (id, parent_id, menu_type, path, component, name, title, icon, permission, sort_order, visible, status, created_at, updated_at, deleted)
VALUES
-- AI模型管理
(20, 2, 2, '/ai-config/model', 'ai-config/model', 'AiModel', 'AI模型管理', '&#xe63c;', 'ai:model:view', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- AI角色管理
(21, 2, 2, '/ai-config/role', 'ai-config/role', 'AiRole', 'AI角色管理', '&#xe640;', 'ai:role:view', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 2.7 AI模型管理按钮权限
INSERT INTO menus (id, parent_id, menu_type, path, component, name, title, icon, permission, sort_order, visible, status, created_at, updated_at, deleted)
VALUES
(200, 20, 3, NULL, NULL, 'AiModelAdd', '新增模型', NULL, 'ai:model:add', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(201, 20, 3, NULL, NULL, 'AiModelEdit', '编辑模型', NULL, 'ai:model:edit', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(202, 20, 3, NULL, NULL, 'AiModelDelete', '删除模型', NULL, 'ai:model:delete', 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 2.8 监控中心子菜单
INSERT INTO menus (id, parent_id, menu_type, path, component, name, title, icon, permission, sort_order, visible, status, created_at, updated_at, deleted)
VALUES
-- 系统日志
(30, 3, 2, '/monitor/logs', 'monitor/logs', 'SystemLogs', '系统日志', '&#xe8d3;', 'monitor:logs:view', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- API调用统计
(31, 3, 2, '/monitor/api-stats', 'monitor/api-stats', 'ApiStats', 'API调用统计', '&#xe641;', 'monitor:api:view', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 3. 为super_admin角色分配所有菜单
-- =====================================================
INSERT INTO role_menus (id, role_id, menu_id, created_at)
SELECT 
    -- 使用菜单ID作为基础生成唯一ID（临时方案，实际应使用雪花ID生成器）
    1000000 + m.id,
    1,  -- super_admin角色ID
    m.id,
    CURRENT_TIMESTAMP
FROM menus m
WHERE m.deleted = 0
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- =====================================================
-- 4. 为admin角色分配菜单（拥有所有权限包括删除）
-- =====================================================
INSERT INTO role_menus (id, role_id, menu_id, created_at)
SELECT 
    2000000 + m.id,
    2,  -- admin角色ID
    m.id,
    CURRENT_TIMESTAMP
FROM menus m
WHERE m.deleted = 0
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- =====================================================
-- 5. 为operator角色分配AI配置菜单
-- =====================================================
INSERT INTO role_menus (id, role_id, menu_id, created_at)
SELECT 
    3000000 + m.id,
    3,  -- operator角色ID
    m.id,
    CURRENT_TIMESTAMP
FROM menus m
WHERE m.deleted = 0
  AND (m.id = 2 OR m.parent_id = 2)  -- AI配置目录及其子菜单
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- =====================================================
-- 6. 为现有ADMIN用户分配super_admin角色
-- =====================================================
INSERT INTO user_roles (id, user_id, role_id, created_at)
SELECT 
    -- 使用用户ID作为基础生成唯一ID
    1000000000 + u.id,
    u.id,
    1,  -- super_admin角色ID
    CURRENT_TIMESTAMP
FROM users u
WHERE u.user_type = 'ADMIN' 
  AND u.deleted = 0
ON CONFLICT (user_id, role_id) DO NOTHING;

-- =====================================================
-- 7. 为现有普通用户分配user角色
-- =====================================================
INSERT INTO user_roles (id, user_id, role_id, created_at)
SELECT 
    2000000000 + u.id,
    u.id,
    5,  -- user角色ID
    CURRENT_TIMESTAMP
FROM users u
WHERE u.user_type = 'USER' 
  AND u.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id
  )
ON CONFLICT (user_id, role_id) DO NOTHING;
