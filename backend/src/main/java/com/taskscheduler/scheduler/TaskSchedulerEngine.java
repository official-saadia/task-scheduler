package com.taskscheduler.scheduler;

import com.taskscheduler.entity.Task;
import com.taskscheduler.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler engine responsible for polling the database and queuing tasks for execution.
 *
 * <p>Runs at a fixed interval (configurable via {@code app.scheduler.polling-interval-ms},
 * default every 5 minutes) and checks all active tasks to determine which ones
 * are due for execution based on their cron expressions.</p>
 *
 * <p>Due tasks are added to the {@link BoundedPriorityTaskQueue} for pickup
 * by the {@link TaskExecutorEngine}. Tasks already present in the queue are
 * skipped to prevent duplicate execution.</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Poll database for all active tasks</li>
 *   <li>Check cron expression against current time</li>
 *   <li>Add due tasks to the bounded priority queue</li>
 *   <li>Skip tasks already queued</li>
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
     * Polls the database for active tasks and queues those that are due for execution.
     *
     * <p>Scheduled to run at a fixed delay defined by {@code app.scheduler.polling-interval-ms}
     * (default: 300000ms = 5 minutes). Fixed delay ensures the next poll only starts
     * after the current poll completes, preventing overlapping polls.</p>
     *
     * <p>For each active task, the cron expression is evaluated against the current
     * time window. If the task is due, it is offered to the {@link BoundedPriorityTaskQueue}.
     * If the queue is full or the task is already queued, it is skipped.</p>
     */
    @Scheduled(fixedDelayString = "${app.scheduler.polling-interval-ms:300000}")
    public void pollAndQueueTasks() {
        log.info("TaskSchedulerEngine: Starting poll at {} | Queue size: {}/{}",
                LocalDateTime.now(), taskQueue.size(), taskQueue.getCapacity());

        List<Task> activeTasks = taskRepository.findAllByIsActiveTrue();

        if (activeTasks.isEmpty()) {
            log.info("TaskSchedulerEngine: No active tasks found.");
            return;
        }

        int queued = 0;
        int skipped = 0;

        for (Task task : activeTasks) {
            if (isDueForExecution(task)) {
                QueuedTask queuedTask = new QueuedTask(task);
                boolean added = taskQueue.offer(queuedTask);
                if (added) {
                    queued++;
                    log.info("TaskSchedulerEngine: Queued task [{}] '{}' scheduled at {}",
                            task.getId(), task.getName(), queuedTask.getNextExecutionTime());
                } else {
                    skipped++;
                    log.warn("TaskSchedulerEngine: Skipped task [{}] '{}' — queue full or already queued.",
                            task.getId(), task.getName());
                }
            }
        }

        log.info("TaskSchedulerEngine: Poll complete. Queued: {} | Skipped: {} | Queue size: {}/{}",
                queued, skipped, taskQueue.size(), taskQueue.getCapacity());
    }

    /**
     * Determines whether a task is due for execution based on its cron expression.
     *
     * <p>Checks if the task's next scheduled execution time falls within
     * the current polling window (i.e., between now and the next poll interval).
     * This ensures tasks are not missed if the poll runs slightly late.</p>
     *
     * @param task the task to evaluate
     * @return {@code true} if the task is due for execution, {@code false} otherwise
     */
    private boolean isDueForExecution(Task task) {
        try {
            CronExpression cron = CronExpression.parse(task.getCronExpression());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextExecution = cron.next(now.minusMinutes(5));
            return nextExecution != null && !nextExecution.isAfter(now);
        } catch (Exception ex) {
            log.error("TaskSchedulerEngine: Failed to parse cron expression for task [{}]: {}",
                    task.getId(), ex.getMessage());
            return false;
        }
    }
}
