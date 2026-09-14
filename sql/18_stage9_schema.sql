USE industrial_maintenance;
SET NAMES utf8mb4;

-- 第9阶段增量结构：设备类型所需技能、SLA 规则绑定和超时事件幂等。
CREATE TABLE IF NOT EXISTS eqp_type_skill (
    type_id BIGINT NOT NULL COMMENT '设备类型 ID（逻辑关联 eqp_type.id）',
    skill_id BIGINT NOT NULL COMMENT '所需技能 ID（逻辑关联 org_skill.id）',
    PRIMARY KEY (type_id, skill_id),
    KEY idx_eqp_type_skill_skill (skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备类型所需技能映射';

SET @has_sla_rule_id = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mnt_work_order' AND COLUMN_NAME = 'sla_rule_id'
);
SET @add_sla_rule_id = IF(@has_sla_rule_id = 0,
    'ALTER TABLE mnt_work_order ADD COLUMN sla_rule_id BIGINT NULL COMMENT ''创建时绑定的 SLA 规则 ID'' AFTER cancel_reason, ADD KEY idx_work_order_sla_rule (sla_rule_id)',
    'SELECT 1');
PREPARE stage9_stmt FROM @add_sla_rule_id;
EXECUTE stage9_stmt;
DEALLOCATE PREPARE stage9_stmt;

SET @has_sla_event_unique = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mnt_sla_event' AND INDEX_NAME = 'uk_sla_event_order_type'
);
SET @add_sla_event_unique = IF(@has_sla_event_unique = 0,
    'ALTER TABLE mnt_sla_event ADD UNIQUE KEY uk_sla_event_order_type (work_order_id, event_type)',
    'SELECT 1');
PREPARE stage9_stmt FROM @add_sla_event_unique;
EXECUTE stage9_stmt;
DEALLOCATE PREPARE stage9_stmt;
