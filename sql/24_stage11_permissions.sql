USE industrial_maintenance;
SET NAMES utf8mb4;

INSERT INTO sys_menu(parent_id,menu_type,name,path,component,permission_code,sort_no,visible,status) VALUES
 (0,'MENU','统计分析','/statistics','statistics/StatisticsView','statistics:view',85,1,'ENABLED')
ON DUPLICATE KEY UPDATE name=VALUES(name),path=VALUES(path),component=VALUES(component),sort_no=VALUES(sort_no),visible=VALUES(visible),status=VALUES(status);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code IN ('ADMIN','MAINTENANCE_SUPERVISOR','ENGINEER','WAREHOUSE_ADMIN')
  AND m.permission_code='statistics:view';
