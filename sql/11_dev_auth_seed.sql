USE industrial_maintenance;
SET NAMES utf8mb4;

-- 仅用于本地开发与答辩演示。统一初始密码为 DevOnly@123，必须在非开发环境删除或修改。
SET @dev_password_hash = '$2a$10$WBfN3eOHsc4FnJbeloIeq.XVYauRoVrliSjiy9mD9P4.EVplGzcMq';

INSERT INTO sys_menu (parent_id, menu_type, name, path, component, permission_code, sort_no, visible, status)
VALUES
    (0, 'MENU', '系统用户', '/system/users', 'system/UserListView', 'system:user:list', 10, 1, 'ENABLED'),
    (0, 'BUTTON', '新增用户', NULL, NULL, 'system:user:add', 11, 0, 'ENABLED'),
    (0, 'BUTTON', '修改用户', NULL, NULL, 'system:user:update', 12, 0, 'ENABLED'),
    (0, 'BUTTON', '删除用户', NULL, NULL, 'system:user:delete', 13, 0, 'ENABLED'),
    (0, 'MENU', '运维主管工作台', '/', 'HomeView', 'workspace:supervisor:view', 20, 1, 'ENABLED'),
    (0, 'MENU', '工程师工作台', '/', 'HomeView', 'workspace:engineer:view', 21, 1, 'ENABLED'),
    (0, 'MENU', '仓库工作台', '/', 'HomeView', 'workspace:warehouse:view', 22, 1, 'ENABLED'),
    (0, 'MENU', '报修工作台', '/', 'HomeView', 'workspace:reporter:view', 23, 1, 'ENABLED')
ON DUPLICATE KEY UPDATE name=VALUES(name), path=VALUES(path), component=VALUES(component),
    sort_no=VALUES(sort_no), visible=VALUES(visible), status=VALUES(status);

INSERT INTO sys_user (username, password_hash, real_name, job_title, status)
VALUES
    ('admin', @dev_password_hash, '开发管理员', '系统管理员', 'ENABLED'),
    ('supervisor', @dev_password_hash, '开发主管', '运维主管', 'ENABLED'),
    ('engineer', @dev_password_hash, '开发工程师', '维修工程师', 'ENABLED'),
    ('warehouse', @dev_password_hash, '开发仓管员', '仓库管理员', 'ENABLED'),
    ('reporter', @dev_password_hash, '开发报修人', '报修人', 'ENABLED'),
    ('disabled_user', @dev_password_hash, '禁用测试用户', '测试账号', 'DISABLED')
ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash), real_name=VALUES(real_name),
    job_title=VALUES(job_title), status=VALUES(status);

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u JOIN sys_role r
WHERE (u.username='admin' AND r.role_code='ADMIN')
   OR (u.username='supervisor' AND r.role_code='MAINTENANCE_SUPERVISOR')
   OR (u.username='engineer' AND r.role_code='ENGINEER')
   OR (u.username='warehouse' AND r.role_code='WAREHOUSE_ADMIN')
   OR (u.username IN ('reporter','disabled_user') AND r.role_code='REPORTER');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m WHERE r.role_code='ADMIN';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON
    (r.role_code='MAINTENANCE_SUPERVISOR' AND m.permission_code='workspace:supervisor:view') OR
    (r.role_code='ENGINEER' AND m.permission_code='workspace:engineer:view') OR
    (r.role_code='WAREHOUSE_ADMIN' AND m.permission_code='workspace:warehouse:view') OR
    (r.role_code='REPORTER' AND m.permission_code='workspace:reporter:view');
