package com.taskscheduler.controller;

import com.taskscheduler.dto.request.CreateReportGenerationConfigurationRequest;
import com.taskscheduler.entity.ReportGenerationConfiguration;
import com.taskscheduler.service.ReportGenerationConfigurationService;
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
@RequestMapping("/api/v1/report-configurations")
@RequiredArgsConstructor
@Tag(name = "Report Configurations", description = "APIs for managing report-generation trigger configurations")
public class ReportGenerationConfigurationController {

    private final ReportGenerationConfigurationService reportGenerationConfigurationService;

    @Operation(summary = "Create report generation configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Report configuration created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<ReportGenerationConfiguration> createConfiguration(
            @Valid @RequestBody CreateReportGenerationConfigurationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportGenerationConfigurationService.createConfiguration(request));
    }

    @Operation(summary = "Get all report generation configurations with pagination")
    @ApiResponse(responseCode = "200", description = "Report configurations retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<ReportGenerationConfiguration>> getAllConfigurations(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reportGenerationConfigurationService.getAllConfigurations(page, size));
    }

    @Operation(summary = "Get report generation configuration by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report configuration found"),
            @ApiResponse(responseCode = "404", description = "Report configuration not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReportGenerationConfiguration> getConfigurationById(
            @Parameter(description = "Report configuration ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(reportGenerationConfigurationService.getConfigurationById(id));
    }

    @Operation(summary = "Update report generation configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report configuration updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Report configuration not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ReportGenerationConfiguration> updateConfiguration(
            @Parameter(description = "Report configuration ID", required = true) @PathVariable Long id,
            @Valid @RequestBody CreateReportGenerationConfigurationRequest request) {
        return ResponseEntity.ok(reportGenerationConfigurationService.updateConfiguration(id, request));
    }

    @Operation(summary = "Deactivate report generation configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Report configuration deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Report configuration not found")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateConfiguration(
            @Parameter(description = "Report configuration ID", required = true) @PathVariable Long id) {
        reportGenerationConfigurationService.deactivateConfiguration(id);
        return ResponseEntity.noContent().build();
    }
}
