-- ========================
-- Report Generation Task Type + Email Attachments
-- Flyway Migration V3
-- ========================

-- Report Generation Configuration
-- Same trigger-a-shell-command model as backup_configurations. Task
-- Scheduler runs `command` and, in addition to checking its exit code,
-- verifies that `output_file_path` exists afterwards — catching scripts
-- that exit 0 without actually producing a report.
CREATE TABLE report_generation_configurations (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(255)        NOT NULL,
    command             TEXT                NOT NULL COMMENT 'Shell command executed to generate the report',
    working_directory   VARCHAR(500)                 COMMENT 'Optional working directory the command is executed from',
    output_file_path    VARCHAR(500)        NOT NULL COMMENT 'Path Task Scheduler checks for existence after the command runs',
    timeout_seconds     INT                 NOT NULL DEFAULT 300,
    is_active           BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE tasks
    ADD COLUMN report_configuration_id BIGINT NULL AFTER backup_configuration_id,
    ADD CONSTRAINT fk_task_report_configuration FOREIGN KEY (report_configuration_id) REFERENCES report_generation_configurations(id),
    -- Optional, only meaningful for EMAIL_NOTIFICATION tasks. Points at a file
    -- on disk (typically a report_generation_configurations.output_file_path)
    -- to attach to every email sent by this task.
    ADD COLUMN attachment_path VARCHAR(500) NULL AFTER report_configuration_id;

-- Report Generation Executions
-- One row per attempt of a REPORT_GENERATION task, analogous to
-- backup_executions.
CREATE TABLE report_generation_executions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_execution_id   BIGINT              NOT NULL,
    command             TEXT                NOT NULL,
    exit_code           INT,
    output_file_path    VARCHAR(500)        NOT NULL,
    file_produced       BOOLEAN             NOT NULL DEFAULT FALSE,
    status              VARCHAR(50)         NOT NULL,
    output              TEXT                COMMENT 'Combined stdout/stderr, truncated to a reasonable size',
    attempt_no          INT                 NOT NULL DEFAULT 1,
    started_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        DATETIME,
    created_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_execution_execution FOREIGN KEY (task_execution_id) REFERENCES task_executions(id)
);

CREATE INDEX idx_report_configurations_is_active ON report_generation_configurations(is_active);
CREATE INDEX idx_report_executions_status        ON report_generation_executions(status);
