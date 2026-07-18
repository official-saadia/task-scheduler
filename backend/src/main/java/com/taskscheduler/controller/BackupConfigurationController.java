package com.taskscheduler.controller;

import com.taskscheduler.dto.request.CreateBackupConfigurationRequest;
import com.taskscheduler.entity.BackupConfiguration;
import com.taskscheduler.service.BackupConfigurationService;
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
@RequestMapping("/api/v1/backup-configurations")
@RequiredArgsConstructor
@Tag(name = "Backup Configurations", description = "APIs for managing database backup trigger configurations")
public class BackupConfigurationController {

    private final BackupConfigurationService backupConfigurationService;

    @Operation(summary = "Create backup configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Backup configuration created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<BackupConfiguration> createConfiguration(
            @Valid @RequestBody CreateBackupConfigurationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(backupConfigurationService.createBackupConfiguration(request));
    }

    @Operation(summary = "Get all backup configurations with pagination")
    @ApiResponse(responseCode = "200", description = "Backup configurations retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<BackupConfiguration>> getAllConfigurations(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(backupConfigurationService.getAllConfigurations(page, size));
    }

    @Operation(summary = "Get backup configuration by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Backup configuration found"),
            @ApiResponse(responseCode = "404", description = "Backup configuration not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BackupConfiguration> getConfigurationById(
            @Parameter(description = "Backup configuration ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(backupConfigurationService.getConfigurationById(id));
    }

    @Operation(summary = "Get active backup configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active backup configuration found"),
            @ApiResponse(responseCode = "404", description = "No active backup configuration found")
    })
    @GetMapping("/active")
    public ResponseEntity<BackupConfiguration> getActiveConfiguration() {
        return ResponseEntity.ok(backupConfigurationService.getActiveConfiguration());
    }

    @Operation(summary = "Update backup configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Backup configuration updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Backup configuration not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BackupConfiguration> updateConfiguration(
            @Parameter(description = "Backup configuration ID", required = true) @PathVariable Long id,
            @Valid @RequestBody CreateBackupConfigurationRequest request) {
        return ResponseEntity.ok(backupConfigurationService.updateConfiguration(id, request));
    }

    @Operation(summary = "Deactivate backup configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Backup configuration deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Backup configuration not found")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateConfiguration(
            @Parameter(description = "Backup configuration ID", required = true) @PathVariable Long id) {
        backupConfigurationService.deactivateConfiguration(id);
        return ResponseEntity.noContent().build();
    }
}
