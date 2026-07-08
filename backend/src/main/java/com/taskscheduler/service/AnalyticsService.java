package com.taskscheduler.service;

import com.taskscheduler.dto.response.AnalyticsResponse;
import com.taskscheduler.enums.ExecutionStatus;
import com.taskscheduler.enums.NotificationStatus;
import com.taskscheduler.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for retrieving task scheduler analytics.
 *
 * <p>Aggregates counts from multiple repositories to produce a summary
 * of task executions, email notifications, and dead letter queue entries.
 * Marked {@link Transactional} with {@code readOnly = true} since this
 * service only performs count queries and never modifies data.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TaskRepository taskRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final EmailNotificationRepository emailNotificationRepository;
    private final TaskDlqRepository taskDlqRepository;

    /**
     * Retrieves aggregated analytics for the task scheduler.
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
