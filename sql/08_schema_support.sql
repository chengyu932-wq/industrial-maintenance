USE industrial_maintenance;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS msg_notification (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    receiver_id BIGINT NOT NULL COMMENT '接收人 ID（逻辑关联 sys_user.id）',
    message_type VARCHAR(30) NOT NULL COMMENT 'ASSIGN/SLA/STOCK_WARNING',
    title VARCHAR(200) NOT NULL COMMENT '消息标题',
    content TEXT NOT NULL COMMENT '消息内容',
    business_type VARCHAR(30) NULL COMMENT 'WORK_ORDER/STOCK',
    business_id BIGINT NULL COMMENT '关联业务 ID',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0 否，1 是',
    read_at DATETIME NULL COMMENT '阅读时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_msg_receiver_read_time (receiver_id, is_read, created_at),
    KEY idx_msg_business (business_type, business_id),
    CONSTRAINT chk_msg_type CHECK (message_type IN ('ASSIGN', 'SLA', 'STOCK_WARNING')),
    CONSTRAINT chk_msg_business_type CHECK (business_type IS NULL OR business_type IN ('WORK_ORDER', 'STOCK')),
    CONSTRAINT chk_msg_read CHECK (is_read IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内消息';
