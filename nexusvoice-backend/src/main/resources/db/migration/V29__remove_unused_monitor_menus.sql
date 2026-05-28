-- 清理当前未实现且不再需要的监控菜单
-- 目标：
-- 1. 删除角色与菜单的绑定，避免继续下发权限
-- 2. 逻辑删除“系统日志”和“API调用统计”菜单，避免动态路由报错

DELETE FROM role_menus
WHERE menu_id IN (
    SELECT id
    FROM menus
    WHERE deleted = 0
      AND permission IN ('monitor:logs:view', 'monitor:api:view')
);

UPDATE menus
SET deleted = 1,
    status = 0,
    visible = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE deleted = 0
  AND permission IN ('monitor:logs:view', 'monitor:api:view');
