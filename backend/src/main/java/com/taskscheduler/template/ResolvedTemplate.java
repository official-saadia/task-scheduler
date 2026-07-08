package com.taskscheduler.template;

import com.taskscheduler.email.EmailResolvedTemplate;

/**
 * Marker interface for resolved templates.
 *
 * <p>Each task type defines its own resolved template implementation
 * containing the fields specific to that task type. This keeps the
 * template resolution generic and extensible without coupling task
 * types to a single template structure.</p>
 *
 * <p>Current implementations:</p>
 * <ul>
 *   <li>{@link EmailResolvedTemplate} — resolved email notification template</li>
 * </ul>
 *
 * <p>To add a new task type template (e.g., database backup):</p>
 * <ol>
 *   <li>Create a new record implementing this interface</li>
 *   <li>Create a corresponding {@link TemplateResolver} implementation</li>
 *   <li>Zero changes required to existing code</li>
 * </ol>
 */
public interface ResolvedTemplate {
}
