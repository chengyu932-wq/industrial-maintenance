USE industrial_maintenance;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS kb_article (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    title VARCHAR(200) NOT NULL COMMENT '知识标题',
    source_work_order_id BIGINT NULL COMMENT '来源已完成工单 ID',
    equipment_type_id BIGINT NULL COMMENT '设备类型 ID',
    fault_symptom TEXT NOT NULL COMMENT '故障现象',
    root_cause TEXT NOT NULL COMMENT '故障原因',
    solution TEXT NOT NULL COMMENT '解决方法',
    spare_summary TEXT NULL COMMENT '相关备件摘要',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/PUBLISHED/REJECTED',
    submitted_by BIGINT NOT NULL COMMENT '创建/提交人 ID',
    reviewed_by BIGINT NULL COMMENT '审核人 ID',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_kb_article_source_order (source_work_order_id),
    KEY idx_kb_article_status_time (status, created_at),
    KEY idx_kb_article_equipment_type (equipment_type_id),
    CONSTRAINT chk_kb_article_status CHECK (status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='故障知识条目';
