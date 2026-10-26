SET @col_doc_feedback = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'documentos' AND COLUMN_NAME = 'teacher_feedback') > 0,
    'SELECT 1',
    'ALTER TABLE documentos ADD COLUMN teacher_feedback VARCHAR(2000)'
));
PREPARE stmt1 FROM @col_doc_feedback;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SET @col_doc_date = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'documentos' AND COLUMN_NAME = 'feedback_date') > 0,
    'SELECT 1',
    'ALTER TABLE documentos ADD COLUMN feedback_date DATETIME(6)'
));
PREPARE stmt2 FROM @col_doc_date;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @col_plan_date = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'planificaciones' AND COLUMN_NAME = 'feedback_date') > 0,
    'SELECT 1',
    'ALTER TABLE planificaciones ADD COLUMN feedback_date DATETIME(6)'
));
PREPARE stmt3 FROM @col_plan_date;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;
