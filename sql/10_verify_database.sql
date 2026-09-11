USE industrial_maintenance;

-- 预期：table_count = 38。
SELECT COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE';

-- 预期：所有表均为 InnoDB、utf8mb4_0900_ai_ci。
SELECT table_name, engine, table_collation
FROM information_schema.tables
WHERE table_schema = DATABASE()
ORDER BY table_name;

-- 预期：role_count = 5，sla_rule_count = 3。
SELECT
    (SELECT COUNT(*) FROM sys_role) AS role_count,
    (SELECT COUNT(*) FROM mnt_sla_rule) AS sla_rule_count;

-- 预期：以下查询均返回 0 行。
SELECT username, COUNT(*) AS duplicate_count
FROM sys_user
GROUP BY username
HAVING COUNT(*) > 1;

SELECT equipment_no, COUNT(*) AS duplicate_count
FROM eqp_equipment
GROUP BY equipment_no
HAVING COUNT(*) > 1;

SELECT work_order_no, COUNT(*) AS duplicate_count
FROM mnt_work_order
GROUP BY work_order_no
HAVING COUNT(*) > 1;

SELECT warehouse_id, spare_part_id, COUNT(*) AS duplicate_count
FROM inv_stock
GROUP BY warehouse_id, spare_part_id
HAVING COUNT(*) > 1;

SELECT spare_no, COUNT(*) AS duplicate_count
FROM inv_spare_part
GROUP BY spare_no
HAVING COUNT(*) > 1;

SELECT warehouse_no, COUNT(*) AS duplicate_count
FROM inv_warehouse
GROUP BY warehouse_no
HAVING COUNT(*) > 1;

SELECT role_code, COUNT(*) AS duplicate_count
FROM sys_role
GROUP BY role_code
HAVING COUNT(*) > 1;

SELECT permission_code, COUNT(*) AS duplicate_count
FROM sys_menu
WHERE permission_code IS NOT NULL
GROUP BY permission_code
HAVING COUNT(*) > 1;

-- 预期：redundant_exact_index_count = 0。
SELECT COUNT(*) AS redundant_exact_index_count
FROM (
    SELECT table_name, index_type, non_unique, columns_signature, COUNT(*) AS duplicate_index_count
    FROM (
        SELECT
            table_name,
            index_name,
            index_type,
            non_unique,
            GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_signature
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
        GROUP BY table_name, index_name, index_type, non_unique
    ) AS index_definitions
    GROUP BY table_name, index_type, non_unique, columns_signature
    HAVING COUNT(*) > 1
) AS duplicate_indexes;
