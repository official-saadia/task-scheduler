package com.taskscheduler.controller;

import com.taskscheduler.dto.request.CreateTaskRequest;
import com.taskscheduler.dto.response.TaskResponse;
import com.taskscheduler.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing scheduled tasks.
 * Provides endpoints to create, retrieve, and deactivate tasks.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "APIs for managing scheduled tasks")
public class TaskController {

    private final TaskService taskService;

    /**
     * Creates a new scheduled task.
     *
     * @param request the task creation request containing name, type, cron expression, and SMTP config ID
     * @return the created task with HTTP 201 status
     */
    @Operation(summary = "Create a new task", description = "Creates a new scheduled task with a cron expression and associated SMTP configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "SMTP configuration not found")
    })
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    /**
     * Retrieves all tasks.
     *
     * @return list of all tasks with HTTP 200 status
     */
    @Operation(summary = "Get all tasks", description = "Returns a list of all scheduled tasks")
    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    /**
     * Retrieves a task by its ID.
     *
     * @param id the task ID
     * @return the task with HTTP 200 status
     */
    @Operation(summary = "Get task by ID", description = "Returns a single task by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(
            @Parameter(description = "Task ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    /**
     * Deactivates a task by its ID.
     * Deactivated tasks will no longer be picked up by the scheduler engine.
     *
     * @param id the task ID to deactivate
     * @return the updated task with HTTP 200 status
     */
    @Operation(summary = "Deactivate a task", description = "Deactivates a task so it is no longer picked up by the scheduler")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<TaskResponse> deactivateTask(
            @Parameter(description = "Task ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(taskService.deactivateTask(id));
    }
}
