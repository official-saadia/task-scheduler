package com.taskscheduler.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateBackupConfigurationRequest(
        @NotBlank String name,
        @NotBlank String command,
        String workingDirectory,
        @Min(1) @Max(86400) Integer timeoutSeconds
) {}
