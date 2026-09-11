USE industrial_maintenance;
SET NAMES utf8mb4;

-- 第4阶段增量数据脚本：组织与设备菜单/按钮权限。可安全重复执行。
INSERT INTO sys_menu (parent_id,menu_type,name,path,component,permission_code,sort_no,visible,status)
VALUES
 (0,'MENU','组织结构','/organization','organization/OrganizationView','organization:list',30,1,'ENABLED'),
 (0,'BUTTON','新增组织','',NULL,'organization:add',31,0,'ENABLED'),
 (0,'BUTTON','修改组织','',NULL,'organization:update',32,0,'ENABLED'),
 (0,'BUTTON','删除组织','',NULL,'organization:delete',33,0,'ENABLED'),
 (0,'MENU','设备台账','/equipment','equipment/EquipmentListView','equipment:list',40,1,'ENABLED'),
 (0,'BUTTON','查看设备','',NULL,'equipment:view',41,0,'ENABLED'),
 (0,'BUTTON','新增设备','',NULL,'equipment:add',42,0,'ENABLED'),
 (0,'BUTTON','修改设备','',NULL,'equipment:update',43,0,'ENABLED'),
 (0,'BUTTON','设备状态','',NULL,'equipment:status',44,0,'ENABLED'),
 (0,'BUTTON','设备报废','',NULL,'equipment:scrap',45,0,'ENABLED'),
 (0,'BUTTON','设备导出','',NULL,'equipment:export',46,0,'ENABLED'),
 (0,'BUTTON','设备导入','',NULL,'equipment:import',47,0,'ENABLED'),
 (0,'BUTTON','设备类型管理','',NULL,'equipment:type:manage',48,0,'ENABLED')
ON DUPLICATE KEY UPDATE name=VALUES(name),path=VALUES(path),component=VALUES(component),sort_no=VALUES(sort_no),visible=VALUES(visible),status=VALUES(status);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='ADMIN' AND (m.permission_code LIKE 'organization:%' OR m.permission_code LIKE 'equipment:%');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='MAINTENANCE_SUPERVISOR' AND m.permission_code IN
 ('organization:list','equipment:list','equipment:view','equipment:add','equipment:update','equipment:status','equipment:scrap','equipment:export','equipment:import');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code IN ('ENGINEER','WAREHOUSE_ADMIN','REPORTER') AND m.permission_code IN ('equipment:list','equipment:view');
