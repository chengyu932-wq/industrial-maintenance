USE industrial_maintenance;
ALTER TABLE mnt_work_order ADD COLUMN pm_plan_no_snapshot VARCHAR(50) NULL, ADD COLUMN pm_plan_name_snapshot VARCHAR(100) NULL;
UPDATE mnt_work_order wo JOIN pm_plan p ON p.id=wo.pm_plan_id SET wo.pm_plan_no_snapshot=p.plan_no,wo.pm_plan_name_snapshot=p.plan_name WHERE wo.work_order_type='MAINTENANCE';
INSERT INTO sys_menu(parent_id,menu_type,name,permission_code,sort_no,visible,status) VALUES(0,'BUTTON','确认报修','repair:confirm',65,0,'ENABLED') ON DUPLICATE KEY UPDATE name=VALUES(name);
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.permission_code='repair:confirm' WHERE r.role_code IN ('ADMIN','MAINTENANCE_SUPERVISOR');
CREATE TABLE IF NOT EXISTS mnt_repair_request_attachment (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, repair_request_id BIGINT NOT NULL,
 file_name VARCHAR(255) NOT NULL, file_path VARCHAR(500) NOT NULL, uploaded_by BIGINT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_repair_attachment_request(repair_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE sys_user ADD COLUMN deleted_at DATETIME NULL;
INSERT INTO sys_menu(parent_id,menu_type,name,permission_code,sort_no,visible,status) VALUES(0,'BUTTON','删除用户','system:user:delete',66,0,'ENABLED') ON DUPLICATE KEY UPDATE name=VALUES(name);
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.permission_code='system:user:delete' WHERE r.role_code='ADMIN';

ALTER TABLE sys_user ADD COLUMN password_version BIGINT NOT NULL DEFAULT 0;
