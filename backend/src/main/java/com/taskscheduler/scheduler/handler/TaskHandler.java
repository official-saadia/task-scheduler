package com.taskscheduler.scheduler.handler;

import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.enums.TaskType;

/**
 * Strategy interface for task type execution handlers.
 *
 * <p>Defines the contract that all task type handlers must implement.
 * Each concrete handler is responsible for executing one specific task type
 * (e.g., email notification, database backup, data cleanup).</p>
 *
 * <p>This interface follows the <b>Open/Closed Principle</b> — the
 * {@link com.taskscheduler.scheduler.TaskExecutorEngine} is closed for
 * modification but open for extension. Adding a new task type requires
 * only creating a new {@code TaskHandler} implementation and registering
 * it in the {@link TaskHandlerRegistry}, with zero changes to the engine.</p>
 *
 * <p>Example implementations:</p>
 * <ul>
 *   <li>{@link EmailNotificationTaskHandler} — sends email notifications</li>
 *   <li>{@code DatabaseBackupTaskHandler} — performs database backups (future)</li>
 *   <li>{@code DataCleanupTaskHandler} — cleans up stale records (future)</li>
 * </ul>
 */
public interface TaskHandler {

    /**
     * Executes the task logic for this handler's specific task type.
     *
     * <p>Implementations are responsible for performing the actual task work,
     * persisting relevant records (e.g., {@link com.taskscheduler.entity.EmailNotification}),
     * and throwing a {@link com.taskscheduler.exception.TaskExecutionException}
     * on failure so the executor engine can handle retries and DLQ logic.</p>
     *
     * @param task          the task entity containing configuration and SMTP details
     * @param taskExecution the current execution record to be updated with results
     * @throws com.taskscheduler.exception.TaskExecutionException if execution fails
     */
    void execute(Task task, TaskExecution taskExecution);

    /**
     * Returns the {@link TaskType} this handler is responsible for.
     *
     * <p>Used by {@link TaskHandlerRegistry} to map task types to their
     * corresponding handlers at startup.</p>
     *
     * @return the {@link TaskType} supported by this handler
     */
    TaskType getTaskType();
}
