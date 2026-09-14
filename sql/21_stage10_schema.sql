USE industrial_maintenance;
SET NAMES utf8mb4;

-- 第10阶段增量结构：保留知识中的维修结果快照和审核意见。
SET @has_kb_repair_result = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'kb_article' AND COLUMN_NAME = 'repair_result'
);
SET @add_kb_repair_result = IF(@has_kb_repair_result = 0,
    'ALTER TABLE kb_article ADD COLUMN repair_result TEXT NULL COMMENT ''维修结果快照'' AFTER solution',
    'SELECT 1');
PREPARE stage10_stmt FROM @add_kb_repair_result;
EXECUTE stage10_stmt;
DEALLOCATE PREPARE stage10_stmt;

SET @has_kb_review_remark = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'kb_article' AND COLUMN_NAME = 'review_remark'
);
SET @add_kb_review_remark = IF(@has_kb_review_remark = 0,
    'ALTER TABLE kb_article ADD COLUMN review_remark VARCHAR(500) NULL COMMENT ''审核意见'' AFTER reviewed_by',
    'SELECT 1');
PREPARE stage10_stmt FROM @add_kb_review_remark;
EXECUTE stage10_stmt;
DEALLOCATE PREPARE stage10_stmt;
