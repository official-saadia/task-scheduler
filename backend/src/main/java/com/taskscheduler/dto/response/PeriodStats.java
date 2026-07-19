package com.taskscheduler.dto.response;

/**
 * Execution outcome counts for a single time window (e.g. today, this week,
 * this month). {@code totalExecutions} is every execution row created in the
 * window; the remaining fields are the subset in each terminal outcome.
 */
public record PeriodStats(
        long totalExecutions,
        long successfulExecutions,
        long skippedExecutions,
        long failedExecutions
) {}
