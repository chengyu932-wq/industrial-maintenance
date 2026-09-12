USE industrial_maintenance;
SET NAMES utf8mb4;

-- 第5阶段增量数据脚本：故障报修与维修工单菜单/按钮权限，可安全重复执行。
INSERT INTO sys_menu(parent_id,menu_type,name,path,component,permission_code,sort_no,visible,status) VALUES
 (0,'MENU','故障报修','/repair-requests','repair/RepairRequestView','repair:list',50,1,'ENABLED'),
 (0,'BUTTON','创建报修','',NULL,'repair:create',51,0,'ENABLED'),
 (0,'BUTTON','查看报修','',NULL,'repair:view',52,0,'ENABLED'),
 (0,'BUTTON','取消报修','',NULL,'repair:cancel',53,0,'ENABLED'),
 (0,'MENU','维修工单','/work-orders','workorder/WorkOrderListView','workorder:list',60,1,'ENABLED'),
 (0,'BUTTON','查看工单','',NULL,'workorder:view',61,0,'ENABLED'),
 (0,'BUTTON','人工派单','',NULL,'workorder:assign',62,0,'ENABLED'),
 (0,'BUTTON','维修处理','',NULL,'workorder:process',63,0,'ENABLED'),
 (0,'BUTTON','维修验收','',NULL,'workorder:accept',64,0,'ENABLED'),
 (0,'BUTTON','取消工单','',NULL,'workorder:cancel',65,0,'ENABLED')
ON DUPLICATE KEY UPDATE name=VALUES(name),path=VALUES(path),component=VALUES(component),sort_no=VALUES(sort_no),visible=VALUES(visible),status=VALUES(status);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='ADMIN' AND (m.permission_code LIKE 'repair:%' OR m.permission_code LIKE 'workorder:%');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='MAINTENANCE_SUPERVISOR' AND m.permission_code IN
 ('repair:list','repair:create','repair:view','workorder:list','workorder:view','workorder:assign','workorder:accept','workorder:cancel');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='ENGINEER' AND m.permission_code IN
 ('repair:list','repair:create','repair:view','workorder:list','workorder:view','workorder:process');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='REPORTER' AND m.permission_code IN
 ('repair:list','repair:create','repair:view','repair:cancel','workorder:list','workorder:view','workorder:accept');
