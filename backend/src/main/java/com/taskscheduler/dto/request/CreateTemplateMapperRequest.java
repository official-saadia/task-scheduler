package com.taskscheduler.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTemplateMapperRequest(
        @NotNull Long taskId,
        @NotNull Long templateId,
        @NotBlank String keyName,
        @NotBlank String value
) {}
