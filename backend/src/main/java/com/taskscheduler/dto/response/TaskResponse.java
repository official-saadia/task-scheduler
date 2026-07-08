package com.taskscheduler.dto.response;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String name,
        String type,
        String cronExpression,
        Long smtpConfigurationId,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
