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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for querying task execution records.
 *
 * <p>Task execution records are created and managed by the scheduler engine.
 * This controller exposes read-only endpoints for monitoring and auditing
 * task execution history.</p>
 */
@RestController
@RequestMapping("/api/v1/task-executions")
@RequiredArgsConstructor
@Tag(name = "Task Executions", description = "Read-only APIs for monitoring task execution history")
public class TaskExecutionController {

    private final TaskExecutionService taskExecutionService;

    /**
     * Retrieves all task execution records.
     *
     * @return list of all task executions with HTTP 200 status
     */
    @Operation(summary = "Get all task executions", description = "Returns a list of all task execution records across all tasks")
    @ApiResponse(responseCode = "200", description = "Task executions retrieved successfully")
    @GetMapping
    public ResponseEntity<List<TaskExecution>> getAllExecutions() {
        return ResponseEntity.ok(taskExecutionService.getAllExecutions());
    }

    /**
     * Retrieves a single task execution record by its ID.
     *
     * @param id the task execution ID
     * @return the task execution record with HTTP 200 status
     */
    @Operation(summary = "Get task execution by ID", description = "Returns a single task execution record by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task execution found"),
            @ApiResponse(responseCode = "404", description = "Task execution not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskExecution> getExecutionById(
            @Parameter(description = "Task execution ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(taskExecutionService.getExecutionById(id));
    }

    /**
     * Retrieves all execution records for a specific task.
     *
     * @param taskId the task ID to retrieve executions for
     * @return list of execution records for the given task with HTTP 200 status
     */
    @Operation(summary = "Get executions by task ID", description = "Returns all execution records for a specific task including retries")
    @ApiResponse(responseCode = "200", description = "Task executions retrieved successfully")
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskExecution>> getExecutionsByTaskId(
            @Parameter(description = "Task ID", required = true) @PathVariable Long taskId) {
        return ResponseEntity.ok(taskExecutionService.getExecutionsByTaskId(taskId));
    }

    /**
     * Retrieves all task execution records matching a specific status.
     *
     * @param status the execution status to filter by (PENDING, IN_PROGRESS, COMPLETED, PARTIAL_SUCCESS, FAILED)
     * @return list of task executions with the given status with HTTP 200 status
     */
    @Operation(summary = "Get executions by status", description = "Returns all task execution records matching a specific status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task executions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskExecution>> getExecutionsByStatus(
            @Parameter(description = "Execution status: PENDING, IN_PROGRESS, COMPLETED, PARTIAL_SUCCESS, FAILED", required = true)
            @PathVariable ExecutionStatus status) {
        return ResponseEntity.ok(taskExecutionService.getExecutionsByStatus(status));
    }
}
