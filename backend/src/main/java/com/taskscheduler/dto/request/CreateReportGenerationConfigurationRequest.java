package com.taskscheduler.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateReportGenerationConfigurationRequest(
        @NotBlank String name,
        @NotBlank String command,
        String workingDirectory,
        @NotBlank String outputFilePath,
        @Min(1) @Max(86400) Integer timeoutSeconds
) {}
