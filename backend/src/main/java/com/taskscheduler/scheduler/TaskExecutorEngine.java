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
        TaskExecution taskExecution = createTaskExecution(task, queuedTask.getRetryCount());
        registerSession(taskExecution, task, queuedTask.getRetryCount());

        try {
            TaskHandler handler = taskHandlerRegistry.getHandler(task.getType());
            handler.execute(task, taskExecution);

            markExecutionCompleted(taskExecution);
            removeSession(taskExecution.getId());

            log.info("TaskExecutorEngine: Task [{}] '{}' completed successfully.",
                    task.getId(), task.getName());

        } catch (Exception ex) {
            handleExecutionFailure(task, taskExecution, queuedTask, ex);
        }
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

        logFailedAttempt(taskExecution, ex.getMessage(), attemptNo);
        markExecutionFailed(taskExecution);
        removeSession(taskExecution.getId());

        if (queuedTask.getRetryCount() < maxRetries - 1) {
            scheduleRetry(task, queuedTask);
        } else {
            moveTaskToDlq(task, taskExecution, ex.getMessage());
        }
    }

    /**
     * Schedules a retry by requeueing the task with an incremented retry count
     * after a configurable delay.
     *
     * <p>Uses {@link BoundedPriorityTaskQueue#offerRetry(QueuedTask)} rather than
     * {@link BoundedPriorityTaskQueue#offer(QueuedTask)} because retries are
     * intentional re-executions and must not be rejected as duplicates of a
     * fresh task with the same {@code taskId} that {@link TaskSchedulerEngine}
     * may have queued in the interim — particularly for frequently-scheduled
     * tasks during testing where polling cycles overlap with retry delays.</p>
     *
     * @param task       the task to retry
     * @param queuedTask the failed queued task
     */
    private void scheduleRetry(Task task, QueuedTask queuedTask) {
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        QueuedTask retryTask = new QueuedTask(task, queuedTask.getRetryCount() + 1);
        boolean requeued = taskQueue.offerRetry(retryTask);
        log.info("TaskExecutorEngine: Task [{}] requeued for retry {}. Success: {}",
                task.getId(), retryTask.getRetryCount(), requeued);

        if (!requeued) {
            log.error("TaskExecutorEngine: Task [{}] retry {} could not be requeued — queue full.",
                    task.getId(), retryTask.getRetryCount());
        }
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
     * Logs a failed execution attempt in the {@code task_execution_logs} table.
     *
     * @param taskExecution the execution record
     * @param message       the failure message
     * @param attemptNo     the attempt number
     */
    private void logFailedAttempt(TaskExecution taskExecution, String message, int attemptNo) {
        TaskExecutionLog executionLog = TaskExecutionLog.builder()
                .taskExecution(taskExecution)
                .status(ExecutionStatus.FAILED)
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