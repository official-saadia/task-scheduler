package com.taskscheduler.controller;

import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.enums.ExecutionStatus;
import com.taskscheduler.service.TaskExecutionService;
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
@RequestMapping("/api/v1/task-executions")
@RequiredArgsConstructor
@Tag(name = "Task Executions", description = "Read-only APIs for monitoring task execution history")
public class TaskExecutionController {

    private final TaskExecutionService taskExecutionService;

    @Operation(summary = "Get all task executions with pagination")
    @ApiResponse(responseCode = "200", description = "Task executions retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<TaskExecution>> getAllExecutions(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(taskExecutionService.getAllExecutions(page, size));
    }

    @Operation(summary = "Get task execution by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task execution found"),
            @ApiResponse(responseCode = "404", description = "Task execution not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskExecution> getExecutionById(
            @Parameter(description = "Task execution ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(taskExecutionService.getExecutionById(id));
    }

    @Operation(summary = "Get executions by task ID with pagination")
    @ApiResponse(responseCode = "200", description = "Task executions retrieved successfully")
    @GetMapping("/task/{taskId}")
    public ResponseEntity<Page<TaskExecution>> getExecutionsByTaskId(
            @Parameter(description = "Task ID", required = true) @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(taskExecutionService.getExecutionsByTaskId(taskId, page, size));
    }

    @Operation(summary = "Get executions by status with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task executions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<TaskExecution>> getExecutionsByStatus(
            @Parameter(description = "Execution status", required = true) @PathVariable ExecutionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(taskExecutionService.getExecutionsByStatus(status, page, size));
    }
}
