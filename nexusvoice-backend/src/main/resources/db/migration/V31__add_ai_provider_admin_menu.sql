-- AI服务商管理菜单及权限

INSERT INTO menus (
    id, parent_id, menu_type, path, component, name, title, icon, permission,
    sort_order, visible, status, keep_alive, created_at, updated_at, deleted
) VALUES
    (22, 2, 2, '/ai-config/provider', 'ai-config/provider', 'AiProvider', 'AI服务商管理', '&#xe63c;', 'ai:provider:view', 2, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (220, 22, 3, NULL, NULL, 'AiProviderAdd', '新增服务商', NULL, 'ai:provider:add', 1, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (221, 22, 3, NULL, NULL, 'AiProviderEdit', '编辑服务商', NULL, 'ai:provider:edit', 2, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (222, 22, 3, NULL, NULL, 'AiProviderDelete', '删除服务商', NULL, 'ai:provider:delete', 3, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (223, 22, 3, NULL, NULL, 'AiProviderEnable', '启用服务商', NULL, 'ai:provider:enable', 4, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (224, 22, 3, NULL, NULL, 'AiProviderDisable', '禁用服务商', NULL, 'ai:provider:disable', 5, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    menu_type = EXCLUDED.menu_type,
    path = EXCLUDED.path,
    component = EXCLUDED.component,
    name = EXCLUDED.name,
    title = EXCLUDED.title,
    icon = EXCLUDED.icon,
    permission = EXCLUDED.permission,
    sort_order = EXCLUDED.sort_order,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status,
    keep_alive = EXCLUDED.keep_alive,
    updated_at = CURRENT_TIMESTAMP,
    deleted = 0;

UPDATE menus
SET sort_order = 3,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 21
  AND deleted = 0
  AND sort_order <> 3;

INSERT INTO role_menus (id, role_id, menu_id, created_at)
VALUES
    (1000022, 1, 22, CURRENT_TIMESTAMP),
    (1000220, 1, 220, CURRENT_TIMESTAMP),
    (1000221, 1, 221, CURRENT_TIMESTAMP),
    (1000222, 1, 222, CURRENT_TIMESTAMP),
    (1000223, 1, 223, CURRENT_TIMESTAMP),
    (1000224, 1, 224, CURRENT_TIMESTAMP),
    (2000022, 2, 22, CURRENT_TIMESTAMP),
    (2000220, 2, 220, CURRENT_TIMESTAMP),
    (2000221, 2, 221, CURRENT_TIMESTAMP),
    (2000222, 2, 222, CURRENT_TIMESTAMP),
    (2000223, 2, 223, CURRENT_TIMESTAMP),
    (2000224, 2, 224, CURRENT_TIMESTAMP),
    (3000022, 3, 22, CURRENT_TIMESTAMP),
    (3000220, 3, 220, CURRENT_TIMESTAMP),
    (3000221, 3, 221, CURRENT_TIMESTAMP),
    (3000222, 3, 222, CURRENT_TIMESTAMP),
    (3000223, 3, 223, CURRENT_TIMESTAMP),
    (3000224, 3, 224, CURRENT_TIMESTAMP)
ON CONFLICT (role_id, menu_id) DO NOTHING;
