package com.taskscheduler.scheduler;

import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.entity.TaskExecutionLog;
import com.taskscheduler.enums.ExecutionStatus;
import com.taskscheduler.repository.TaskExecutionLogRepository;
import com.taskscheduler.repository.TaskExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Drives the {@link RetryQueue}: promotes retries whose delay has elapsed onto
 * the main execution queue, and rebuilds the queue after a restart.
 *
 * <p>This is the half of the retry mechanism that used to be a
 * {@code Thread.sleep} inside a pool thread. Failures are now parked in the
 * {@link RetryQueue} with a due time and the pool thread is released; this
 * engine sweeps the queue on a short interval and re-queues the ones that are
 * ready. The delay is enforced by a timestamp, so it costs no threads.</p>
 *
 * <p><b>Skipping.</b> A retry is dropped rather than run when it would collide
 * with the task's normal schedule — either because the task's next scheduled
 * occurrence has already arrived, or because a fresh run is already queued or
 * executing. In both cases the interrupted run's execution row is marked
 * {@link ExecutionStatus#SKIPPED} with a log entry recording both the intended
 * retry time and the scheduled time, so nothing disappears silently.</p>
 *
 * <p><b>Restart.</b> The retry queue is in-memory, so a crash loses it. On
 * startup it is rebuilt from the execution rows a previous run left behind:
 * every {@code IN_PROGRESS} row (a run that was mid-flight or between retries),
 * and any {@code FAILED} row that still has retries left. Both advance to their
 * next attempt — an interrupted {@code IN_PROGRESS} run counts the crash as a
 * consumed retry, so a run that only ever crashes eventually exhausts its budget
 * and is dead-lettered on restart rather than tying up a thread forever. A
 * {@code FAILED} run whose retries are already spent is in the DLQ and left
 * alone. {@code IN_PROGRESS} retries are due immediately; {@code FAILED} retries
 * wait out the remaining delay.</p>
 *
 * <p><b>The restart caveat.</b> Re-running an interrupted {@code IN_PROGRESS}
 * run can repeat a side effect that had already happened — an email that was
 * sent, a backup that was written — because the crash leaves the run in an
 * unknown state. This engine retries it anyway, trading at-most-once for
 * at-least-once. Handlers that must not double up need their own idempotency.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryQueueEngine {

    private final RetryQueue retryQueue;
    private final BoundedPriorityTaskQueue taskQueue;
    private final TaskExecutorEngine taskExecutorEngine;
    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskExecutionLogRepository taskExecutionLogRepository;

    @Value("${app.scheduler.max-retries:3}")
    private int maxRetries;

    @Value("${app.scheduler.retry-delay-ms:5000}")
    private long retryDelayMs;

    /**
     * Sweeps the retry queue and promotes every due retry onto the main queue,
     * skipping any that would collide with the task's normal schedule.
     *
     * <p>Runs on a short fixed delay (default 1s, {@code app.scheduler.retry-queue-poll-ms})
     * so retries fire close to their intended time without a dedicated timer per retry.</p>
     */
    @Scheduled(fixedDelayString = "${app.scheduler.retry-queue-poll-ms:1000}")
    public void promoteDueRetries() {
        LocalDateTime now = LocalDateTime.now();
        List<RetryEntry> due = retryQueue.pollDue(now);
        if (due.isEmpty()) {
            return;
        }

        log.debug("RetryQueueEngine: {} retr{} due.", due.size(), due.size() == 1 ? "y" : "ies");

        for (RetryEntry entry : due) {
            try {
                promote(entry);
            } catch (Exception ex) {
                // One bad entry must not stall the sweep. Re-park it so a transient
                // fault (e.g. a momentary DB blip) gets another attempt next sweep.
                log.error("RetryQueueEngine: Failed to promote retry for task [{}]; re-parking. Reason: {}",
                        entry.taskId(), ex.getMessage());
                retryQueue.schedule(entry);
            }
        }
    }

    /**
     * Promotes a single due retry, or skips it if it collides with the schedule.
     *
     * @param entry the due retry
     */
    private void promote(RetryEntry entry) {
        TaskExecution execution = taskExecutionRepository.findById(entry.taskExecutionId()).orElse(null);
        if (execution == null) {
            log.error("RetryQueueEngine: Retry for task [{}] references execution [{}], which no longer "
                    + "exists. Dropping.", entry.taskId(), entry.taskExecutionId());
            return;
        }

        Task task = execution.getTask();
        if (task == null) {
            log.error("RetryQueueEngine: Execution [{}] has no task. Dropping retry.", entry.taskExecutionId());
            return;
        }

        int attemptNo = entry.retryCount() + 1;

        // Guard A — the task's next scheduled occurrence has caught up with the retry.
        LocalDateTime nextScheduled = task.getNextExecutionTime();
        if (nextScheduled != null && !entry.nextRetryTime().isBefore(nextScheduled)) {
            markSkipped(execution, attemptNo, String.format(
                    "Retry was due at %s, but the task's next scheduled run at %s has arrived first. "
                            + "Skipping the retry; the task will run on schedule.",
                    entry.nextRetryTime(), nextScheduled));
            return;
        }

        // Guard B — a fresh run of this task is already queued or executing.
        if (taskQueue.contains(entry.taskId()) || taskExecutorEngine.isTaskActive(entry.taskId())) {
            markSkipped(execution, attemptNo, String.format(
                    "Retry was due at %s, but a fresh run of this task is already queued or executing. "
                            + "Skipping the retry to avoid a duplicate run.",
                    entry.nextRetryTime()));
            return;
        }

        QueuedTask retryTask = new QueuedTask(task, entry.retryCount(), entry.taskExecutionId());
        boolean requeued = taskQueue.offerRetry(retryTask);
        if (requeued) {
            log.info("RetryQueueEngine: Task [{}] promoted to the main queue for attempt {}.",
                    entry.taskId(), attemptNo);
        } else {
            // Queue full — hold the retry and try again next sweep. This is the
            // backpressure the old thread-blocking approach gave for free.
            retryQueue.schedule(entry);
            log.warn("RetryQueueEngine: Main queue full; retry for task [{}] held for the next sweep.",
                    entry.taskId());
        }
    }

    /**
     * Rebuilds the retry queue from the execution rows a previous run left behind,
     * so retries interrupted by a restart resume.
     *
     * <p>Two kinds of row are recovered, and only while they still have attempts left:</p>
     * <ul>
     *   <li>{@code IN_PROGRESS} — a run that was mid-flight or between retries. Its
     *       current attempt never finished, so it is re-run as the <em>same</em> attempt
     *       (retry count unchanged) for an immediate retry ({@code nextRetryTime = now}):
     *       the process was down, so any delay has effectively passed.</li>
     *   <li>{@code FAILED} with retries left — a completed failed attempt that had not yet
     *       exhausted its budget. It advances to its <em>next</em> attempt, due one retry
     *       delay after it failed ({@code nextRetryTime = updatedAt + retry-delay}). A
     *       {@code FAILED} row whose retries are spent is already in the DLQ and is left
     *       alone; a DLQ-existence check guards against a raised {@code max-retries}
     *       resurrecting one.</li>
     * </ul>
     *
     * <p>A run whose next scheduled occurrence is already due is skipped instead of retried —
     * the fresh run supersedes the interrupted one. Actual promotion, and the collision guards
     * around it, are left to {@link #promoteDueRetries()}; this method only re-populates the queue.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void rebuildOnStartup() {
        List<TaskExecution> recoverable = taskExecutionRepository.findByStatusIn(
                List.of(ExecutionStatus.IN_PROGRESS, ExecutionStatus.FAILED));
        if (recoverable.isEmpty()) {
            log.info("RetryQueueEngine: No runs to recover on startup.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int rescheduled = 0;
        int skipped = 0;
        int leftAlone = 0;
        int deadLettered = 0;

        for (TaskExecution execution : recoverable) {
            try {
                Task task = execution.getTask();
                if (task == null) {
                    log.error("RetryQueueEngine: Execution [{}] has no task. Leaving as-is.", execution.getId());
                    continue;
                }

                // retryCount is retries, not attempts: attempt 1 + `retryCount` retries, so the
                // attempt that was running/failed is retryCount + 1. Exhaustion is
                // retryCount == maxRetries - 1 — the runtime's own DLQ boundary.
                int retryCount = execution.getRetryCount() == null ? 0 : execution.getRetryCount();
                boolean inProgress = execution.getStatus() == ExecutionStatus.IN_PROGRESS;

                // Both statuses advance to the next attempt: a FAILED attempt is done, and an
                // interrupted IN_PROGRESS attempt counts the crash as a consumed retry — so a run
                // that only ever crashes eventually stops instead of tying up a thread forever.
                int resumeRetryCount = retryCount + 1;
                boolean exhausted = retryCount >= maxRetries - 1;

                LocalDateTime nextRetryTime;
                if (inProgress) {
                    // Process was down, so no delay to wait out — due now.
                    nextRetryTime = now;
                } else {
                    // FAILED: the engine already processed this failure. If retries were spent it
                    // is already in the DLQ; leave it. Otherwise it had parked a retry we lost with
                    // the in-memory queue, so rebuild that retry due one delay after it failed.
                    if (exhausted) {
                        leftAlone++;
                        continue;
                    }
                    LocalDateTime failedAt = execution.getUpdatedAt() == null ? now : execution.getUpdatedAt();
                    nextRetryTime = failedAt.plus(Duration.ofMillis(retryDelayMs));
                }

                // A retry at or past the task's next scheduled run is pointless — the fresh run
                // supersedes it — so skip rather than retry or dead-letter the interrupted run.
                LocalDateTime nextScheduled = task.getNextExecutionTime();
                if (nextScheduled != null && !nextRetryTime.isBefore(nextScheduled)) {
                    markSkipped(execution, retryCount + 1, String.format(
                            "Run was %s when the application stopped, and its retry (due %s) is at or past the "
                                    + "next scheduled run at %s. Skipping the retry; the task will run on schedule.",
                            execution.getStatus(), nextRetryTime, nextScheduled));
                    skipped++;
                    continue;
                }

                if (inProgress && exhausted) {
                    // Crashes used up the budget. A crash never reaches the runtime's failure path,
                    // so this is the only place that can dead-letter it.
                    taskExecutorEngine.deadLetterInterrupted(task, execution, retryCount + 1,
                            "Run was IN_PROGRESS when the application stopped and has no retries left. "
                                    + "Dead-lettered on restart so it does not re-run indefinitely.");
                    deadLettered++;
                    log.warn("RetryQueueEngine: Task [{}] (execution [{}]) exhausted its retries via crashes. Dead-lettered.",
                            task.getId(), execution.getId());
                    continue;
                }

                retryQueue.schedule(new RetryEntry(task.getId(), execution.getId(), resumeRetryCount, nextRetryTime));
                rescheduled++;
                log.info("RetryQueueEngine: Recovered {} run for task [{}] (execution [{}], attempt {}, due {}).",
                        execution.getStatus(), task.getId(), execution.getId(), resumeRetryCount + 1, nextRetryTime);

            } catch (Exception ex) {
                log.error("RetryQueueEngine: Failed to recover execution [{}] on startup. Reason: {}",
                        execution.getId(), ex.getMessage());
            }
        }

        log.info("RetryQueueEngine: Startup recovery complete. Re-queued: {} | Skipped: {} | Dead-lettered: {} | Already terminal: {}",
                rescheduled, skipped, deadLettered, leftAlone);
    }

    /**
     * Marks a run as {@link ExecutionStatus#SKIPPED} and records why in the log.
     *
     * @param execution the execution row to close out
     * @param attemptNo the attempt the skipped retry would have been
     * @param reason    the human-readable reason, carrying the relevant timestamps
     */
    private void markSkipped(TaskExecution execution, int attemptNo, String reason) {
        execution.setStatus(ExecutionStatus.SKIPPED);
        execution.setCompletedAt(LocalDateTime.now());
        taskExecutionRepository.save(execution);

        TaskExecutionLog logEntry = TaskExecutionLog.builder()
                .taskExecution(execution)
                .status(ExecutionStatus.SKIPPED)
                .attemptNo(attemptNo)
                .message(reason)
                .build();
        taskExecutionLogRepository.save(logEntry);

        log.info("RetryQueueEngine: Execution [{}] marked SKIPPED. {}", execution.getId(), reason);
    }
}
