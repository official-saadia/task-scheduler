package com.taskscheduler.scheduler;

import com.taskscheduler.enums.ExecutionStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents an in-memory session for a task that is currently being executed.
 *
 * <p>Stored in the {@link java.util.concurrent.ConcurrentHashMap} within
 * {@link TaskExecutorEngine} to track active task executions. Each session
 * mirrors the key fields of the {@link com.taskscheduler.entity.TaskExecution}
 * database entity, providing a fast in-memory lookup without hitting the database
 * on every status check.</p>
 *
 * <p>Sessions are created when a task starts executing and removed when
 * the task completes, fails permanently, or is moved to the Dead Letter Queue.</p>
 */
@Getter
@Setter
@Builder
public class TaskExecutionSession {

    /** The unique ID of the task execution record in the database. */
    private Long taskExecutionId;

    /** The ID of the task being executed. */
    private Long taskId;

    /** The name of the task, used for logging and debugging. */
    private String taskName;

    /** The ID of the thread executing this task. */
    private long threadId;

    /** The current execution status of this session. */
    private ExecutionStatus status;

    /** The number of retry attempts made so far for this execution. */
    private int retryCount;

    /** The timestamp when this execution session started. */
    private LocalDateTime startedAt;

    /** The timestamp when this execution session completed or failed permanently. */
    private LocalDateTime completedAt;

    /** The reason for failure, populated on the last failed attempt. */
    private String failureReason;
}
