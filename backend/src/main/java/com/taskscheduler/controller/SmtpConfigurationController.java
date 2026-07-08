package com.taskscheduler.controller;

import com.taskscheduler.dto.request.CreateSmtpConfigurationRequest;
import com.taskscheduler.entity.SmtpConfiguration;
import com.taskscheduler.service.SmtpConfigurationService;
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
@RequestMapping("/api/v1/smtp-configurations")
@RequiredArgsConstructor
@Tag(name = "SMTP Configurations", description = "APIs for managing SMTP email server configurations")
public class SmtpConfigurationController {

    private final SmtpConfigurationService smtpConfigurationService;

    @Operation(summary = "Create SMTP configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "SMTP configuration created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<SmtpConfiguration> createConfiguration(
            @Valid @RequestBody CreateSmtpConfigurationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(smtpConfigurationService.createSmtpConfiguration(request));
    }

    @Operation(summary = "Get all SMTP configurations with pagination")
    @ApiResponse(responseCode = "200", description = "SMTP configurations retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<SmtpConfiguration>> getAllConfigurations(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(smtpConfigurationService.getAllConfigurations(page, size));
    }

    @Operation(summary = "Get SMTP configuration by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SMTP configuration found"),
            @ApiResponse(responseCode = "404", description = "SMTP configuration not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SmtpConfiguration> getConfigurationById(
            @Parameter(description = "SMTP configuration ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(smtpConfigurationService.getConfigurationById(id));
    }

    @Operation(summary = "Get active SMTP configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active SMTP configuration found"),
            @ApiResponse(responseCode = "404", description = "No active SMTP configuration found")
    })
    @GetMapping("/active")
    public ResponseEntity<SmtpConfiguration> getActiveConfiguration() {
        return ResponseEntity.ok(smtpConfigurationService.getActiveConfiguration());
    }

    @Operation(summary = "Update SMTP configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SMTP configuration updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "SMTP configuration not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<SmtpConfiguration> updateConfiguration(
            @Parameter(description = "SMTP configuration ID", required = true) @PathVariable Long id,
            @Valid @RequestBody CreateSmtpConfigurationRequest request) {
        return ResponseEntity.ok(smtpConfigurationService.updateConfiguration(id, request));
    }

    @Operation(summary = "Deactivate SMTP configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "SMTP configuration deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "SMTP configuration not found")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateConfiguration(
            @Parameter(description = "SMTP configuration ID", required = true) @PathVariable Long id) {
        smtpConfigurationService.deactivateConfiguration(id);
        return ResponseEntity.noContent().build();
    }
}
