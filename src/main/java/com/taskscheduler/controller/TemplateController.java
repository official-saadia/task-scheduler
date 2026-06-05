package com.taskscheduler.controller;

import com.taskscheduler.dto.request.CreateTemplateRequest;
import com.taskscheduler.dto.response.TemplateResponse;
import com.taskscheduler.service.TemplateService;
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
 * REST controller for managing email templates.
 * Templates support dynamic placeholders that are resolved at task execution time.
 */
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "APIs for managing reusable email templates")
public class TemplateController {

    private final TemplateService templateService;

    /**
     * Creates a new email template.
     * Templates can contain dynamic placeholders in the format {{placeholderName}}.
     *
     * @param request the template creation request containing name and template body
     * @return the created template with HTTP 201 status
     */
    @Operation(summary = "Create a new template", description = "Creates a reusable email template with optional dynamic placeholders e.g. {{name}}")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Template created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(request));
    }

    /**
     * Retrieves all active templates.
     *
     * @return list of active templates with HTTP 200 status
     */
    @Operation(summary = "Get all active templates", description = "Returns a list of all active email templates")
    @ApiResponse(responseCode = "200", description = "Templates retrieved successfully")
    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    /**
     * Retrieves a template by its ID.
     *
     * @param id the template ID
     * @return the template with HTTP 200 status
     */
    @Operation(summary = "Get template by ID", description = "Returns a single template by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template found"),
            @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getTemplateById(
            @Parameter(description = "Template ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    /**
     * Deactivates a template by its ID.
     *
     * @param id the template ID to deactivate
     * @return the updated template with HTTP 200 status
     */
    @Operation(summary = "Deactivate a template", description = "Deactivates a template so it can no longer be used in new tasks")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<TemplateResponse> deactivateTemplate(
            @Parameter(description = "Template ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(templateService.deactivateTemplate(id));
    }
}
