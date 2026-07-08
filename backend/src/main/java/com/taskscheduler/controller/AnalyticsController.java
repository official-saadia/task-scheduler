package com.taskscheduler.controller;

import com.taskscheduler.dto.response.AnalyticsResponse;
import com.taskscheduler.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for retrieving task scheduler analytics.
 * Provides aggregated statistics on task executions and email notifications.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "APIs for retrieving task scheduler analytics and statistics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Retrieves aggregated analytics for the task scheduler.
     * Includes total tasks, execution counts, success and failure rates,
     * email notification stats, and DLQ entry count.
     *
     * @return analytics summary with HTTP 200 status
     */
    @Operation(
            summary = "Get analytics",
            description = "Returns aggregated statistics including total tasks, executions, email notifications sent, success rate, failure rate, and dead letter queue count"
    )
    @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully")
    @GetMapping
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(analyticsService.getAnalytics());
    }
}
