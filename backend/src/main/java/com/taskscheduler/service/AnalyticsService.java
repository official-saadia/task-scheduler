package com.taskscheduler.service;

import com.taskscheduler.dto.response.AnalyticsResponse;
import com.taskscheduler.dto.response.PeriodStats;
import com.taskscheduler.enums.ExecutionStatus;
import com.taskscheduler.enums.NotificationStatus;
import com.taskscheduler.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

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
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        // Week starts Monday (ISO-8601); with(MONDAY) rewinds to this week's Monday.
        LocalDateTime startOfWeek = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        return new AnalyticsResponse(
                taskRepository.count(),
                taskExecutionRepository.count(),
                taskExecutionRepository.countByStatus(ExecutionStatus.COMPLETED),
                taskExecutionRepository.countByStatus(ExecutionStatus.SKIPPED),
                taskExecutionRepository.countByStatus(ExecutionStatus.FAILED),
                emailNotificationRepository.count(),
                emailNotificationRepository.countByStatus(NotificationStatus.SUCCESS),
                emailNotificationRepository.countByStatus(NotificationStatus.FAILED),
                taskDlqRepository.count(),
                statsSince(startOfDay),
                statsSince(startOfWeek),
                statsSince(startOfMonth)
        );
    }

    /**
     * Builds execution outcome counts for every row created on or after
     * {@code since}, in a single grouped query. Statuses absent from the
     * window default to zero.
     *
     * @param since inclusive lower bound on {@code TaskExecution.createdAt}
     * @return a {@link PeriodStats} for the window
     */
    private PeriodStats statsSince(LocalDateTime since) {
        Map<ExecutionStatus, Long> counts = new EnumMap<>(ExecutionStatus.class);
        for (Object[] row : taskExecutionRepository.countByStatusSince(since)) {
            counts.put((ExecutionStatus) row[0], (Long) row[1]);
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new PeriodStats(
                total,
                counts.getOrDefault(ExecutionStatus.COMPLETED, 0L),
                counts.getOrDefault(ExecutionStatus.SKIPPED, 0L),
                counts.getOrDefault(ExecutionStatus.FAILED, 0L)
        );
    }
}
