package com.taskscheduler.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating the JSON payload of an existing task template data record.
 *
 * <p>The {@code data} field must be a valid JSON string whose structure
 * depends on the task type. For EMAIL_NOTIFICATION tasks, it must be a
 * JSON array where each entry contains a {@code recipient} field and
 * any number of placeholder key-value pairs:</p>
 * <pre>
 * [
 *   {"recipient": "john@example.com", "recipientName": "John Doe", "username": "johndoe"}
 * ]
 * </pre>
 *
 * @param data the new JSON payload to replace the existing data
 */
public record UpdateTaskTemplateDataRequest(
        @NotBlank String data
) {}
