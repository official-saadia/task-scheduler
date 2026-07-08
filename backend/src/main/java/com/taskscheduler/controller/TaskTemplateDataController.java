package com.taskscheduler.controller;

import com.taskscheduler.dto.request.CreateTaskTemplateDataRequest;
import com.taskscheduler.dto.request.UpdateTaskTemplateDataRequest;
import com.taskscheduler.entity.TaskTemplateData;
import com.taskscheduler.service.TaskTemplateDataService;
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

/**
 * REST controller for managing task template data.
 *
 * <p>Task template data associates a task with a template and provides
 * the runtime JSON payload used during template resolution at execution time.</p>
 */
@RestController
@RequestMapping("/api/v1/task-template-data")
@RequiredArgsConstructor
@Tag(name = "Task Template Data", description = "APIs for managing task template runtime data")
public class TaskTemplateDataController {

    private final TaskTemplateDataService taskTemplateDataService;

    /**
     * Creates a new task template data record.
     *
     * @param request the creation request containing taskId, templateId, and JSON data
     * @return the created task template data with HTTP 201 status
     */
    @Operation(summary = "Create task template data",
            description = "Associates a task with a template and provides the JSON payload for template resolution")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task template data created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Task or template not found")
    })
    @PostMapping
    public ResponseEntity<TaskTemplateData> create(
            @Valid @RequestBody CreateTaskTemplateDataRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskTemplateDataService.create(request));
    }

    /**
     * Retrieves the template data for a specific task.
     *
     * @param taskId the task ID to retrieve template data for
     * @return the task template data with HTTP 200 status
     */
    @Operation(summary = "Get template data by task ID",
            description = "Returns the template data associated with a specific task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task template data found"),
            @ApiResponse(responseCode = "404", description = "Task template data not found")
    })
    @GetMapping("/task/{taskId}")
    public ResponseEntity<TaskTemplateData> getByTaskId(
            @Parameter(description = "Task ID", required = true) @PathVariable Long taskId) {
        return ResponseEntity.ok(taskTemplateDataService.getByTaskId(taskId));
    }

    /**
     * Updates the JSON data of an existing task template data record.
     *
     * @param id      the task template data ID to update
     * @param request the update request containing the new JSON payload
     * @return the updated task template data with HTTP 200 status
     */
    @Operation(summary = "Update task template data",
            description = "Updates the JSON payload of an existing task template data record")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task template data updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Task template data not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TaskTemplateData> update(
            @Parameter(description = "Task template data ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdateTaskTemplateDataRequest request) {
        return ResponseEntity.ok(taskTemplateDataService.updateData(id, request.data()));
    }

    /**
     * Deletes a task template data record by its ID.
     *
     * @param id the task template data ID to delete
     * @return HTTP 204 No Content on success
     */
    @Operation(summary = "Delete task template data",
            description = "Permanently deletes a task template data record")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task template data deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Task template data not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Task template data ID", required = true) @PathVariable Long id) {
        taskTemplateDataService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
