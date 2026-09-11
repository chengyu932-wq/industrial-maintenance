USE industrial_maintenance;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(20) NULL COMMENT '手机号',
    email VARCHAR(100) NULL COMMENT '邮箱',
    job_title VARCHAR(50) NULL COMMENT '岗位',
    team_id BIGINT NULL COMMENT '所属班组 ID（逻辑关联 org_team.id）',
    workshop_id BIGINT NULL COMMENT '主要负责车间 ID（逻辑关联 org_workshop.id）',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    failed_login_count INT NOT NULL DEFAULT 0 COMMENT '持久化登录失败次数',
    last_login_at DATETIME NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_team (team_id),
    KEY idx_sys_user_workshop (workshop_id),
    CONSTRAINT chk_sys_user_status CHECK (status IN ('ENABLED', 'DISABLED')),
    CONSTRAINT chk_sys_user_failed_login CHECK (failed_login_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    data_scope VARCHAR(30) NOT NULL COMMENT '数据范围编码',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    remark VARCHAR(255) NULL COMMENT '备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (role_code),
    CONSTRAINT chk_sys_role_status CHECK (status IN ('ENABLED', 'DISABLED')),
    CONSTRAINT chk_sys_role_data_scope CHECK (
        data_scope IN ('ALL', 'WORKSHOP_OR_TEAM', 'SELF_OR_TEAM', 'WAREHOUSE', 'SELF')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户 ID（逻辑关联 sys_user.id）',
    role_id BIGINT NOT NULL COMMENT '角色 ID（逻辑关联 sys_role.id）',
    PRIMARY KEY (user_id, role_id),
    KEY idx_sys_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父节点 ID，0 表示根节点',
    menu_type VARCHAR(20) NOT NULL COMMENT 'DIR/MENU/BUTTON',
    name VARCHAR(50) NOT NULL COMMENT '菜单或按钮名称',
    path VARCHAR(200) NULL COMMENT '前端路由',
    component VARCHAR(200) NULL COMMENT '前端组件路径',
    permission_code VARCHAR(100) NULL COMMENT '按钮/API 权限标识',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示：0 否，1 是',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_menu_permission (permission_code),
    KEY idx_sys_menu_parent_sort (parent_id, sort_no),
    CONSTRAINT chk_sys_menu_type CHECK (menu_type IN ('DIR', 'MENU', 'BUTTON')),
    CONSTRAINT chk_sys_menu_visible CHECK (visible IN (0, 1)),
    CONSTRAINT chk_sys_menu_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单和按钮权限表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色 ID（逻辑关联 sys_role.id）',
    menu_id BIGINT NOT NULL COMMENT '菜单 ID（逻辑关联 sys_menu.id）',
    PRIMARY KEY (role_id, menu_id),
    KEY idx_sys_role_menu_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NULL COMMENT '操作人 ID（逻辑关联 sys_user.id）',
    module VARCHAR(50) NOT NULL COMMENT '业务模块',
    operation VARCHAR(100) NOT NULL COMMENT '操作类型',
    request_uri VARCHAR(255) NOT NULL COMMENT '请求接口',
    http_method VARCHAR(10) NOT NULL COMMENT 'HTTP 方法',
    ip_address VARCHAR(64) NULL COMMENT '客户端 IP',
    request_summary TEXT NULL COMMENT '脱敏后的参数摘要',
    result VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAIL',
    error_message VARCHAR(500) NULL COMMENT '错误摘要',
    duration_ms BIGINT NULL COMMENT '执行耗时（毫秒）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    PRIMARY KEY (id),
    KEY idx_operation_user_time (user_id, created_at),
    KEY idx_operation_module_time (module, created_at),
    CONSTRAINT chk_operation_result CHECK (result IN ('SUCCESS', 'FAIL')),
    CONSTRAINT chk_operation_duration CHECK (duration_ms IS NULL OR duration_ms >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作审计日志';
