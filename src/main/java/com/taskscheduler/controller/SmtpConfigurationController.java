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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing SMTP configurations.
 *
 * <p>SMTP configurations define the email server settings used when sending
 * email notifications. Each task references an SMTP configuration, allowing
 * future multi-client support without code changes.</p>
 */
@RestController
@RequestMapping("/api/v1/smtp-configurations")
@RequiredArgsConstructor
@Tag(name = "SMTP Configurations", description = "APIs for managing SMTP email server configurations")
public class SmtpConfigurationController {

    private final SmtpConfigurationService smtpConfigurationService;

    /**
     * Creates a new SMTP configuration.
     *
     * @param request the SMTP configuration creation request
     * @return the created SMTP configuration with HTTP 201 status
     */
    @Operation(summary = "Create SMTP configuration", description = "Creates a new SMTP email server configuration")
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

    /**
     * Retrieves all SMTP configurations.
     *
     * @return list of all SMTP configurations with HTTP 200 status
     */
    @Operation(summary = "Get all SMTP configurations", description = "Returns a list of all SMTP configurations regardless of active status")
    @ApiResponse(responseCode = "200", description = "SMTP configurations retrieved successfully")
    @GetMapping
    public ResponseEntity<List<SmtpConfiguration>> getAllConfigurations() {
        return ResponseEntity.ok(smtpConfigurationService.getAllConfigurations());
    }

    /**
     * Retrieves a single SMTP configuration by its ID.
     *
     * @param id the SMTP configuration ID
     * @return the SMTP configuration with HTTP 200 status
     */
    @Operation(summary = "Get SMTP configuration by ID", description = "Returns a single SMTP configuration by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SMTP configuration found"),
            @ApiResponse(responseCode = "404", description = "SMTP configuration not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SmtpConfiguration> getConfigurationById(
            @Parameter(description = "SMTP configuration ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(smtpConfigurationService.getConfigurationById(id));
    }

    /**
     * Retrieves the currently active SMTP configuration.
     *
     * @return the active SMTP configuration with HTTP 200 status
     */
    @Operation(summary = "Get active SMTP configuration", description = "Returns the currently active SMTP configuration used by the scheduler engine")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active SMTP configuration found"),
            @ApiResponse(responseCode = "404", description = "No active SMTP configuration found")
    })
    @GetMapping("/active")
    public ResponseEntity<SmtpConfiguration> getActiveConfiguration() {
        return ResponseEntity.ok(smtpConfigurationService.getActiveConfiguration());
    }

    /**
     * Deactivates an SMTP configuration by its ID.
     *
     * @param id the SMTP configuration ID to deactivate
     * @return HTTP 204 No Content on success
     */
    @Operation(summary = "Deactivate SMTP configuration", description = "Deactivates an SMTP configuration so it is no longer used by the scheduler engine")
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
