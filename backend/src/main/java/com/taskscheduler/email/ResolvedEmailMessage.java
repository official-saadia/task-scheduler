package com.taskscheduler.email;

/**
 * Represents a single fully resolved and personalized email message
 * ready to be sent to one recipient.
 *
 * <p>Produced by {@link EmailTemplateResolver} for each recipient in the
 * recipient list. Each instance contains the resolved subject and body
 * specific to that recipient, with all placeholders replaced by their
 * personal values.</p>
 *
 * @param recipient the recipient's email address
 * @param subject   the fully resolved and personalized email subject
 * @param body      the fully resolved and personalized email body
 */
public record ResolvedEmailMessage(
        String recipient,
        String subject,
        String body
) {}
