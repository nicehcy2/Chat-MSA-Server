-- 03_event.sql
USE chatdb;  -- 실제 DB명으로 변경

SET GLOBAL event_scheduler = ON;

CREATE EVENT IF NOT EXISTS clean_outbox_old_records
ON SCHEDULE EVERY 1 DAY
STARTS STR_TO_DATE(CONCAT(DATE(NOW() + INTERVAL 1 DAY), ' 03:00:00'), '%Y-%m-%d %H:%i:%s')
DO
DELETE FROM outbox
WHERE created_at < NOW() - INTERVAL 2 DAY;