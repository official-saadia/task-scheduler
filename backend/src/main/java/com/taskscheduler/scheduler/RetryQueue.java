package com.taskscheduler.scheduler;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory holding area for failed attempts waiting out their retry delay.
 *
 * <p>Replaces the old approach of sleeping a pool thread for the retry delay.
 * A failed attempt is parked here as a {@link RetryEntry} and the pool thread
 * is released immediately; {@link RetryQueueEngine} sweeps this queue and moves
 * entries whose delay has elapsed onto the main execution queue. The delay is
 * enforced by a timestamp comparison, not by pinning a thread, so a long delay
 * across many failing tasks no longer starves the pool.</p>
 *
 * <p><b>Keyed by task id.</b> A task can hold at most one pending retry at a
 * time — scheduling a retry for a task that already has one is a no-op. This is
 * the queue's contribution to preventing a task from being retried twice
 * concurrently; the caller still checks the main queue and the active-session
 * map before actually re-queueing, because those can hold the same task from a
 * fresh scheduled run.</p>
 *
 * <p>Backed by a {@link ConcurrentHashMap}: pool threads add entries on failure
 * while the sweep thread drains due ones, with no external locking required.</p>
 */
@Component
public class RetryQueue {

    private final ConcurrentHashMap<Long, RetryEntry> pending = new ConcurrentHashMap<>();

    /**
     * Parks a retry for its task, unless one is already pending for that task.
     *
     * @param entry the retry to hold
     * @return {@code true} if parked, {@code false} if the task already had a
     *         pending retry (this entry is discarded)
     */
    public boolean schedule(RetryEntry entry) {
        return pending.putIfAbsent(entry.taskId(), entry) == null;
    }

    /**
     * Removes and returns every retry whose delay has elapsed as of {@code now}.
     *
     * <p>Entries are removed with a compare-and-remove so a retry re-parked for
     * the same task between the scan and the removal is not lost.</p>
     *
     * @param now the reference time
     * @return the due retries, in no particular order (possibly empty)
     */
    public List<RetryEntry> pollDue(LocalDateTime now) {
        List<RetryEntry> due = new ArrayList<>();
        for (Map.Entry<Long, RetryEntry> e : pending.entrySet()) {
            RetryEntry entry = e.getValue();
            if (entry.isDue(now) && pending.remove(e.getKey(), entry)) {
                due.add(entry);
            }
        }
        return due;
    }

    /**
     * @param taskId the task to check
     * @return {@code true} if a retry is currently pending for the task
     */
    public boolean contains(Long taskId) {
        return pending.containsKey(taskId);
    }

    /**
     * Drops any pending retry for the given task.
     *
     * @param taskId the task whose pending retry to remove
     */
    public void remove(Long taskId) {
        pending.remove(taskId);
    }

    /**
     * @return the number of retries currently waiting
     */
    public int size() {
        return pending.size();
    }
}
