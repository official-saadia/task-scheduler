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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "APIs for managing reusable email templates")
public class TemplateController {

    private final TemplateService templateService;

    @Operation(summary = "Create a new template")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Template created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(request));
    }

    @Operation(summary = "Get all active templates with pagination")
    @ApiResponse(responseCode = "200", description = "Templates retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<TemplateResponse>> getAllTemplates(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(templateService.getAllTemplates(page, size));
    }

    @Operation(summary = "Get template by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template found"),
            @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getTemplateById(
            @Parameter(description = "Template ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    @Operation(summary = "Update a template")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @Parameter(description = "Template ID", required = true) @PathVariable Long id,
            @Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.ok(templateService.updateTemplate(id, request));
    }

    @Operation(summary = "Deactivate a template")
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
