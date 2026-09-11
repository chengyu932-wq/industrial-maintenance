USE industrial_maintenance;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS pm_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_no VARCHAR(50) NOT NULL COMMENT '保养计划编号',
    plan_name VARCHAR(100) NOT NULL COMMENT '计划名称',
    equipment_id BIGINT NOT NULL COMMENT '目标设备 ID（逻辑关联 eqp_equipment.id）',
    cycle_type VARCHAR(20) NOT NULL COMMENT 'WEEK/MONTH/QUARTER',
    cycle_value INT NOT NULL DEFAULT 1 COMMENT '周期倍数',
    next_execute_date DATE NOT NULL COMMENT '下次执行日期',
    running_hour_threshold DECIMAL(12,2) NULL COMMENT '运行小时扩展触发阈值',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    last_generated_at DATETIME NULL COMMENT '最近生成工单时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pm_plan_no (plan_no),
    KEY idx_pm_plan_due (status, next_execute_date),
    KEY idx_pm_plan_equipment (equipment_id),
    CONSTRAINT chk_pm_plan_cycle CHECK (cycle_type IN ('WEEK', 'MONTH', 'QUARTER')),
    CONSTRAINT chk_pm_plan_cycle_value CHECK (cycle_value > 0),
    CONSTRAINT chk_pm_plan_hours CHECK (running_hour_threshold IS NULL OR running_hour_threshold >= 0),
    CONSTRAINT chk_pm_plan_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预防性维护计划';

CREATE TABLE IF NOT EXISTS pm_plan_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT NOT NULL COMMENT '保养计划 ID（逻辑关联 pm_plan.id）',
    item_name VARCHAR(100) NOT NULL COMMENT '检查项目名称',
    standard_description VARCHAR(500) NULL COMMENT '检查标准',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    required TINYINT NOT NULL DEFAULT 1 COMMENT '是否必检：0 否，1 是',
    PRIMARY KEY (id),
    KEY idx_pm_plan_item_plan_sort (plan_id, sort_no),
    CONSTRAINT chk_pm_plan_item_required CHECK (required IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保养计划检查项目';

CREATE TABLE IF NOT EXISTS pm_execution_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    work_order_id BIGINT NOT NULL COMMENT '保养工单 ID（逻辑关联 mnt_work_order.id）',
    plan_item_id BIGINT NOT NULL COMMENT '计划项目 ID（逻辑关联 pm_plan_item.id）',
    result VARCHAR(20) NOT NULL COMMENT 'NORMAL/ABNORMAL',
    measured_value VARCHAR(100) NULL COMMENT '实测值',
    remark VARCHAR(500) NULL COMMENT '执行说明',
    executor_id BIGINT NOT NULL COMMENT '执行工程师 ID',
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pm_execution_order_item (work_order_id, plan_item_id),
    KEY idx_pm_execution_executor_time (executor_id, executed_at),
    CONSTRAINT chk_pm_execution_result CHECK (result IN ('NORMAL', 'ABNORMAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保养工单检查结果';
