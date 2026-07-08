package com.taskscheduler.dto.request;

import com.taskscheduler.validation.ValidCronExpression;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating an existing scheduled task.
 *
 * <p>All fields are required. To partially update a task,
 * fetch the existing task first and reuse unchanged values.</p>
 *
 * @param name                the updated display name of the task
 * @param type                the updated task type, must match a valid {@link com.taskscheduler.enums.TaskType}
 * @param cronExpression      a valid Spring cron expression defining the updated execution schedule
 * @param smtpConfigurationId the ID of the SMTP configuration to use for email dispatch
 * @param isActive            whether the task should be active after the update
 */
public record UpdateTaskRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank @ValidCronExpression String cronExpression,
        @NotNull Long smtpConfigurationId,
        @NotNull Boolean isActive
) {}
