-- ========================
-- Database Backup Task Type
-- Flyway Migration V2
-- ========================

-- Tasks are no longer required to have an SMTP configuration —
-- only EMAIL_NOTIFICATION tasks do. DATABASE_BACKUP tasks use a
-- backup_configuration instead (see below).
ALTER TABLE tasks
    MODIFY COLUMN smtp_configuration_id BIGINT NULL;

-- Backup Configuration
-- Holds the shell command Task Scheduler invokes to trigger an
-- external backup process. Task Scheduler does not know how the
-- backup itself works — it only runs the command and records the
-- exit code / output. Actual backup correctness (e.g. a valid DB
-- dump) is the responsibility of the script/tool being invoked.
CREATE TABLE backup_configurations (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(255)        NOT NULL,
    command             TEXT                NOT NULL COMMENT 'Shell command executed to trigger the backup, e.g. /opt/scripts/backup.sh',
    working_directory   VARCHAR(500)                 COMMENT 'Optional working directory the command is executed from',
    timeout_seconds      INT                 NOT NULL DEFAULT 300 COMMENT 'Max time to wait for the command before treating it as a failure',
    is_active           BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE tasks
    ADD COLUMN backup_configuration_id BIGINT NULL AFTER smtp_configuration_id,
    ADD CONSTRAINT fk_task_backup_configuration FOREIGN KEY (backup_configuration_id) REFERENCES backup_configurations(id);

-- Backup Executions
-- One row per attempt of a DATABASE_BACKUP task, analogous to
-- email_notifications for EMAIL_NOTIFICATION tasks.
CREATE TABLE backup_executions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_execution_id   BIGINT              NOT NULL,
    command             TEXT                NOT NULL,
    exit_code           INT,
    status              VARCHAR(50)         NOT NULL,
    output              TEXT                COMMENT 'Combined stdout/stderr, truncated to a reasonable size',
    attempt_no          INT                 NOT NULL DEFAULT 1,
    started_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        DATETIME,
    created_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_backup_execution_execution FOREIGN KEY (task_execution_id) REFERENCES task_executions(id)
);

CREATE INDEX idx_backup_configurations_is_active ON backup_configurations(is_active);
CREATE INDEX idx_backup_executions_status        ON backup_executions(status);
