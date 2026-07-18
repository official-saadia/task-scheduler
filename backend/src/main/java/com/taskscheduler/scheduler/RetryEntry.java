package com.taskscheduler.scheduler;

import java.time.LocalDateTime;

/**
 * A single pending retry held in the in-memory {@link RetryQueue}.
 *
 * <p>Records everything needed to re-queue a failed attempt without touching the
 * database in the meantime: which task, which execution row the run already owns,
 * the retry count the next attempt will run under, and the earliest time that
 * attempt may be re-queued.</p>
 *
 * <p>This is intentionally in-memory only. It is not the source of truth — the
 * {@code task_executions} row is. If the process dies, the queue is lost and
 * rebuilt from {@code IN_PROGRESS} execution rows on startup (see
 * {@link RetryQueueEngine}). Promoting this to a durable store (a Redis sorted
 * set keyed on {@code nextRetryTime}, say) is the natural next step and would
 * remove the rebuild-on-startup dance entirely.</p>
 *
 * @param taskId          the task to retry
 * @param taskExecutionId the execution row this run is recorded against, carried
 *                        forward so every attempt stays on one row
 * @param retryCount      the retry count the next attempt runs under
 *                        ({@code attemptNo == retryCount + 1})
 * @param nextRetryTime   the earliest time this retry may be re-queued
 */
public record RetryEntry(
        Long taskId,
        Long taskExecutionId,
        int retryCount,
        LocalDateTime nextRetryTime
) {
    /**
     * @param now the reference time, normally {@link LocalDateTime#now()}
     * @return {@code true} if this retry's delay has elapsed and it may be re-queued
     */
    public boolean isDue(LocalDateTime now) {
        return !nextRetryTime.isAfter(now);
    }
}
