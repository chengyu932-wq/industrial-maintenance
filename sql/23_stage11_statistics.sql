USE industrial_maintenance;
SET NAMES utf8mb4;

-- 统计期先按日期过滤运行小时，再按设备应用数据范围；为该访问路径补充复合索引。
SET @stage11_runtime_index_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eqp_runtime_record'
      AND INDEX_NAME = 'idx_runtime_date_equipment'
);
SET @stage11_runtime_index_sql = IF(
    @stage11_runtime_index_exists = 0,
    'ALTER TABLE eqp_runtime_record ADD INDEX idx_runtime_date_equipment(record_date, equipment_id)',
    'SELECT 1'
);
PREPARE stage11_runtime_index_stmt FROM @stage11_runtime_index_sql;
EXECUTE stage11_runtime_index_stmt;
DEALLOCATE PREPARE stage11_runtime_index_stmt;
