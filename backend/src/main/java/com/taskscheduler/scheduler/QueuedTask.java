package com.taskscheduler.scheduler;

import com.taskscheduler.entity.Task;
import lombok.Getter;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;

/**
 * Represents a task that has been queued for execution in the scheduler engine.
 *
 * <p>Implements {@link Comparable} to enable priority ordering in the
 * {@link BoundedPriorityTaskQueue}. Tasks are ordered by their next
 * scheduled execution time — earlier execution times have higher priority.</p>
 *
 * <p>If two tasks share the same execution time, the one created earlier
 * takes priority.</p>
 */
@Getter
public class QueuedTask implements Comparable<QueuedTask> {

    private final Long taskId;
    private final String taskName;
    private final String cronExpression;
    private final LocalDateTime nextExecutionTime;
    private final LocalDateTime createdAt;
    private final int retryCount;

    /**
     * The {@code task_executions} row this attempt belongs to, or {@code null} for a
     * fresh run that has not created one yet.
     *
     * <p>A retry carries the id forward so the whole run — every attempt — stays on a
     * single execution row, rather than each attempt minting its own.</p>
     */
    private final Long taskExecutionId;

    /**
     * Constructs a {@code QueuedTask} from a {@link Task} entity.
     * Calculates the next execution time from the task's cron expression.
     *
     * @param task the task entity to queue
     */
    public QueuedTask(Task task) {
        this.taskId = task.getId();
        this.taskName = task.getName();
        this.cronExpression = task.getCronExpression();
        this.nextExecutionTime = resolveNextExecutionTime(task.getCronExpression());
        this.createdAt = task.getCreatedAt();
        this.retryCount = 0;
        this.taskExecutionId = null;
    }

    /**
     * Constructs a {@code QueuedTask} for a retry attempt.
     *
     * <p>Carries both the retry count and the id of the execution row this run
     * already owns, so the retry continues that run instead of starting a new one.</p>
     *
     * @param task            the task entity to retry
     * @param retryCount      the number of previous failed attempts
     * @param taskExecutionId the execution row this run is already recorded against
     */
    public QueuedTask(Task task, int retryCount, Long taskExecutionId) {
        this.taskId = task.getId();
        this.taskName = task.getName();
        this.cronExpression = task.getCronExpression();
        this.nextExecutionTime = LocalDateTime.now();
        this.createdAt = task.getCreatedAt();
        this.retryCount = retryCount;
        this.taskExecutionId = taskExecutionId;
    }

    /**
     * Resolves the next scheduled execution time from the given cron expression.
     *
     * @param cronExpression the Spring cron expression
     * @return the next execution time, or {@link LocalDateTime#MAX} if it cannot be resolved
     */
    private LocalDateTime resolveNextExecutionTime(String cronExpression) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            LocalDateTime next = cron.next(LocalDateTime.now());
            return next != null ? next : LocalDateTime.MAX;
        } catch (Exception e) {
            return LocalDateTime.MAX;
        }
    }

    /**
     * Compares this task to another by next execution time, then by creation time.
     * Earlier execution times and earlier creation times have higher priority.
     *
     * @param other the other queued task to compare to
     * @return negative if this task has higher priority, positive if lower
     */
    @Override
    public int compareTo(QueuedTask other) {
        int timeComparison = this.nextExecutionTime.compareTo(other.nextExecutionTime);
        if (timeComparison != 0) return timeComparison;
        return this.createdAt.compareTo(other.createdAt);
    }
}
