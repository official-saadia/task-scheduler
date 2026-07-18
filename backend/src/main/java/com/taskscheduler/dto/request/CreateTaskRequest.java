package com.taskscheduler.dto.request;

import com.taskscheduler.validation.ValidCronExpression;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new scheduled task.
 *
 * <p>Exactly one of {@code smtpConfigurationId}, {@code backupConfigurationId},
 * or {@code reportConfigurationId} must be provided, matching {@code type}:</p>
 * <ul>
 *   <li>{@code EMAIL_NOTIFICATION} → requires {@code smtpConfigurationId}.
 *       {@code attachmentPath} is optional — point it at a file (typically a
 *       REPORT_GENERATION task's output file) to attach it to every email.</li>
 *   <li>{@code DATABASE_BACKUP} → requires {@code backupConfigurationId}</li>
 *   <li>{@code REPORT_GENERATION} → requires {@code reportConfigurationId}</li>
 * </ul>
 */
public record CreateTaskRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank @ValidCronExpression String cronExpression,
        Long smtpConfigurationId,
        Long backupConfigurationId,
        Long reportConfigurationId,
        String attachmentPath
) {}
