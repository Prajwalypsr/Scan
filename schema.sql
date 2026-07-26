-- ============================================================
--  DiskInsight — database schema
--
--  Run this once before starting the application with MySQL:
--      mysql -u root -p < schema.sql
--
--  Three tables:
--    scans  — one row per completed scan (the Scan history screen)
--    files  — every file found during a scan
--    rules  — the cleanup rules, so they survive a restart
-- ============================================================

CREATE DATABASE IF NOT EXISTS diskinsight
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE diskinsight;


-- ------------------------------------------------------------
-- One completed scan of one folder
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scans (
    scan_id      INT AUTO_INCREMENT PRIMARY KEY,
    folder_path  VARCHAR(512)  NOT NULL,
    scanned_at   DATETIME      NOT NULL,
    file_count   INT           NOT NULL DEFAULT 0,
    total_size   BIGINT        NOT NULL DEFAULT 0,  -- bytes
    flagged_size BIGINT        NOT NULL DEFAULT 0,  -- bytes matched by rules

    INDEX idx_scans_folder (folder_path),
    INDEX idx_scans_when   (scanned_at)
) ENGINE = InnoDB;


-- ------------------------------------------------------------
-- Every file found during a scan
--
-- size_bytes is BIGINT, not INT: an INT stops at 2.1 GB and a
-- single video file can be larger than that.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS files (
    file_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    scan_id       INT           NOT NULL,
    file_name     VARCHAR(512)  NOT NULL,
    extension     VARCHAR(32)   NOT NULL DEFAULT '',
    category      VARCHAR(32)   NOT NULL DEFAULT 'OTHER',
    size_bytes    BIGINT        NOT NULL DEFAULT 0,
    last_modified DATETIME      NOT NULL,
    folder_path   VARCHAR(512)  NOT NULL,

    CONSTRAINT fk_files_scan
        FOREIGN KEY (scan_id) REFERENCES scans (scan_id)
        ON DELETE CASCADE,

    INDEX idx_files_scan      (scan_id),
    INDEX idx_files_size      (size_bytes),
    INDEX idx_files_extension (extension),
    INDEX idx_files_modified  (last_modified)
) ENGINE = InnoDB;


-- ------------------------------------------------------------
-- Cleanup rules
--
-- A blank extensions column means "any file type",
-- min_size = 0 means "any size", older_than_days = 0 means "any age".
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rules (
    rule_id         INT PRIMARY KEY,
    rule_name       VARCHAR(128) NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    extensions      VARCHAR(255) NOT NULL DEFAULT '',
    min_size        BIGINT       NOT NULL DEFAULT 0,   -- bytes
    older_than_days INT          NOT NULL DEFAULT 0
) ENGINE = InnoDB;


-- ------------------------------------------------------------
-- The rules the application starts with.
-- These match Rule.defaults() in the Java code.
-- ------------------------------------------------------------
INSERT INTO rules
    (rule_id, rule_name, enabled, extensions, min_size, older_than_days)
VALUES
    (1, 'Very large files',       TRUE,  '',                         1073741824, 0),
    (2, 'Stale temporary files',  TRUE,  'tmp,part,crdownload,log',           0, 30),
    (3, 'Old archives',           TRUE,  'zip,rar,7z',                        0, 180),
    (4, 'Installers you kept',    TRUE,  'exe,msi,dmg,deb',                   0, 90),
    (5, 'Big videos over a year', FALSE, 'mp4,mkv,mov,avi',           524288000, 365)
ON DUPLICATE KEY UPDATE rule_name = VALUES(rule_name);


-- ------------------------------------------------------------
-- Useful queries for the report / viva
-- ------------------------------------------------------------

-- The largest files in the most recent scan
--   SELECT file_name, folder_path, size_bytes
--   FROM files
--   WHERE scan_id = (SELECT MAX(scan_id) FROM scans)
--   ORDER BY size_bytes DESC
--   LIMIT 20;

-- Space used per category
--   SELECT category, COUNT(*) AS files, SUM(size_bytes) AS bytes
--   FROM files
--   WHERE scan_id = (SELECT MAX(scan_id) FROM scans)
--   GROUP BY category
--   ORDER BY bytes DESC;

-- Files matched by rule 2 (stale temporary files), evaluated in SQL
--   SELECT f.file_name, f.size_bytes, f.last_modified
--   FROM files f
--   JOIN rules r ON r.rule_id = 2
--   WHERE f.scan_id = (SELECT MAX(scan_id) FROM scans)
--     AND FIND_IN_SET(f.extension, r.extensions)
--     AND f.last_modified < NOW() - INTERVAL r.older_than_days DAY;

-- Is the folder growing?
--   SELECT folder_path, scanned_at, file_count, total_size
--   FROM scans
--   ORDER BY scanned_at DESC
--   LIMIT 10;
