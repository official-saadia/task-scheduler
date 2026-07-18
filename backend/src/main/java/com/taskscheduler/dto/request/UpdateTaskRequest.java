package com.taskscheduler.dto.request;

import com.taskscheduler.validation.ValidCronExpression;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating an existing scheduled task.
 *
 * <p>All fields are required except the configuration IDs and attachment path.
 * Exactly one configuration ID must be provided matching {@code type}:</p>
 * <ul>
 *   <li>{@code EMAIL_NOTIFICATION} → requires {@code smtpConfigurationId}, optional {@code attachmentPath}</li>
 *   <li>{@code DATABASE_BACKUP} → requires {@code backupConfigurationId}</li>
 *   <li>{@code REPORT_GENERATION} → requires {@code reportConfigurationId}</li>
 * </ul>
 *
 * @param name                   the updated display name of the task
 * @param type                   the updated task type, must match a valid {@link com.taskscheduler.enums.TaskType}
 * @param cronExpression         a valid Spring cron expression defining the updated execution schedule
 * @param smtpConfigurationId    the SMTP configuration ID, required when type is EMAIL_NOTIFICATION
 * @param backupConfigurationId  the backup configuration ID, required when type is DATABASE_BACKUP
 * @param reportConfigurationId  the report configuration ID, required when type is REPORT_GENERATION
 * @param attachmentPath         optional file path to attach, only used when type is EMAIL_NOTIFICATION
 * @param isActive               whether the task should be active after the update
 */
public record UpdateTaskRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank @ValidCronExpression String cronExpression,
        Long smtpConfigurationId,
        Long backupConfigurationId,
        Long reportConfigurationId,
        String attachmentPath,
        @NotNull Boolean isActive
) {}
