-- ================================
-- Task execution state
-- Flyway Migration V4
-- ================================
--
-- Fixes duplicate execution of due tasks.
--
-- Previously the scheduler decided whether a task was due purely by
-- re-evaluating its cron expression on every poll, against a hardcoded
-- five-minute lookback window:
--
--     cron.next(now.minusMinutes(5)) <= now
--
-- Nothing recorded that a task had already run, so a task remained "due"
-- for the entire five-minute window. With a 10s polling interval that meant
-- roughly 30 executions per intended run. The in-memory queue's duplicate
-- check could not prevent it, because the executor drains the queue between
-- polls, leaving nothing to match against.
--
-- These columns move that decision out of the cron expression and into
-- persisted state:
--
--   next_execution_time  When this task should next run. The scheduler selects
--                        tasks whose time has passed, then immediately advances
--                        this to the following occurrence — so a task stops
--                        being due the instant it is queued, not five minutes
--                        later. Also survives restarts.
--
--   last_execution_time  When the task was last queued. Diagnostic only; the
--                        scheduler does not read it.
--
-- Both are NULL for existing rows. The scheduler treats NULL as "not yet
-- initialised": it computes the next occurrence and saves it without running
-- the task, so upgrading cannot cause a backlog of missed runs to fire at once.

ALTER TABLE tasks
    ADD COLUMN next_execution_time DATETIME NULL
        COMMENT 'When this task is next due. Advanced as soon as the task is queued.'
        AFTER is_active,
    ADD COLUMN last_execution_time DATETIME NULL
        COMMENT 'When this task was last queued for execution. Diagnostic only.'
        AFTER next_execution_time;

-- The scheduler polls with WHERE is_active = TRUE AND next_execution_time <= ?
CREATE INDEX idx_tasks_next_execution_time ON tasks(next_execution_time);
