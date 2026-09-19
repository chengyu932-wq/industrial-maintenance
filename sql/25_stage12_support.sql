USE industrial_maintenance;

INSERT INTO sys_menu(parent_id,menu_type,name,path,component,permission_code,sort_no,visible,status)
VALUES
(0,'MENU','系统用户','/system/users','system/UserListView','system:user:list',10,1,'ENABLED'),
(0,'BUTTON','新增用户',NULL,NULL,'system:user:add',11,0,'ENABLED'),
(0,'BUTTON','修改用户',NULL,NULL,'system:user:update',12,0,'ENABLED'),
(0,'BUTTON','用户批量导入',NULL,NULL,'system:user:import',13,0,'ENABLED')
ON DUPLICATE KEY UPDATE name=VALUES(name),path=VALUES(path),component=VALUES(component),visible=VALUES(visible),status=VALUES(status);

INSERT INTO sys_menu(parent_id,menu_type,name,path,component,permission_code,sort_no,visible,status)
SELECT 0,'BUTTON','查看操作日志',NULL,NULL,'system:operation-log:list',990,0,'ENABLED'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission_code='system:operation-log:list');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.permission_code='system:operation-log:list'
WHERE r.role_code IN ('ADMIN','MAINTENANCE_SUPERVISOR');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='ADMIN' AND m.permission_code IN ('system:user:list','system:user:add','system:user:update','system:user:import');
