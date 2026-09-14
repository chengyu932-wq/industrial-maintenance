USE industrial_maintenance;
SET NAMES utf8mb4;

INSERT INTO sys_menu(parent_id,menu_type,name,path,component,permission_code,sort_no,visible,status) VALUES
 (0,'MENU','SLA管理','/sla','sla/SlaManagementView','sla:rule:list',80,1,'ENABLED'),
 (0,'BUTTON','修改SLA规则','',NULL,'sla:rule:update',81,0,'ENABLED'),
 (0,'BUTTON','查看SLA事件','',NULL,'sla:event:list',82,0,'ENABLED'),
 (0,'BUTTON','处理SLA事件','',NULL,'sla:event:handle',83,0,'ENABLED'),
 (0,'MENU','消息中心','/notifications','notification/NotificationView','notification:list',90,1,'ENABLED'),
 (0,'BUTTON','消息标记已读','',NULL,'notification:read',91,0,'ENABLED')
ON DUPLICATE KEY UPDATE name=VALUES(name),path=VALUES(path),component=VALUES(component),sort_no=VALUES(sort_no),visible=VALUES(visible),status=VALUES(status);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='ADMIN' AND (m.permission_code LIKE 'sla:%' OR m.permission_code LIKE 'notification:%');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='MAINTENANCE_SUPERVISOR' AND (m.permission_code LIKE 'sla:%' OR m.permission_code LIKE 'notification:%');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code IN ('ENGINEER','WAREHOUSE_ADMIN','REPORTER') AND m.permission_code IN ('notification:list','notification:read');
