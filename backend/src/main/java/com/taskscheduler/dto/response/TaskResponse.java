package com.taskscheduler.dto.response;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String name,
        String type,
        String cronExpression,
        Long smtpConfigurationId,
        Long backupConfigurationId,
        Long reportConfigurationId,
        String attachmentPath,
        Boolean isActive,
        LocalDateTime nextExecutionTime,
        LocalDateTime lastExecutionTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
