package com.taskscheduler.dto.request;

import com.taskscheduler.validation.ValidCronExpression;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTaskRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank @ValidCronExpression String cronExpression,
        @NotNull Long smtpConfigurationId
) {}
