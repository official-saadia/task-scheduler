package com.taskscheduler.controller;

import com.taskscheduler.dto.request.CreateTemplateMapperRequest;
import com.taskscheduler.entity.TemplateMapper;
import com.taskscheduler.service.TemplateMapperService;
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
 * REST controller for managing template mappers.
 *
 * <p>Template mappers define the mapping between template placeholders
 * and their actual runtime values for a specific task. For example,
 * a template containing {@code {{recipientName}}} requires a mapper entry
 * with {@code keyName = "recipientName"} and the corresponding value.</p>
 */
@RestController
@RequestMapping("/api/v1/template-mappers")
@RequiredArgsConstructor
@Tag(name = "Template Mappers", description = "APIs for managing template placeholder mappings for tasks")
public class TemplateMapperController {

    private final TemplateMapperService templateMapperService;

    /**
     * Creates a new template mapper entry.
     *
     * @param request the mapper creation request containing taskId, templateId, keyName, and value
     * @return the created template mapper with HTTP 201 status
     */
    @Operation(summary = "Create template mapper", description = "Creates a new placeholder-to-value mapping for a task and template")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Template mapper created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Task or template not found")
    })
    @PostMapping
    public ResponseEntity<TemplateMapper> createTemplateMapper(
            @Valid @RequestBody CreateTemplateMapperRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateMapperService.createTemplateMapper(request));
    }

    /**
     * Retrieves all template mapper entries for a specific task.
     *
     * @param taskId the task ID to retrieve mappers for
     * @return list of template mappers for the given task with HTTP 200 status
     */
    @Operation(summary = "Get mappers by task ID", description = "Returns all template mapper entries associated with a specific task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template mappers retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TemplateMapper>> getMappersByTaskId(
            @Parameter(description = "Task ID", required = true) @PathVariable Long taskId) {
        return ResponseEntity.ok(templateMapperService.getMappersByTaskId(taskId));
    }

    /**
     * Deletes a template mapper entry by its ID.
     *
     * @param id the template mapper ID to delete
     * @return HTTP 204 No Content on success
     */
    @Operation(summary = "Delete template mapper", description = "Permanently deletes a template mapper entry by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Template mapper deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Template mapper not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMapper(
            @Parameter(description = "Template mapper ID", required = true) @PathVariable Long id) {
        templateMapperService.deleteMapper(id);
        return ResponseEntity.noContent().build();
    }
}
