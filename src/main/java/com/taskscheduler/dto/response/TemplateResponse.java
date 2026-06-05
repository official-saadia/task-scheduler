package com.taskscheduler.dto.response;

import java.time.LocalDateTime;

public record TemplateResponse(
        Long id,
        String name,
        String template,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
