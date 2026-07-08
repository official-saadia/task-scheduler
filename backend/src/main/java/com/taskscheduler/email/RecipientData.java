package com.taskscheduler.email;

import java.util.Map;

/**
 * Represents a single recipient parsed from the {@code task_template_data} JSON array.
 *
 * <p>Each recipient entry in the JSON array is a flat object where:</p>
 * <ul>
 *   <li>{@code recipient} is the reserved key for the email address</li>
 *   <li>All other keys are treated as placeholder values for template resolution</li>
 * </ul>
 *
 * <p>Example JSON entry:</p>
 * <pre>
 * {"recipient": "john@example.com", "recipientName": "John Doe", "username": "johndoe"}
 * </pre>
 *
 * @param recipient    the recipient's email address
 * @param placeholders all other key-value pairs used as template placeholder values
 */
public record RecipientData(
        String recipient,
        Map<String, String> placeholders
) {}
