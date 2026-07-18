package com.taskscheduler.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A bounded, thread-safe priority queue for managing scheduled tasks.
 *
 * <p>{@link PriorityBlockingQueue} is unbounded by default in Java. This class
 * wraps it with a configurable capacity limit and a {@link ReentrantLock} to
 * enforce the bound during concurrent producer access, making it safe for
 * use in a multi-threaded scheduler environment.</p>
 *
 * <p>Tasks are ordered by their next execution time via {@link QueuedTask#compareTo},
 * ensuring the earliest-due task is always at the head of the queue.</p>
 *
 * <p>Duplicate prevention: tasks already present in the queue by task ID
 * are silently skipped to avoid duplicate execution.</p>
 */
@Component
public class BoundedPriorityTaskQueue {

    private final PriorityBlockingQueue<QueuedTask> queue;
    private final ReentrantLock lock = new ReentrantLock();
    private final int capacity;

    /**
     * Constructs a {@code BoundedPriorityTaskQueue} with a configurable capacity.
     *
     * @param capacity the maximum number of tasks the queue can hold,
     *                 loaded from {@code app.scheduler.queue-capacity} in {@code application.yml}
     */
    public BoundedPriorityTaskQueue(@Value("${app.scheduler.queue-capacity:500}") int capacity) {
        this.capacity = capacity;
        this.queue = new PriorityBlockingQueue<>(capacity);
    }

    /**
     * Attempts to add a task to the queue if the queue is not full
     * and the task is not already present.
     *
     * <p>Uses a {@link ReentrantLock} to safely enforce the capacity bound
     * under concurrent producer access.</p>
     *
     * @param task the queued task to add
     * @return {@code true} if the task was added, {@code false} if the queue
     *         is full or the task is already present
     */
    public boolean offer(QueuedTask task) {
        lock.lock();
        try {
            if (queue.size() >= capacity) {
                return false;
            }
            boolean alreadyQueued = queue.stream()
                    .anyMatch(q -> q.getTaskId().equals(task.getTaskId()));
            if (alreadyQueued) {
                return false;
            }
            return queue.offer(task);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds a retry task to the queue, bypassing the duplicate-by-taskId check.
     *
     * <p>Retry tasks are intentional re-executions following a failed attempt
     * and must not be rejected as duplicates of a fresh task with the same
     * {@code taskId} that may have been queued by {@link TaskSchedulerEngine}
     * in the interim (e.g., for frequently-scheduled tasks during testing).</p>
     *
     * <p>Only the capacity bound is enforced. If the queue is full, the retry
     * is dropped and logged by the caller.</p>
     *
     * @param task the retry task to add, with an incremented retry count
     * @return {@code true} if the retry was added, {@code false} if the queue is full
     */
    public boolean offerRetry(QueuedTask task) {
        lock.lock();
        try {
            if (queue.size() >= capacity) {
                return false;
            }
            return queue.offer(task);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves and removes the highest-priority task from the queue,
     * or returns {@code null} if the queue is empty.
     *
     * @return the highest-priority {@link QueuedTask}, or {@code null} if empty
     */
    public QueuedTask poll() {
        return queue.poll();
    }

    /**
     * Returns {@code true} if a task with the given id is currently queued.
     *
     * <p>Used by the retry sweep to avoid re-queueing a retry for a task that
     * already has a fresh run waiting — {@link #offerRetry(QueuedTask)}
     * deliberately bypasses the duplicate check, so the caller guards against
     * that collision itself. A weakly-consistent read against the live queue;
     * a task can still be polled the instant after this returns, so it narrows
     * the double-run window rather than closing it outright.</p>
     *
     * @param taskId the task id to look for
     * @return {@code true} if the task is present in the queue
     */
    public boolean contains(Long taskId) {
        return queue.stream().anyMatch(q -> q.getTaskId().equals(taskId));
    }

    /**
     * Returns the current number of tasks in the queue.
     *
     * @return the queue size
     */
    public int size() {
        return queue.size();
    }

    /**
     * Returns {@code true} if the queue contains no tasks.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Returns the configured maximum capacity of the queue.
     *
     * @return the queue capacity
     */
    public int getCapacity() {
        return capacity;
    }
}