USE industrial_maintenance;
SET NAMES utf8mb4;

-- 仅初始化跨环境都需要的基础角色，不写入默认管理员密码。
INSERT INTO sys_role (role_code, role_name, data_scope, status, remark)
VALUES
    ('ADMIN', '管理员', 'ALL', 'ENABLED', '管理全部系统与业务数据'),
    ('MAINTENANCE_SUPERVISOR', '运维主管', 'WORKSHOP_OR_TEAM', 'ENABLED', '负责车间或班组范围'),
    ('ENGINEER', '工程师', 'SELF_OR_TEAM', 'ENABLED', '本人或所属班组授权工单'),
    ('WAREHOUSE_ADMIN', '仓库管理员', 'WAREHOUSE', 'ENABLED', '授权仓库范围'),
    ('REPORTER', '报修人', 'SELF', 'ENABLED', '本人报修及关联工单')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    data_scope = VALUES(data_scope),
    status = VALUES(status),
    remark = VALUES(remark);

-- 项目开发默认值，可在系统配置中调整；不是学校或任务书规定的固定时长。
INSERT INTO mnt_sla_rule (priority, response_minutes, resolve_minutes, enabled)
VALUES
    ('URGENT', 30, 240, 1),
    ('IMPORTANT', 120, 480, 1),
    ('NORMAL', 240, 1440, 1)
ON DUPLICATE KEY UPDATE
    response_minutes = VALUES(response_minutes),
    resolve_minutes = VALUES(resolve_minutes),
    enabled = VALUES(enabled);
