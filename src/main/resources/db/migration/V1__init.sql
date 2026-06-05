-- ========================
-- Task Scheduler Init SQL
-- Flyway Migration V1
-- ========================

-- SMTP Configuration
CREATE TABLE smtp_configurations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    host        VARCHAR(255)        NOT NULL,
    port        INT                 NOT NULL,
    username    VARCHAR(255)        NOT NULL,
    password    VARCHAR(255)        NOT NULL,
    is_active   BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Templates
CREATE TABLE templates (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255)        NOT NULL,
    template    TEXT                NOT NULL,
    is_active   BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tasks
CREATE TABLE tasks (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                    VARCHAR(255)        NOT NULL,
    type                    VARCHAR(50)         NOT NULL,
    cron_expression         VARCHAR(100)        NOT NULL,
    smtp_configuration_id   BIGINT              NOT NULL,
    is_active               BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at              DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_smtp FOREIGN KEY (smtp_configuration_id) REFERENCES smtp_configurations(id)
);

-- Template Mappers
CREATE TABLE template_mappers (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id     BIGINT              NOT NULL,
    template_id BIGINT              NOT NULL,
    key_name    VARCHAR(255)        NOT NULL,
    value       TEXT                NOT NULL,
    created_at  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mapper_task     FOREIGN KEY (task_id)     REFERENCES tasks(id),
    CONSTRAINT fk_mapper_template FOREIGN KEY (template_id) REFERENCES templates(id)
);

-- Task Executions
CREATE TABLE task_executions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT              NOT NULL,
    status          VARCHAR(50)         NOT NULL,
    retry_count     INT                 NOT NULL DEFAULT 0,
    started_at      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    DATETIME,
    created_at      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_execution_task FOREIGN KEY (task_id) REFERENCES tasks(id)
);

-- Task Execution Logs
CREATE TABLE task_execution_logs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_execution_id   BIGINT              NOT NULL,
    status              VARCHAR(50)         NOT NULL,
    attempt_no          INT                 NOT NULL DEFAULT 1,
    message             TEXT,
    created_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_execution FOREIGN KEY (task_execution_id) REFERENCES task_executions(id)
);

-- Email Notifications
CREATE TABLE email_notifications (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_execution_id   BIGINT              NOT NULL,
    email_message       TEXT                NOT NULL,
    status              VARCHAR(50)         NOT NULL,
    attempt_no          INT                 NOT NULL DEFAULT 1,
    sent_at             DATETIME,
    created_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_execution FOREIGN KEY (task_execution_id) REFERENCES task_executions(id)
);

-- Dead Letter Queue
CREATE TABLE task_dlq (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id             BIGINT              NOT NULL,
    task_execution_id   BIGINT              NOT NULL,
    failure_reason      TEXT                NOT NULL,
    status              VARCHAR(50)         NOT NULL DEFAULT 'NEW',
    created_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dlq_task      FOREIGN KEY (task_id)           REFERENCES tasks(id),
    CONSTRAINT fk_dlq_execution FOREIGN KEY (task_execution_id) REFERENCES task_executions(id)
);

-- Indexes for performance
CREATE INDEX idx_tasks_is_active             ON tasks(is_active);
CREATE INDEX idx_task_executions_task_id     ON task_executions(task_id);
CREATE INDEX idx_task_executions_status      ON task_executions(status);
CREATE INDEX idx_email_notifications_status  ON email_notifications(status);
CREATE INDEX idx_task_dlq_status             ON task_dlq(status);
CREATE INDEX idx_template_mappers_task_id    ON template_mappers(task_id);
