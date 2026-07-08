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
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/task-dlq")
@RequiredArgsConstructor
@Tag(name = "Dead Letter Queue", description = "APIs for reviewing and managing failed tasks in the Dead Letter Queue")
public class TaskDlqController {

    private final TaskDlqService taskDlqService;

    @Operation(summary = "Get all DLQ entries with pagination")
    @ApiResponse(responseCode = "200", description = "DLQ entries retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<TaskDlq>> getAllDlqEntries(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(taskDlqService.getAllDlqEntries(page, size));
    }

    @Operation(summary = "Get DLQ entry by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DLQ entry found"),
            @ApiResponse(responseCode = "404", description = "DLQ entry not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskDlq> getDlqEntryById(
            @Parameter(description = "DLQ entry ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(taskDlqService.getDlqEntryById(id));
    }

    @Operation(summary = "Get DLQ entries by status with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DLQ entries retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<TaskDlq>> getDlqEntriesByStatus(
            @Parameter(description = "DLQ status", required = true) @PathVariable DlqStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(taskDlqService.getDlqEntriesByStatus(status, page, size));
    }

    @Operation(summary = "Update DLQ entry status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DLQ entry status updated successfully"),
            @ApiResponse(responseCode = "404", description = "DLQ entry not found")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskDlq> updateDlqStatus(
            @Parameter(description = "DLQ entry ID", required = true) @PathVariable Long id,
            @Parameter(description = "New status", required = true) @RequestParam DlqStatus status) {
        return ResponseEntity.ok(taskDlqService.updateDlqStatus(id, status));
    }
}
