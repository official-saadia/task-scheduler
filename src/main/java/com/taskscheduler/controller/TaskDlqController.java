package com.taskscheduler.controller;

import com.taskscheduler.entity.TaskDlq;
import com.taskscheduler.enums.DlqStatus;
import com.taskscheduler.service.TaskDlqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing the Dead Letter Queue (DLQ).
 *
 * <p>The DLQ holds tasks that have exhausted all retry attempts and require
 * manual investigation. This controller allows developers to review failed
 * tasks and update their investigation status.</p>
 *
 * <p>DLQ status lifecycle:</p>
 * <pre>
 *   NEW → IN_PROGRESS → ANALYSED → FIXED
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/task-dlq")
@RequiredArgsConstructor
@Tag(name = "Dead Letter Queue", description = "APIs for reviewing and managing failed tasks in the Dead Letter Queue")
public class TaskDlqController {

    private final TaskDlqService taskDlqService;

    /**
     * Retrieves all DLQ entries regardless of their status.
     *
     * @return list of all DLQ entries with HTTP 200 status
     */
    @Operation(summary = "Get all DLQ entries", description = "Returns all tasks in the Dead Letter Queue regardless of their investigation status")
    @ApiResponse(responseCode = "200", description = "DLQ entries retrieved successfully")
    @GetMapping
    public ResponseEntity<List<TaskDlq>> getAllDlqEntries() {
        return ResponseEntity.ok(taskDlqService.getAllDlqEntries());
    }

    /**
     * Retrieves a single DLQ entry by its ID.
     *
     * @param id the DLQ entry ID
     * @return the DLQ entry with HTTP 200 status
     */
    @Operation(summary = "Get DLQ entry by ID", description = "Returns a single DLQ entry by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DLQ entry found"),
            @ApiResponse(responseCode = "404", description = "DLQ entry not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskDlq> getDlqEntryById(
            @Parameter(description = "DLQ entry ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(taskDlqService.getDlqEntryById(id));
    }

    /**
     * Retrieves all DLQ entries matching a specific status.
     *
     * @param status the DLQ status to filter by (NEW, IN_PROGRESS, ANALYSED, FIXED)
     * @return list of DLQ entries with the given status with HTTP 200 status
     */
    @Operation(summary = "Get DLQ entries by status", description = "Returns all DLQ entries matching a specific investigation status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DLQ entries retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskDlq>> getDlqEntriesByStatus(
            @Parameter(description = "DLQ status: NEW, IN_PROGRESS, ANALYSED, FIXED", required = true)
            @PathVariable DlqStatus status) {
        return ResponseEntity.ok(taskDlqService.getDlqEntriesByStatus(status));
    }

    /**
     * Updates the investigation status of a DLQ entry.
     *
     * <p>Used by developers to track the progress of manual investigation
     * and resolution of failed tasks. Status should progress in order:
     * NEW → IN_PROGRESS → ANALYSED → FIXED.</p>
     *
     * @param id     the DLQ entry ID to update
     * @param status the new DLQ status to set
     * @return the updated DLQ entry with HTTP 200 status
     */
    @Operation(summary = "Update DLQ entry status", description = "Updates the investigation status of a DLQ entry as it progresses through review")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DLQ entry status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status value"),
            @ApiResponse(responseCode = "404", description = "DLQ entry not found")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskDlq> updateDlqStatus(
            @Parameter(description = "DLQ entry ID", required = true) @PathVariable Long id,
            @Parameter(description = "New status: NEW, IN_PROGRESS, ANALYSED, FIXED", required = true)
            @RequestParam DlqStatus status) {
        return ResponseEntity.ok(taskDlqService.updateDlqStatus(id, status));
    }
}
