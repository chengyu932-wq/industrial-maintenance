USE industrial_maintenance;
SET NAMES utf8mb4;

INSERT INTO sys_menu(parent_id,menu_type,name,path,component,permission_code,sort_no,visible,status) VALUES
 (0,'MENU','故障知识库','/knowledge','knowledge/KnowledgeView','knowledge:list',75,1,'ENABLED'),
 (0,'BUTTON','查看知识详情','',NULL,'knowledge:view',76,0,'ENABLED'),
 (0,'BUTTON','创建知识草稿','',NULL,'knowledge:create',77,0,'ENABLED'),
 (0,'BUTTON','编辑知识草稿','',NULL,'knowledge:update',78,0,'ENABLED'),
 (0,'BUTTON','提交知识审核','',NULL,'knowledge:submit',79,0,'ENABLED'),
 (0,'BUTTON','审核故障知识','',NULL,'knowledge:audit',80,0,'ENABLED'),
 (0,'BUTTON','相似工单推荐','',NULL,'workorder:similar',81,0,'ENABLED')
ON DUPLICATE KEY UPDATE name=VALUES(name),path=VALUES(path),component=VALUES(component),sort_no=VALUES(sort_no),visible=VALUES(visible),status=VALUES(status);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='ADMIN' AND (m.permission_code LIKE 'knowledge:%' OR m.permission_code='workorder:similar');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='MAINTENANCE_SUPERVISOR' AND (m.permission_code LIKE 'knowledge:%' OR m.permission_code='workorder:similar');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='ENGINEER' AND m.permission_code IN ('knowledge:list','knowledge:view','knowledge:create','knowledge:update','knowledge:submit','workorder:similar');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='REPORTER' AND m.permission_code IN ('knowledge:list','knowledge:view','workorder:similar');
