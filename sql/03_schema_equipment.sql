USE industrial_maintenance;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS eqp_type (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    type_code VARCHAR(50) NOT NULL COMMENT '设备类型编码',
    type_name VARCHAR(100) NOT NULL COMMENT '设备类型名称',
    description VARCHAR(500) NULL COMMENT '说明',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    PRIMARY KEY (id),
    UNIQUE KEY uk_eqp_type_code (type_code),
    CONSTRAINT chk_eqp_type_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备类型表';

CREATE TABLE IF NOT EXISTS eqp_equipment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    equipment_no VARCHAR(50) NOT NULL COMMENT '设备编号',
    equipment_name VARCHAR(100) NOT NULL COMMENT '设备名称',
    type_id BIGINT NOT NULL COMMENT '设备类型 ID（逻辑关联 eqp_type.id）',
    model VARCHAR(100) NULL COMMENT '型号',
    manufacturer VARCHAR(100) NULL COMMENT '厂商',
    specifications TEXT NULL COMMENT '规格参数',
    manufacture_date DATE NULL COMMENT '出厂日期',
    commissioning_date DATE NULL COMMENT '启用日期',
    responsible_user_id BIGINT NULL COMMENT '责任人 ID（逻辑关联 sys_user.id）',
    responsible_team_id BIGINT NULL COMMENT '负责班组 ID（逻辑关联 org_team.id）',
    station_id BIGINT NOT NULL COMMENT '工位 ID（逻辑关联 org_station.id）',
    warranty_expire_date DATE NULL COMMENT '保修到期日期',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '设备生命周期状态',
    running_hours DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '当前累计运行小时',
    qr_code VARCHAR(255) NOT NULL COMMENT '二维码唯一标识或相对地址',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_eqp_equipment_no (equipment_no),
    UNIQUE KEY uk_eqp_equipment_qr_code (qr_code),
    KEY idx_eqp_equipment_status (status),
    KEY idx_eqp_equipment_station (station_id),
    KEY idx_eqp_equipment_team (responsible_team_id),
    KEY idx_eqp_equipment_user (responsible_user_id),
    CONSTRAINT chk_eqp_equipment_status CHECK (
        status IN ('PENDING', 'RUNNING', 'FAULT', 'REPAIRING', 'STOPPED', 'SCRAPPED')
    ),
    CONSTRAINT chk_eqp_equipment_hours CHECK (running_hours >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备台账表';

CREATE TABLE IF NOT EXISTS eqp_status_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    equipment_id BIGINT NOT NULL COMMENT '设备 ID（逻辑关联 eqp_equipment.id）',
    from_status VARCHAR(20) NULL COMMENT '原状态，初始建档可为空',
    to_status VARCHAR(20) NOT NULL COMMENT '新状态',
    source_type VARCHAR(30) NOT NULL COMMENT 'MANUAL/WORK_ORDER/SCRAP',
    source_id BIGINT NULL COMMENT '来源业务 ID',
    reason VARCHAR(500) NULL COMMENT '变更原因',
    operator_id BIGINT NOT NULL COMMENT '操作人 ID（逻辑关联 sys_user.id）',
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    PRIMARY KEY (id),
    KEY idx_eqp_status_log_equipment_time (equipment_id, changed_at),
    KEY idx_eqp_status_log_source (source_type, source_id),
    CONSTRAINT chk_eqp_status_log_from CHECK (
        from_status IS NULL OR from_status IN ('PENDING', 'RUNNING', 'FAULT', 'REPAIRING', 'STOPPED', 'SCRAPPED')
    ),
    CONSTRAINT chk_eqp_status_log_to CHECK (
        to_status IN ('PENDING', 'RUNNING', 'FAULT', 'REPAIRING', 'STOPPED', 'SCRAPPED')
    ),
    CONSTRAINT chk_eqp_status_log_source CHECK (source_type IN ('MANUAL', 'WORK_ORDER', 'SCRAP'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备状态变更履历';

CREATE TABLE IF NOT EXISTS eqp_runtime_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    equipment_id BIGINT NOT NULL COMMENT '设备 ID（逻辑关联 eqp_equipment.id）',
    record_date DATE NOT NULL COMMENT '运行小时归属日期',
    running_hours_increment DECIMAL(10,2) NOT NULL COMMENT '本次新增运行小时',
    total_running_hours DECIMAL(12,2) NOT NULL COMMENT '记录后的累计小时快照',
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL',
    recorded_by BIGINT NOT NULL COMMENT '记录人 ID（逻辑关联 sys_user.id）',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_runtime_equipment_date (equipment_id, record_date),
    CONSTRAINT chk_runtime_increment CHECK (running_hours_increment >= 0),
    CONSTRAINT chk_runtime_total CHECK (total_running_hours >= 0),
    CONSTRAINT chk_runtime_source CHECK (source = 'MANUAL')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备人工运行小时记录';

CREATE TABLE IF NOT EXISTS eqp_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    equipment_id BIGINT NOT NULL COMMENT '设备 ID（逻辑关联 eqp_equipment.id）',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '本地相对路径',
    file_type VARCHAR(100) NULL COMMENT 'MIME 类型或扩展名',
    uploaded_by BIGINT NOT NULL COMMENT '上传人 ID（逻辑关联 sys_user.id）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_eqp_attachment_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备附件元数据';
