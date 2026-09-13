USE industrial_maintenance;
SET NAMES utf8mb4;

-- 第8阶段增量权限：预防性维护计划、手动扫描和保养履历。
INSERT INTO sys_menu(parent_id,menu_type,name,path,component,permission_code,sort_no,visible,status) VALUES
 (0,'MENU','预防性维护','/maintenance-plans','maintenance/MaintenancePlanView','maintenance:plan:list',70,1,'ENABLED'),
 (0,'BUTTON','查看保养计划','',NULL,'maintenance:plan:view',71,0,'ENABLED'),
 (0,'BUTTON','新增保养计划','',NULL,'maintenance:plan:add',72,0,'ENABLED'),
 (0,'BUTTON','修改保养计划','',NULL,'maintenance:plan:update',73,0,'ENABLED'),
 (0,'BUTTON','启停保养计划','',NULL,'maintenance:plan:status',74,0,'ENABLED'),
 (0,'BUTTON','扫描到期计划','',NULL,'maintenance:scan',75,0,'ENABLED'),
 (0,'BUTTON','查看保养履历','',NULL,'maintenance:history:view',76,0,'ENABLED')
ON DUPLICATE KEY UPDATE name=VALUES(name),path=VALUES(path),component=VALUES(component),sort_no=VALUES(sort_no),visible=VALUES(visible),status=VALUES(status);

UPDATE sys_menu SET name='工单中心' WHERE permission_code='workorder:list';

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='ADMIN' AND m.permission_code LIKE 'maintenance:%';

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='MAINTENANCE_SUPERVISOR' AND m.permission_code IN
 ('maintenance:plan:list','maintenance:plan:view','maintenance:plan:add','maintenance:plan:update','maintenance:plan:status','maintenance:scan','maintenance:history:view');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='ENGINEER' AND m.permission_code='maintenance:history:view';
