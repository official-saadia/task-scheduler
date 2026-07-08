package com.taskscheduler.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSmtpConfigurationRequest(
        @NotBlank String host,
        @NotNull Integer port,
        @NotBlank String username,
        @NotBlank String password
) {}
