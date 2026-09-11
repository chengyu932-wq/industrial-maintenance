USE industrial_maintenance;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS inv_warehouse (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    warehouse_no VARCHAR(50) NOT NULL COMMENT '仓库编号',
    warehouse_name VARCHAR(100) NOT NULL COMMENT '仓库名称',
    location VARCHAR(255) NULL COMMENT '仓库位置',
    manager_id BIGINT NULL COMMENT '负责人 ID（逻辑关联 sys_user.id）',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_warehouse_no (warehouse_no),
    KEY idx_inv_warehouse_manager (manager_id),
    CONSTRAINT chk_inv_warehouse_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仓库表';

CREATE TABLE IF NOT EXISTS inv_warehouse_user (
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID（逻辑关联 inv_warehouse.id）',
    user_id BIGINT NOT NULL COMMENT '授权用户 ID（逻辑关联 sys_user.id）',
    PRIMARY KEY (warehouse_id, user_id),
    KEY idx_inv_warehouse_user_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户授权仓库关系';

CREATE TABLE IF NOT EXISTS inv_supplier (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    supplier_no VARCHAR(50) NOT NULL COMMENT '供应商编号',
    supplier_name VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact_name VARCHAR(50) NULL COMMENT '联系人',
    phone VARCHAR(20) NULL COMMENT '联系电话',
    address VARCHAR(255) NULL COMMENT '地址',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_supplier_no (supplier_no),
    CONSTRAINT chk_inv_supplier_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备件供应商表';

CREATE TABLE IF NOT EXISTS inv_spare_part (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    spare_no VARCHAR(50) NOT NULL COMMENT '备件编号',
    spare_name VARCHAR(100) NOT NULL COMMENT '备件名称',
    specification VARCHAR(200) NULL COMMENT '规格',
    brand VARCHAR(100) NULL COMMENT '品牌',
    unit VARCHAR(20) NOT NULL COMMENT '计量单位',
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '标准单价',
    supplier_id BIGINT NULL COMMENT '供应商 ID（逻辑关联 inv_supplier.id）',
    compatible_model VARCHAR(200) NULL COMMENT '适配设备型号',
    lead_time_days INT NOT NULL DEFAULT 0 COMMENT '采购提前期天数',
    safety_days INT NOT NULL DEFAULT 0 COMMENT '安全天数',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_spare_part_no (spare_no),
    KEY idx_inv_spare_supplier (supplier_id),
    KEY idx_inv_spare_name (spare_name),
    CONSTRAINT chk_inv_spare_price CHECK (unit_price >= 0),
    CONSTRAINT chk_inv_spare_days CHECK (lead_time_days >= 0 AND safety_days >= 0),
    CONSTRAINT chk_inv_spare_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备件主数据';

CREATE TABLE IF NOT EXISTS inv_stock (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID（逻辑关联 inv_warehouse.id）',
    spare_part_id BIGINT NOT NULL COMMENT '备件 ID（逻辑关联 inv_spare_part.id）',
    current_qty DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '当前库存数量',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_stock_warehouse_spare (warehouse_id, spare_part_id),
    KEY idx_inv_stock_spare (spare_part_id),
    CONSTRAINT chk_inv_stock_qty CHECK (current_qty >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仓库备件当前库存';

CREATE TABLE IF NOT EXISTS inv_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    transaction_no VARCHAR(50) NOT NULL COMMENT '库存流水业务号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID（逻辑关联 inv_warehouse.id）',
    spare_part_id BIGINT NOT NULL COMMENT '备件 ID（逻辑关联 inv_spare_part.id）',
    transaction_type VARCHAR(30) NOT NULL COMMENT '库存业务类型',
    qty_change DECIMAL(12,2) NOT NULL COMMENT '正数增加、负数减少',
    qty_before DECIMAL(12,2) NOT NULL COMMENT '变更前数量',
    qty_after DECIMAL(12,2) NOT NULL COMMENT '变更后数量',
    unit_price DECIMAL(12,2) NULL COMMENT '当次成本单价快照',
    work_order_id BIGINT NULL COMMENT '关联工单 ID',
    equipment_id BIGINT NULL COMMENT '关联设备 ID',
    related_transaction_id BIGINT NULL COMMENT '调拨对端或退库来源流水 ID',
    operator_id BIGINT NOT NULL COMMENT '操作人 ID',
    remark VARCHAR(500) NULL COMMENT '业务单号或说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '流水时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv_transaction_no (transaction_no),
    KEY idx_inv_transaction_stock_time (warehouse_id, spare_part_id, created_at),
    KEY idx_inv_transaction_type_time (transaction_type, created_at),
    KEY idx_inv_transaction_work_order (work_order_id),
    KEY idx_inv_transaction_equipment (equipment_id),
    KEY idx_inv_transaction_related (related_transaction_id),
    CONSTRAINT chk_inv_transaction_type CHECK (
        transaction_type IN ('INBOUND', 'OUTBOUND', 'ISSUE', 'RETURN', 'TRANSFER_IN', 'TRANSFER_OUT', 'STOCKTAKE', 'SCRAP')
    ),
    CONSTRAINT chk_inv_transaction_qty CHECK (qty_change <> 0 AND qty_before >= 0 AND qty_after >= 0),
    CONSTRAINT chk_inv_transaction_balance CHECK (qty_after = qty_before + qty_change),
    CONSTRAINT chk_inv_transaction_price CHECK (unit_price IS NULL OR unit_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不可普通删除的库存流水';

CREATE TABLE IF NOT EXISTS inv_work_order_spare (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    work_order_id BIGINT NOT NULL COMMENT '工单 ID（逻辑关联 mnt_work_order.id）',
    warehouse_id BIGINT NOT NULL COMMENT '领用仓库 ID',
    spare_part_id BIGINT NOT NULL COMMENT '备件 ID',
    qty DECIMAL(12,2) NOT NULL COMMENT '领用数量',
    unit_price_snapshot DECIMAL(12,2) NOT NULL COMMENT '领用时单价快照',
    transaction_id BIGINT NOT NULL COMMENT '对应 ISSUE 库存流水 ID',
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED/RETURNED/PARTIAL_RETURN',
    issued_by BIGINT NOT NULL COMMENT '领用操作人/工程师 ID',
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领用时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_order_spare_transaction (transaction_id),
    KEY idx_work_order_spare_order (work_order_id),
    KEY idx_work_order_spare_stock (warehouse_id, spare_part_id),
    CONSTRAINT chk_work_order_spare_qty CHECK (qty > 0),
    CONSTRAINT chk_work_order_spare_price CHECK (unit_price_snapshot >= 0),
    CONSTRAINT chk_work_order_spare_status CHECK (status IN ('ISSUED', 'RETURNED', 'PARTIAL_RETURN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单备件领用关系';

CREATE TABLE IF NOT EXISTS inv_warning (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    spare_part_id BIGINT NOT NULL COMMENT '备件 ID',
    warning_type VARCHAR(20) NOT NULL DEFAULT 'LOW_STOCK' COMMENT 'LOW_STOCK',
    threshold_qty DECIMAL(12,2) NOT NULL COMMENT '触发时建议安全库存 SS',
    current_qty DECIMAL(12,2) NOT NULL COMMENT '触发时当前库存',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
    triggered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at DATETIME NULL,
    message_id BIGINT NULL COMMENT '关联站内消息 ID',
    PRIMARY KEY (id),
    KEY idx_inv_warning_open (status, triggered_at),
    KEY idx_inv_warning_stock (warehouse_id, spare_part_id, triggered_at),
    CONSTRAINT chk_inv_warning_type CHECK (warning_type = 'LOW_STOCK'),
    CONSTRAINT chk_inv_warning_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT chk_inv_warning_qty CHECK (threshold_qty >= 0 AND current_qty >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='低库存预警事件';
