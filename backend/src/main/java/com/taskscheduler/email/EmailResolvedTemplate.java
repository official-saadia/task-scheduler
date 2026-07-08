package com.taskscheduler.email;

import com.taskscheduler.template.ResolvedTemplate;

import java.util.List;

/**
 * Represents a fully resolved email notification template containing
 * a personalized {@link ResolvedEmailMessage} for each recipient.
 *
 * <p>Produced by {@link EmailTemplateResolver} after parsing the JSON template,
 * resolving shared placeholders, and generating individual personalized messages
 * for each recipient in the recipients list.</p>
 *
 * <p>Each {@link ResolvedEmailMessage} in the list has its own resolved subject
 * and body, allowing fully personalized emails where each recipient receives
 * content tailored specifically to them.</p>
 *
 * @param messages the list of personalized resolved email messages,
 *                 one per recipient
 */
public record EmailResolvedTemplate(
        List<ResolvedEmailMessage> messages
) implements ResolvedTemplate {}