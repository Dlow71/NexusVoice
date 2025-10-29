-- =====================================================
-- RBAC + 菜单管理系统表结构
-- 版本: V16
-- 说明: 创建系统角色、菜单、关联关系表
-- 注意: roles表已被AI聊天角色使用，系统角色使用sys_roles
-- =====================================================

SET search_path TO nexusvoice;

-- =====================================================
-- 1. 系统角色表 (sys_roles)
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_roles (
    id BIGINT PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    sort_order INT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    is_system SMALLINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0,
    
    CONSTRAINT uk_sys_roles_code UNIQUE (role_code)
);

CREATE INDEX IF NOT EXISTS idx_sys_roles_status ON sys_roles(status);
CREATE INDEX IF NOT EXISTS idx_sys_roles_deleted ON sys_roles(deleted);

COMMENT ON TABLE sys_roles IS '系统角色表（区别于AI聊天角色roles表）';
COMMENT ON COLUMN sys_roles.id IS '角色唯一ID（雪花ID）';
COMMENT ON COLUMN sys_roles.role_code IS '角色编码，全局唯一，用于代码中判断：admin, operator, auditor等';
COMMENT ON COLUMN sys_roles.role_name IS '角色名称，用于界面显示：管理员、运营人员等';
COMMENT ON COLUMN sys_roles.description IS '角色描述';
COMMENT ON COLUMN sys_roles.sort_order IS '排序，数字越小越靠前';
COMMENT ON COLUMN sys_roles.status IS '状态：0-禁用 1-启用';
COMMENT ON COLUMN sys_roles.is_system IS '是否系统内置角色：0-否 1-是（系统内置角色不允许删除）';
COMMENT ON COLUMN sys_roles.created_at IS '创建时间';
COMMENT ON COLUMN sys_roles.updated_at IS '更新时间';
COMMENT ON COLUMN sys_roles.deleted IS '逻辑删除标识：0-未删除 1-已删除';

-- =====================================================
-- 2. 用户角色关联表 (user_roles)
-- =====================================================
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_user_roles UNIQUE (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role_id);

COMMENT ON TABLE user_roles IS '用户角色关联表（多对多关系）';
COMMENT ON COLUMN user_roles.id IS '主键ID（雪花ID）';
COMMENT ON COLUMN user_roles.user_id IS '用户ID，关联users.id';
COMMENT ON COLUMN user_roles.role_id IS '角色ID，关联sys_roles.id';
COMMENT ON COLUMN user_roles.created_at IS '关联创建时间';

-- =====================================================
-- 3. 菜单表 (menus)
-- =====================================================
CREATE TABLE IF NOT EXISTS menus (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    menu_type SMALLINT NOT NULL,
    path VARCHAR(200),
    component VARCHAR(200),
    name VARCHAR(50) NOT NULL,
    title VARCHAR(50) NOT NULL,
    icon VARCHAR(50),
    permission VARCHAR(100),
    sort_order INT DEFAULT 0,
    visible SMALLINT DEFAULT 1,
    status SMALLINT DEFAULT 1,
    keep_alive SMALLINT DEFAULT 0,
    external_link VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_menus_parent ON menus(parent_id);
CREATE INDEX IF NOT EXISTS idx_menus_status ON menus(status);
CREATE INDEX IF NOT EXISTS idx_menus_sort ON menus(sort_order);
CREATE INDEX IF NOT EXISTS idx_menus_deleted ON menus(deleted);

COMMENT ON TABLE menus IS '系统菜单表';
COMMENT ON COLUMN menus.id IS '菜单唯一ID（雪花ID）';
COMMENT ON COLUMN menus.parent_id IS '父菜单ID，0表示根菜单';
COMMENT ON COLUMN menus.menu_type IS '菜单类型：1-目录(有子菜单的父节点) 2-菜单(页面) 3-按钮(页面内操作权限)';
COMMENT ON COLUMN menus.path IS '路由路径，如：/system/user';
COMMENT ON COLUMN menus.component IS '组件路径，如：system/user/index';
COMMENT ON COLUMN menus.name IS '路由名称（唯一），如：User';
COMMENT ON COLUMN menus.title IS '菜单标题（显示文本），如：用户管理';
COMMENT ON COLUMN menus.icon IS '菜单图标（Element Plus图标或Iconfont编码）';
COMMENT ON COLUMN menus.permission IS '权限标识，格式：模块:功能:操作，如：system:user:add';
COMMENT ON COLUMN menus.sort_order IS '同级菜单排序，数字越小越靠前';
COMMENT ON COLUMN menus.visible IS '是否在菜单中显示：0-隐藏 1-显示';
COMMENT ON COLUMN menus.status IS '菜单状态：0-禁用 1-启用';
COMMENT ON COLUMN menus.keep_alive IS '是否缓存页面：0-否 1-是';
COMMENT ON COLUMN menus.external_link IS '外部链接地址（如果是外链菜单）';
COMMENT ON COLUMN menus.created_at IS '创建时间';
COMMENT ON COLUMN menus.updated_at IS '更新时间';
COMMENT ON COLUMN menus.deleted IS '逻辑删除标识：0-未删除 1-已删除';

-- =====================================================
-- 4. 角色菜单关联表 (role_menus)
-- =====================================================
CREATE TABLE IF NOT EXISTS role_menus (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_role_menus UNIQUE (role_id, menu_id)
);

CREATE INDEX IF NOT EXISTS idx_role_menus_role ON role_menus(role_id);
CREATE INDEX IF NOT EXISTS idx_role_menus_menu ON role_menus(menu_id);

COMMENT ON TABLE role_menus IS '角色菜单关联表（多对多关系）';
COMMENT ON COLUMN role_menus.id IS '主键ID（雪花ID）';
COMMENT ON COLUMN role_menus.role_id IS '角色ID，关联sys_roles.id';
COMMENT ON COLUMN role_menus.menu_id IS '菜单ID，关联menus.id';
COMMENT ON COLUMN role_menus.created_at IS '关联创建时间';
