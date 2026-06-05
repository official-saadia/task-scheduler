package com.taskscheduler.service;

import com.taskscheduler.dto.response.AnalyticsResponse;
import com.taskscheduler.enums.ExecutionStatus;
import com.taskscheduler.enums.NotificationStatus;
import com.taskscheduler.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service layer for retrieving task scheduler analytics.
 * Aggregates counts from multiple repositories to produce a summary
 * of task executions, email notifications, and dead letter queue entries.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TaskRepository taskRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final EmailNotificationRepository emailNotificationRepository;
    private final TaskDlqRepository taskDlqRepository;

    /**
     * Retrieves aggregated analytics for the task scheduler.
     * Queries the database for counts of tasks, executions by status,
     * email notifications by status, and total DLQ entries.
     *
     * @return an {@link AnalyticsResponse} containing aggregated statistics
     */
    public AnalyticsResponse getAnalytics() {
        return new AnalyticsResponse(
                taskRepository.count(),
                taskExecutionRepository.count(),
                taskExecutionRepository.countByStatus(ExecutionStatus.COMPLETED),
                taskExecutionRepository.countByStatus(ExecutionStatus.FAILED),
                emailNotificationRepository.count(),
                emailNotificationRepository.countByStatus(NotificationStatus.SUCCESS),
                emailNotificationRepository.countByStatus(NotificationStatus.FAILED),
                taskDlqRepository.count()
        );
    }
}
