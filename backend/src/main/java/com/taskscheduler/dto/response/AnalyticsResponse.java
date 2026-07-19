package com.taskscheduler.dto.response;

public record AnalyticsResponse(
        long totalTasks,
        long totalExecutions,
        long successfulExecutions,
        long skippedExecutions,
        long failedExecutions,
        long totalEmailsSent,
        long successfulEmails,
        long failedEmails,
        long totalDlqEntries,
        PeriodStats today,
        PeriodStats thisWeek,
        PeriodStats thisMonth
) {}
