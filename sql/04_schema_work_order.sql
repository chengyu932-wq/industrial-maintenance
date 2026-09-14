USE industrial_maintenance;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS mnt_repair_request (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    request_no VARCHAR(50) NOT NULL COMMENT '报修业务号',
    equipment_id BIGINT NOT NULL COMMENT '设备 ID（逻辑关联 eqp_equipment.id）',
    reporter_id BIGINT NOT NULL COMMENT '报修人 ID（逻辑关联 sys_user.id）',
    source VARCHAR(20) NOT NULL COMMENT 'PC/QR/ENGINEER',
    priority VARCHAR(20) NOT NULL COMMENT 'URGENT/IMPORTANT/NORMAL',
    fault_description TEXT NOT NULL COMMENT '故障现象',
    reported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报修时间',
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/CONVERTED/CANCELLED',
    cancelled_at DATETIME NULL COMMENT '取消时间',
    cancel_reason VARCHAR(500) NULL COMMENT '取消原因',
    sla_rule_id BIGINT NULL COMMENT '创建时绑定的 SLA 规则 ID（逻辑关联 mnt_sla_rule.id）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_repair_request_no (request_no),
    KEY idx_repair_equipment (equipment_id),
    KEY idx_repair_reporter_time (reporter_id, reported_at),
    KEY idx_repair_status_time (status, reported_at),
    CONSTRAINT chk_repair_source CHECK (source IN ('PC', 'QR', 'ENGINEER')),
    CONSTRAINT chk_repair_priority CHECK (priority IN ('URGENT', 'IMPORTANT', 'NORMAL')),
    CONSTRAINT chk_repair_status CHECK (status IN ('SUBMITTED', 'CONVERTED', 'CANCELLED')),
    CONSTRAINT chk_repair_cancel CHECK (
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancel_reason IS NOT NULL)
        OR status <> 'CANCELLED'
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='故障报修单';

CREATE TABLE IF NOT EXISTS mnt_work_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    work_order_no VARCHAR(50) NOT NULL COMMENT '工单业务号',
    work_order_type VARCHAR(20) NOT NULL COMMENT 'REPAIR/MAINTENANCE',
    repair_request_id BIGINT NULL COMMENT '来源报修 ID（逻辑关联 mnt_repair_request.id）',
    pm_plan_id BIGINT NULL COMMENT '来源保养计划 ID（逻辑关联 pm_plan.id）',
    equipment_id BIGINT NOT NULL COMMENT '设备 ID（逻辑关联 eqp_equipment.id）',
    priority VARCHAR(20) NOT NULL COMMENT 'URGENT/IMPORTANT/NORMAL',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_ASSIGN' COMMENT '工单状态',
    assigned_engineer_id BIGINT NULL COMMENT '当前工程师 ID（逻辑关联 sys_user.id）',
    assigned_team_id BIGINT NULL COMMENT '当前班组 ID（逻辑关联 org_team.id）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间/SLA 起点',
    assigned_at DATETIME NULL COMMENT '派单时间',
    accepted_at DATETIME NULL COMMENT '工程师接单时间/SLA 响应时间',
    started_at DATETIME NULL COMMENT '开始维修时间',
    submitted_at DATETIME NULL COMMENT '提交验收时间',
    completed_at DATETIME NULL COMMENT '完成时间/SLA 解决时间',
    cancelled_at DATETIME NULL COMMENT '取消时间',
    cancel_reason VARCHAR(500) NULL COMMENT '取消原因',
    sla_response_deadline DATETIME NULL COMMENT '响应截止时间',
    sla_resolve_deadline DATETIME NULL COMMENT '解决截止时间',
    acceptance_return_count INT NOT NULL DEFAULT 0 COMMENT '验收退回次数',
    created_by BIGINT NOT NULL COMMENT '创建人 ID（逻辑关联 sys_user.id）',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_order_no (work_order_no),
    UNIQUE KEY uk_work_order_repair_request (repair_request_id),
    KEY idx_work_order_status_created (status, created_at),
    KEY idx_work_order_engineer_status (assigned_engineer_id, status),
    KEY idx_work_order_team_status (assigned_team_id, status),
    KEY idx_work_order_equipment_status (equipment_id, status),
    KEY idx_work_order_completed (completed_at),
    KEY idx_work_order_pm_plan (pm_plan_id),
    KEY idx_work_order_sla_rule (sla_rule_id),
    CONSTRAINT chk_work_order_type CHECK (work_order_type IN ('REPAIR', 'MAINTENANCE')),
    CONSTRAINT chk_work_order_priority CHECK (priority IN ('URGENT', 'IMPORTANT', 'NORMAL')),
    CONSTRAINT chk_work_order_status CHECK (
        status IN ('PENDING_ASSIGN', 'ASSIGNED', 'PROCESSING', 'SUSPENDED', 'PENDING_ACCEPT', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT chk_work_order_source CHECK (
        (work_order_type = 'REPAIR' AND repair_request_id IS NOT NULL AND pm_plan_id IS NULL)
        OR (work_order_type = 'MAINTENANCE' AND repair_request_id IS NULL AND pm_plan_id IS NOT NULL)
    ),
    CONSTRAINT chk_work_order_return_count CHECK (acceptance_return_count >= 0),
    CONSTRAINT chk_work_order_cancel CHECK (
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancel_reason IS NOT NULL)
        OR status <> 'CANCELLED'
    ),
    CONSTRAINT chk_work_order_complete CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR status <> 'COMPLETED'
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维修和保养统一工单';

CREATE TABLE IF NOT EXISTS mnt_work_order_flow (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    work_order_id BIGINT NOT NULL COMMENT '工单 ID（逻辑关联 mnt_work_order.id）',
    from_status VARCHAR(30) NULL COMMENT '原状态，初始创建可为空',
    to_status VARCHAR(30) NOT NULL COMMENT '新状态',
    action VARCHAR(50) NOT NULL COMMENT 'CREATE/ASSIGN/START/SUSPEND/RESUME/SUBMIT/ACCEPT/RETURN/CANCEL',
    operator_id BIGINT NOT NULL COMMENT '操作人 ID（逻辑关联 sys_user.id）',
    remark VARCHAR(500) NULL COMMENT '流转说明、挂起原因或验收意见',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_work_order_flow_order_time (work_order_id, created_at),
    KEY idx_work_order_flow_action_time (action, created_at),
    CONSTRAINT chk_work_order_flow_from CHECK (
        from_status IS NULL OR from_status IN ('PENDING_ASSIGN', 'ASSIGNED', 'PROCESSING', 'SUSPENDED', 'PENDING_ACCEPT', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT chk_work_order_flow_to CHECK (
        to_status IN ('PENDING_ASSIGN', 'ASSIGNED', 'PROCESSING', 'SUSPENDED', 'PENDING_ACCEPT', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT chk_work_order_flow_action CHECK (
        action IN ('CREATE', 'ASSIGN', 'REASSIGN', 'ACCEPT', 'START', 'SUSPEND', 'RESUME', 'SUBMIT', 'ACCEPT_PASS', 'ACCEPT_RETURN', 'CANCEL')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单状态流转日志';

CREATE TABLE IF NOT EXISTS mnt_assignment_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    work_order_id BIGINT NOT NULL COMMENT '工单 ID（逻辑关联 mnt_work_order.id）',
    old_engineer_id BIGINT NULL COMMENT '原工程师 ID',
    new_engineer_id BIGINT NOT NULL COMMENT '新工程师 ID',
    old_team_id BIGINT NULL COMMENT '原班组 ID',
    new_team_id BIGINT NULL COMMENT '新班组 ID',
    assignment_type VARCHAR(20) NOT NULL COMMENT 'MANUAL/RECOMMENDED/REASSIGN',
    score_snapshot JSON NULL COMMENT '智能推荐采纳时的可解释评分快照',
    reason VARCHAR(500) NULL COMMENT '派单或改派原因',
    operator_id BIGINT NOT NULL COMMENT '操作人 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_assignment_order_time (work_order_id, created_at),
    KEY idx_assignment_engineer_time (new_engineer_id, created_at),
    CONSTRAINT chk_assignment_type CHECK (assignment_type IN ('MANUAL', 'RECOMMENDED', 'REASSIGN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='派单和改派记录';

CREATE TABLE IF NOT EXISTS mnt_repair_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    work_order_id BIGINT NOT NULL COMMENT '维修工单 ID（逻辑关联 mnt_work_order.id）',
    inspection_process TEXT NULL COMMENT '故障排查过程',
    root_cause TEXT NULL COMMENT '根因分析',
    repair_action TEXT NULL COMMENT '处理措施',
    repair_result TEXT NULL COMMENT '维修结果',
    labor_hours DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '维修工时',
    downtime_minutes INT NOT NULL DEFAULT 0 COMMENT '故障停机分钟数',
    repairable TINYINT NULL COMMENT '是否修复：0 否，1 是',
    created_by BIGINT NOT NULL COMMENT '工程师 ID（逻辑关联 sys_user.id）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_repair_record_order (work_order_id),
    CONSTRAINT chk_repair_record_labor CHECK (labor_hours >= 0),
    CONSTRAINT chk_repair_record_downtime CHECK (downtime_minutes >= 0),
    CONSTRAINT chk_repair_record_repairable CHECK (repairable IS NULL OR repairable IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维修过程与结果记录';

CREATE TABLE IF NOT EXISTS mnt_work_order_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    work_order_id BIGINT NOT NULL COMMENT '工单 ID（逻辑关联 mnt_work_order.id）',
    attachment_type VARCHAR(20) NOT NULL COMMENT 'FAULT/REPAIR/ACCEPTANCE',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '本地相对路径',
    uploaded_by BIGINT NOT NULL COMMENT '上传人 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_work_order_attachment_order (work_order_id),
    CONSTRAINT chk_work_order_attachment_type CHECK (attachment_type IN ('FAULT', 'REPAIR', 'ACCEPTANCE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单附件元数据';

CREATE TABLE IF NOT EXISTS mnt_sla_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    priority VARCHAR(20) NOT NULL COMMENT 'URGENT/IMPORTANT/NORMAL',
    response_minutes INT NOT NULL COMMENT '响应时限（分钟）',
    resolve_minutes INT NOT NULL COMMENT '解决时限（分钟）',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0 否，1 是',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sla_rule_priority (priority),
    CONSTRAINT chk_sla_rule_priority CHECK (priority IN ('URGENT', 'IMPORTANT', 'NORMAL')),
    CONSTRAINT chk_sla_rule_minutes CHECK (response_minutes > 0 AND resolve_minutes > 0),
    CONSTRAINT chk_sla_rule_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='故障等级 SLA 配置';

CREATE TABLE IF NOT EXISTS mnt_sla_event (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    work_order_id BIGINT NOT NULL COMMENT '工单 ID（逻辑关联 mnt_work_order.id）',
    event_type VARCHAR(30) NOT NULL COMMENT 'RESPONSE_TIMEOUT/RESOLVE_TIMEOUT/ESCALATION',
    rule_id BIGINT NOT NULL COMMENT 'SLA 规则 ID（逻辑关联 mnt_sla_rule.id）',
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    escalated_to_user_id BIGINT NULL COMMENT '升级接收人 ID',
    message_id BIGINT NULL COMMENT '站内消息 ID（逻辑关联 msg_notification.id）',
    handled TINYINT NOT NULL DEFAULT 0 COMMENT '是否处理',
    handled_at DATETIME NULL COMMENT '处理时间',
    remark VARCHAR(500) NULL COMMENT '说明',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sla_event_order_type (work_order_id, event_type),
    KEY idx_sla_event_handled_time (handled, occurred_at),
    CONSTRAINT chk_sla_event_type CHECK (event_type IN ('RESPONSE_TIMEOUT', 'RESOLVE_TIMEOUT', 'ESCALATION')),
    CONSTRAINT chk_sla_event_handled CHECK (handled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SLA 超时与升级事件';
