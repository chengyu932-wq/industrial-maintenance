USE industrial_maintenance;
SET NAMES utf8mb4;

-- 第7阶段增量权限：仓库、备件、库存、流水与工单备件领退。
INSERT INTO sys_menu(parent_id,menu_type,name,path,component,permission_code,sort_no,visible,status) VALUES
 (0,'MENU','仓库管理','/warehouses','inventory/WarehouseView','warehouse:list',70,1,'ENABLED'),
 (0,'BUTTON','维护仓库','',NULL,'warehouse:manage',71,0,'ENABLED'),
 (0,'BUTTON','仓库授权','',NULL,'warehouse:authorize',72,0,'ENABLED'),
 (0,'MENU','备件管理','/spare-parts','inventory/SparePartView','spare:list',80,1,'ENABLED'),
 (0,'BUTTON','维护备件','',NULL,'spare:manage',81,0,'ENABLED'),
 (0,'MENU','库存管理','/inventory/stocks','inventory/InventoryView','inventory:stock:list',90,1,'ENABLED'),
 (0,'BUTTON','备件入库','',NULL,'inventory:inbound',91,0,'ENABLED'),
 (0,'BUTTON','普通出库','',NULL,'inventory:outbound',92,0,'ENABLED'),
 (0,'BUTTON','库存调拨','',NULL,'inventory:transfer',93,0,'ENABLED'),
 (0,'BUTTON','库存盘点','',NULL,'inventory:stocktake',94,0,'ENABLED'),
 (0,'BUTTON','备件报废','',NULL,'inventory:scrap',95,0,'ENABLED'),
 (0,'BUTTON','工单备件领用','',NULL,'inventory:issue',96,0,'ENABLED'),
 (0,'BUTTON','工单备件退库','',NULL,'inventory:return',97,0,'ENABLED'),
 (0,'MENU','库存流水','/inventory/transactions','inventory/TransactionView','inventory:transaction:list',100,1,'ENABLED'),
 (0,'BUTTON','库存预警查询','',NULL,'inventory:warning:list',101,0,'ENABLED')
ON DUPLICATE KEY UPDATE name=VALUES(name),path=VALUES(path),component=VALUES(component),sort_no=VALUES(sort_no),visible=VALUES(visible),status=VALUES(status);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='ADMIN' AND (m.permission_code LIKE 'warehouse:%' OR m.permission_code LIKE 'spare:%' OR m.permission_code LIKE 'inventory:%');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='WAREHOUSE_ADMIN' AND m.permission_code IN
 ('warehouse:list','warehouse:manage','spare:list','spare:manage','inventory:stock:list','inventory:inbound','inventory:outbound','inventory:transfer','inventory:stocktake','inventory:scrap','inventory:transaction:list','inventory:warning:list');

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='ENGINEER' AND m.permission_code IN ('inventory:issue','inventory:return');
