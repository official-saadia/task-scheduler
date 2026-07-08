package com.taskscheduler.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating task template data.
 *
 * <p>The {@code data} field must be a valid JSON string whose structure
 * depends on the task type:</p>
 *
 * <p>For EMAIL_NOTIFICATION tasks:</p>
 * <pre>
 * [
 *   {"recipient": "john@example.com", "recipientName": "John Doe", "username": "johndoe"},
 *   {"recipient": "jane@example.com", "recipientName": "Jane Smith", "username": "janesmith"}
 * ]
 * </pre>
 *
 * @param taskId     the ID of the task this data belongs to
 * @param templateId the ID of the template to associate with this task
 * @param data       the JSON payload containing runtime data for template resolution
 */
public record CreateTaskTemplateDataRequest(
        @NotNull Long taskId,
        @NotNull Long templateId,
        @NotBlank String data
) {}
