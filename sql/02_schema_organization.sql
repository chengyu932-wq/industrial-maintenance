USE industrial_maintenance;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS org_workshop (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    workshop_no VARCHAR(50) NOT NULL COMMENT '车间编号',
    workshop_name VARCHAR(100) NOT NULL COMMENT '车间名称',
    manager_id BIGINT NULL COMMENT '负责人 ID（逻辑关联 sys_user.id）',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_workshop_no (workshop_no),
    KEY idx_org_workshop_manager (manager_id),
    CONSTRAINT chk_org_workshop_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车间表';

CREATE TABLE IF NOT EXISTS org_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    workshop_id BIGINT NOT NULL COMMENT '车间 ID（逻辑关联 org_workshop.id）',
    line_no VARCHAR(50) NOT NULL COMMENT '产线编号',
    line_name VARCHAR(100) NOT NULL COMMENT '产线名称',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_line_workshop_no (workshop_id, line_no),
    KEY idx_org_line_workshop_status (workshop_id, status),
    CONSTRAINT chk_org_line_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产线表';

CREATE TABLE IF NOT EXISTS org_station (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    line_id BIGINT NOT NULL COMMENT '产线 ID（逻辑关联 org_line.id）',
    station_no VARCHAR(50) NOT NULL COMMENT '工位编号',
    station_name VARCHAR(100) NOT NULL COMMENT '工位名称',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_station_line_no (line_id, station_no),
    KEY idx_org_station_line_status (line_id, status),
    CONSTRAINT chk_org_station_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工位表';

CREATE TABLE IF NOT EXISTS org_team (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    team_no VARCHAR(50) NOT NULL COMMENT '班组编号',
    team_name VARCHAR(100) NOT NULL COMMENT '班组名称',
    workshop_id BIGINT NOT NULL COMMENT '所属车间 ID（逻辑关联 org_workshop.id）',
    leader_id BIGINT NULL COMMENT '班组长 ID（逻辑关联 sys_user.id）',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_team_no (team_no),
    KEY idx_org_team_workshop_status (workshop_id, status),
    KEY idx_org_team_leader (leader_id),
    CONSTRAINT chk_org_team_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运维班组表';

CREATE TABLE IF NOT EXISTS org_skill (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    skill_code VARCHAR(50) NOT NULL COMMENT '技能编码',
    skill_name VARCHAR(100) NOT NULL COMMENT '技能名称',
    description VARCHAR(500) NULL COMMENT '技能说明',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_skill_code (skill_code),
    CONSTRAINT chk_org_skill_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工程师技能标签表';

CREATE TABLE IF NOT EXISTS org_user_skill (
    user_id BIGINT NOT NULL COMMENT '用户 ID（逻辑关联 sys_user.id）',
    skill_id BIGINT NOT NULL COMMENT '技能 ID（逻辑关联 org_skill.id）',
    skill_level INT NOT NULL DEFAULT 1 COMMENT '技能等级 1-5',
    PRIMARY KEY (user_id, skill_id),
    KEY idx_org_user_skill_skill (skill_id),
    CONSTRAINT chk_org_user_skill_level CHECK (skill_level BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户技能关联表';
