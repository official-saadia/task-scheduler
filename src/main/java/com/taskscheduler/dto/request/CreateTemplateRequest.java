package com.taskscheduler.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTemplateRequest(
        @NotBlank String name,
        @NotBlank String template
) {}
