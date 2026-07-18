package com.taskscheduler.scheduler;

import com.taskscheduler.entity.Task;
import com.taskscheduler.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler engine responsible for polling the database and queuing tasks for execution.
 *
 * <p>Runs at a fixed interval (configurable via {@code app.scheduler.polling-interval-ms})
 * and selects active tasks whose persisted {@code nextExecutionTime} has passed.</p>
 *
 * <p>Dueness is tracked in the database, not re-derived from the cron expression
 * on every poll. When a task is queued, its {@code nextExecutionTime} is advanced
 * to the following cron occurrence in the same transaction. A task therefore stops
 * being due the instant it is picked up, and each scheduled occurrence produces
 * exactly one execution regardless of how often polling runs.</p>
 *
 * <p>The previous implementation asked {@code cron.next(now.minusMinutes(5)) <= now},
 * which left a task due for a full five minutes. With a ten-second poll that produced
 * roughly thirty executions per intended run — the queue's duplicate check could not
 * prevent it, because {@link TaskExecutorEngine} drains the queue between polls.</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Poll the database for tasks whose next execution time has passed</li>
 *   <li>Initialise {@code nextExecutionTime} for tasks that do not yet have one</li>
 *   <li>Add due tasks to the bounded priority queue</li>
 *   <li>Advance {@code nextExecutionTime} on successfully queued tasks</li>
 *   <li>Log queue state for monitoring</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskSchedulerEngine {

    private final TaskRepository taskRepository;
    private final BoundedPriorityTaskQueue taskQueue;

    /**
     * Polls the database for due tasks and queues them for execution.
     *
     * <p>Scheduled at a fixed delay defined by {@code app.scheduler.polling-interval-ms}.
     * Fixed delay ensures the next poll only starts once the current one completes,
     * preventing overlapping polls.</p>
     *
     * <p>Transactional so that queuing a task and advancing its
     * {@code nextExecutionTime} commit together. Without this, a crash between
     * the two would re-queue the task on the next poll.</p>
     */
    @Transactional
    @Scheduled(fixedDelayString = "${app.scheduler.polling-interval-ms:300000}")
    public void pollAndQueueTasks() {
        LocalDateTime now = LocalDateTime.now();

        log.info("TaskSchedulerEngine: Starting poll at {} | Queue size: {}/{}",
                now, taskQueue.size(), taskQueue.getCapacity());

        List<Task> dueTasks = taskRepository.findDueTasks(now);

        if (dueTasks.isEmpty()) {
            log.info("TaskSchedulerEngine: No tasks due.");
            return;
        }

        int queued = 0;
        int initialised = 0;
        int skipped = 0;

        for (Task task : dueTasks) {
            CronExpression cron = parseCron(task);
            if (cron == null) {
                skipped++;
                continue;
            }

            // A task with no nextExecutionTime has never been scheduled — it was
            // created before this column existed, or by a path that did not set it.
            // Schedule it forward rather than running it now, so an upgrade cannot
            // fire a backlog of "missed" runs at once.
            if (task.getNextExecutionTime() == null) {
                LocalDateTime next = cron.next(now);
                if (next == null) {
                    log.warn("TaskSchedulerEngine: Task [{}] '{}' cron '{}' has no future occurrence.",
                            task.getId(), task.getName(), task.getCronExpression());
                    skipped++;
                    continue;
                }
                task.setNextExecutionTime(next);
                taskRepository.save(task);
                initialised++;
                log.info("TaskSchedulerEngine: Initialised task [{}] '{}' — first run at {}",
                        task.getId(), task.getName(), next);
                continue;
            }

            LocalDateTime dueAt = task.getNextExecutionTime();
            boolean added = taskQueue.offer(new QueuedTask(task));

            if (!added) {
                // Queue full, or the task is still queued from an earlier poll.
                // nextExecutionTime is deliberately left alone so the task stays
                // due and is retried on the next poll rather than being dropped.
                skipped++;
                log.warn("TaskSchedulerEngine: Skipped task [{}] '{}' — queue full or already queued. "
                                + "Still due at {}.",
                        task.getId(), task.getName(), dueAt);
                continue;
            }

            // Advance before the next poll can see this task again. This — not the
            // queue's duplicate check — is what guarantees one run per occurrence.
            LocalDateTime next = cron.next(now);
            task.setLastExecutionTime(now);
            task.setNextExecutionTime(next);
            taskRepository.save(task);
            queued++;

            log.info("TaskSchedulerEngine: Queued task [{}] '{}' due at {} | next run {}",
                    task.getId(), task.getName(), dueAt, next);
        }

        log.info("TaskSchedulerEngine: Poll complete. Queued: {} | Initialised: {} | Skipped: {} | Queue size: {}/{}",
                queued, initialised, skipped, taskQueue.size(), taskQueue.getCapacity());
    }

    /**
     * Parses a task's cron expression, logging and returning {@code null}
     * if it is invalid rather than aborting the whole poll.
     *
     * @param task the task whose cron expression to parse
     * @return the parsed expression, or {@code null} if it cannot be parsed
     */
    private CronExpression parseCron(Task task) {
        try {
            return CronExpression.parse(task.getCronExpression());
        } catch (IllegalArgumentException ex) {
            log.error("TaskSchedulerEngine: Failed to parse cron expression '{}' for task [{}]: {}",
                    task.getCronExpression(), task.getId(), ex.getMessage());
            return null;
        }
    }
}
