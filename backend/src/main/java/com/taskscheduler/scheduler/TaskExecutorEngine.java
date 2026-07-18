package com.taskscheduler.scheduler;

import com.taskscheduler.entity.*;
import com.taskscheduler.enums.DlqStatus;
import com.taskscheduler.enums.ExecutionStatus;
import com.taskscheduler.repository.*;
import com.taskscheduler.scheduler.handler.TaskHandler;
import com.taskscheduler.scheduler.handler.TaskHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Execution engine responsible for consuming tasks from the priority queue
 * and executing them asynchronously via the thread pool.
 *
 * <p>This class is intentionally kept free of task-type-specific logic.
 * It delegates all execution work to the appropriate {@link TaskHandler}
 * resolved from the {@link TaskHandlerRegistry}, fully adhering to the
 * <b>Open/Closed Principle</b>.</p>
 *
 * <p>Execution flow per task:</p>
 * <ol>
 *   <li>Dequeue highest-priority task from {@link BoundedPriorityTaskQueue}</li>
 *   <li>Create {@link TaskExecution} record, mark as {@code IN_PROGRESS}</li>
 *   <li>Register in-memory session in {@link ConcurrentHashMap}</li>
 *   <li>Submit to thread pool for async execution</li>
 *   <li>Resolve handler via {@link TaskHandlerRegistry}</li>
 *   <li>Delegate execution to handler</li>
 *   <li>On success: mark {@code COMPLETED}, remove session</li>
 *   <li>On failure: log attempt, retry or move to DLQ</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExecutorEngine {

    private final BoundedPriorityTaskQueue taskQueue;
    private final RetryQueue retryQueue;
    private final TaskRepository taskRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskExecutionLogRepository taskExecutionLogRepository;
    private final TaskDlqRepository taskDlqRepository;
    private final TaskHandlerRegistry taskHandlerRegistry;
    private final Executor taskExecutor;

    @Value("${app.scheduler.max-retries:3}")
    private int maxRetries;

    @Value("${app.scheduler.retry-delay-ms:5000}")
    private long retryDelayMs;

    /**
     * In-memory session map tracking all currently active task executions.
     * Key: taskExecutionId, Value: {@link TaskExecutionSession}
     */
    private final ConcurrentHashMap<Long, TaskExecutionSession> activeSessions =
            new ConcurrentHashMap<>();

    /**
     * Continuously drains the {@link BoundedPriorityTaskQueue} and submits
     * due tasks to the thread pool for execution.
     *
     * <p>Runs every 10 seconds (configurable via {@code app.scheduler.executor-poll-ms}).</p>
     */
    @Scheduled(fixedDelayString = "${app.scheduler.executor-poll-ms:10000}")
    public void drainQueueAndExecute() {
        if (taskQueue.isEmpty()) {
            return;
        }

        log.info("TaskExecutorEngine: Draining queue. Current size: {}", taskQueue.size());

        QueuedTask queuedTask;
        while ((queuedTask = taskQueue.poll()) != null) {
            final QueuedTask taskToExecute = queuedTask;
            taskExecutor.execute(() -> executeTask(taskToExecute));
        }
    }

    /**
     * Executes a single task by delegating to the appropriate {@link TaskHandler}.
     *
     * <p>Creates a database execution record, registers an in-memory session,
     * then resolves and delegates to the correct handler via {@link TaskHandlerRegistry}.
     * Handles success and failure outcomes accordingly.</p>
     *
     * @param queuedTask the task dequeued from the {@link BoundedPriorityTaskQueue}
     */
    private void executeTask(QueuedTask queuedTask) {
        log.info("TaskExecutorEngine: Executing task [{}] '{}' | Attempt: {}",
                queuedTask.getTaskId(), queuedTask.getTaskName(), queuedTask.getRetryCount() + 1);

        Optional<Task> taskOptional = taskRepository.findById(queuedTask.getTaskId());
        if (taskOptional.isEmpty()) {
            log.error("TaskExecutorEngine: Task [{}] not found. Skipping.", queuedTask.getTaskId());
            return;
        }

        Task task = taskOptional.get();
        TaskExecution taskExecution = resolveTaskExecution(task, queuedTask);
        if (taskExecution == null) {
            return;
        }
        registerSession(taskExecution, task, queuedTask.getRetryCount());

        int attemptNo = queuedTask.getRetryCount() + 1;

        try {
            TaskHandler handler = taskHandlerRegistry.getHandler(task.getType());
            handler.execute(task, taskExecution);

            logAttempt(taskExecution, ExecutionStatus.COMPLETED, null, attemptNo);
            markExecutionCompleted(taskExecution);
            removeSession(taskExecution.getId());

            log.info("TaskExecutorEngine: Task [{}] '{}' completed successfully on attempt {}.",
                    task.getId(), task.getName(), attemptNo);

        } catch (Exception ex) {
            handleExecutionFailure(task, taskExecution, queuedTask, ex);
        }
    }

    /**
     * Returns the execution row this attempt belongs to.
     *
     * <p>A fresh run creates one. A retry loads the row the run already owns and
     * advances its {@code retryCount}, so all attempts of a run share a single
     * {@code task_executions} record and the per-attempt detail lives in
     * {@code task_execution_logs}.</p>
     *
     * @param task       the task being executed
     * @param queuedTask the dequeued task
     * @return the execution record, or {@code null} if a retry's execution row has vanished
     */
    private TaskExecution resolveTaskExecution(Task task, QueuedTask queuedTask) {
        if (queuedTask.getTaskExecutionId() == null) {
            return createTaskExecution(task, queuedTask.getRetryCount());
        }

        Optional<TaskExecution> existing =
                taskExecutionRepository.findById(queuedTask.getTaskExecutionId());

        if (existing.isEmpty()) {
            log.error("TaskExecutorEngine: Task [{}] retry references execution [{}], which no longer "
                            + "exists. Skipping.",
                    task.getId(), queuedTask.getTaskExecutionId());
            return null;
        }

        TaskExecution taskExecution = existing.get();
        // Handlers read attemptNo off retryCount, so it must reflect this attempt.
        taskExecution.setRetryCount(queuedTask.getRetryCount());
        taskExecution.setStatus(ExecutionStatus.IN_PROGRESS);
        return taskExecutionRepository.save(taskExecution);
    }

    /**
     * Handles a task execution failure by logging the attempt and
     * either scheduling a retry or moving the task to the DLQ.
     *
     * @param task          the task that failed
     * @param taskExecution the current execution record
     * @param queuedTask    the original queued task
     * @param ex            the exception that caused the failure
     */
    private void handleExecutionFailure(Task task, TaskExecution taskExecution,
                                        QueuedTask queuedTask, Exception ex) {
        int attemptNo = queuedTask.getRetryCount() + 1;
        log.error("TaskExecutorEngine: Task [{}] '{}' failed on attempt {}. Reason: {}",
                task.getId(), task.getName(), attemptNo, ex.getMessage());

        logAttempt(taskExecution, ExecutionStatus.FAILED, ex.getMessage(), attemptNo);
        removeSession(taskExecution.getId());

        if (queuedTask.getRetryCount() < maxRetries - 1) {
            // The run is not over, so the execution row stays IN_PROGRESS. Marking it
            // FAILED here would make a run that later succeeds look like it failed —
            // and IN_PROGRESS is also what lets the retry queue be rebuilt after a
            // crash (see RetryQueueEngine), since a FAILED row means retries exhausted.
            enqueueRetry(task, queuedTask, taskExecution.getId());
        } else {
            markExecutionFailed(taskExecution);
            moveTaskToDlq(task, taskExecution, ex.getMessage());
        }
    }

    /**
     * Parks a failed attempt in the {@link RetryQueue} to be re-queued once its
     * delay elapses, then returns immediately — the calling pool thread is freed
     * rather than sleeping out the delay.
     *
     * <p>The delay is enforced by {@link RetryQueueEngine}, which sweeps the retry
     * queue and moves due entries onto the main queue via
     * {@link BoundedPriorityTaskQueue#offerRetry(QueuedTask)}. That path bypasses
     * the queue's duplicate-by-taskId check because a retry is a deliberate
     * re-execution and must not be mistaken for a duplicate of a fresh run; the
     * sweep does its own collision check before re-queueing.</p>
     *
     * <p>The retry carries an incremented retry count and the id of the execution
     * row this run already owns, so every attempt of a run stays on a single row.</p>
     *
     * @param task            the task to retry
     * @param queuedTask      the failed queued task
     * @param taskExecutionId the execution row this run already owns
     */
    private void enqueueRetry(Task task, QueuedTask queuedTask, Long taskExecutionId) {
        int nextRetryCount = queuedTask.getRetryCount() + 1;
        LocalDateTime nextRetryTime = LocalDateTime.now().plus(Duration.ofMillis(retryDelayMs));
        RetryEntry entry = new RetryEntry(task.getId(), taskExecutionId, nextRetryCount, nextRetryTime);

        boolean parked = retryQueue.schedule(entry);
        if (parked) {
            log.info("TaskExecutorEngine: Task [{}] parked for retry {} at {}.",
                    task.getId(), nextRetryCount, nextRetryTime);
        } else {
            // A retry is already pending for this task — should not normally happen
            // for a run in flight, but if it does, the existing one stands.
            log.warn("TaskExecutorEngine: Task [{}] already has a pending retry; "
                    + "not scheduling another.", task.getId());
        }
    }

    /**
     * Returns {@code true} if the given task currently has an execution in flight.
     *
     * <p>The active-session map is keyed by execution id, so this scans the live
     * sessions for a matching task id. Used by the retry sweep to avoid launching
     * a retry while a fresh run of the same task is already executing.</p>
     *
     * @param taskId the task id to check
     * @return {@code true} if an execution for the task is currently active
     */
    public boolean isTaskActive(Long taskId) {
        return activeSessions.values().stream()
                .anyMatch(s -> taskId.equals(s.getTaskId()));
    }

    /**
     * Moves a task that has exhausted all retries to the Dead Letter Queue.
     *
     * @param task          the failed task
     * @param taskExecution the final execution record
     * @param failureReason the reason for the final failure
     */
    private void moveTaskToDlq(Task task, TaskExecution taskExecution, String failureReason) {
        log.error("TaskExecutorEngine: Task [{}] '{}' exhausted all retries. Moving to DLQ.",
                task.getId(), task.getName());

        TaskDlq dlqEntry = TaskDlq.builder()
                .task(task)
                .taskExecution(taskExecution)
                .failureReason(failureReason)
                .status(DlqStatus.NEW)
                .build();
        taskDlqRepository.save(dlqEntry);
    }

    /**
     * Dead-letters a run whose retries were spent by crashes rather than clean failures.
     *
     * <p>Used by {@link RetryQueueEngine} on startup. An interrupted {@code IN_PROGRESS}
     * attempt with no retries left never reached {@link #handleExecutionFailure} — the
     * process died mid-run — so this is the only path that can move it to the DLQ instead
     * of letting it be re-run forever. Records the interrupted attempt as {@code FAILED},
     * marks the execution {@code FAILED}, and writes the DLQ entry, mirroring the terminal
     * branch of a normal failure.</p>
     *
     * @param task          the owning task
     * @param taskExecution the interrupted execution row
     * @param attemptNo     the attempt that was interrupted ({@code retryCount + 1})
     * @param failureReason reason recorded on both the attempt log and the DLQ entry
     */
    public void deadLetterInterrupted(Task task, TaskExecution taskExecution,
                                      int attemptNo, String failureReason) {
        logAttempt(taskExecution, ExecutionStatus.FAILED, failureReason, attemptNo);
        markExecutionFailed(taskExecution);
        moveTaskToDlq(task, taskExecution, failureReason);
    }

    /**
     * Creates and persists a new {@link TaskExecution} record.
     *
     * @param task       the task being executed
     * @param retryCount the current retry count
     * @return the persisted execution record
     */
    private TaskExecution createTaskExecution(Task task, int retryCount) {
        TaskExecution execution = TaskExecution.builder()
                .task(task)
                .status(ExecutionStatus.IN_PROGRESS)
                .retryCount(retryCount)
                .startedAt(LocalDateTime.now())
                .build();
        return taskExecutionRepository.save(execution);
    }

    /**
     * Registers an in-memory session for the given execution.
     *
     * @param taskExecution the execution record
     * @param task          the task being executed
     * @param retryCount    the current retry count
     */
    private void registerSession(TaskExecution taskExecution, Task task, int retryCount) {
        TaskExecutionSession session = TaskExecutionSession.builder()
                .taskExecutionId(taskExecution.getId())
                .taskId(task.getId())
                .taskName(task.getName())
                .threadId(Thread.currentThread().getId())
                .status(ExecutionStatus.IN_PROGRESS)
                .retryCount(retryCount)
                .startedAt(LocalDateTime.now())
                .build();
        activeSessions.put(taskExecution.getId(), session);
    }

    /**
     * Removes the in-memory session for the given execution ID.
     *
     * @param taskExecutionId the execution ID to remove
     */
    private void removeSession(Long taskExecutionId) {
        activeSessions.remove(taskExecutionId);
    }

    /**
     * Marks a task execution as completed in the database.
     *
     * @param taskExecution the execution record to update
     */
    private void markExecutionCompleted(TaskExecution taskExecution) {
        taskExecution.setStatus(ExecutionStatus.COMPLETED);
        taskExecution.setCompletedAt(LocalDateTime.now());
        taskExecutionRepository.save(taskExecution);
    }

    /**
     * Marks a task execution as failed in the database.
     *
     * @param taskExecution the execution record to update
     */
    private void markExecutionFailed(TaskExecution taskExecution) {
        taskExecution.setStatus(ExecutionStatus.FAILED);
        taskExecution.setCompletedAt(LocalDateTime.now());
        taskExecutionRepository.save(taskExecution);
    }

    /**
     * Logs a single execution attempt in the {@code task_execution_logs} table.
     *
     * <p>Written for every attempt, successful or not. A {@link TaskExecution} is one
     * row per run; this table is one row per attempt within it. A task that fails twice
     * and succeeds on the third attempt leaves one {@code COMPLETED} execution and three
     * logs, so the run reads as a single event while the attempts that got it there
     * remain inspectable.</p>
     *
     * @param taskExecution the execution record this attempt belongs to
     * @param status        the outcome of this attempt
     * @param message       the failure reason, or {@code null} for a successful attempt
     * @param attemptNo     the attempt number, starting at 1
     */
    private void logAttempt(TaskExecution taskExecution, ExecutionStatus status,
                            String message, int attemptNo) {
        TaskExecutionLog executionLog = TaskExecutionLog.builder()
                .taskExecution(taskExecution)
                .status(status)
                .attemptNo(attemptNo)
                .message(message)
                .build();
        taskExecutionLogRepository.save(executionLog);
    }

    /**
     * Returns all currently active task execution sessions.
     *
     * @return the active sessions map
     */
    public ConcurrentHashMap<Long, TaskExecutionSession> getActiveSessions() {
        return activeSessions;
    }
}