package com.taskscheduler.dto.response;

public record AnalyticsResponse(
        long totalTasks,
        long totalExecutions,
        long successfulExecutions,
        long failedExecutions,
        long totalEmailsSent,
        long successfulEmails,
        long failedEmails,
        long totalDlqEntries
) {}
