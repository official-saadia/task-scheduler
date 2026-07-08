package com.taskscheduler.email;

import com.taskscheduler.entity.TaskTemplateData;
import com.taskscheduler.template.ResolvedTemplate;

/**
 * Generic interface for resolving task templates into their concrete resolved form.
 *
 * <p>Each task type provides its own implementation of this interface, defining
 * how its JSON template is parsed and how the runtime data stored in
 * {@link TaskTemplateData#getData()} is used to resolve placeholders.</p>
 *
 * <p>This follows the <b>Open/Closed Principle</b> — adding a new task type
 * requires only a new implementation of this interface with zero changes
 * to existing resolvers or the execution engine.</p>
 *
 * <p>Current implementations:</p>
 * <ul>
 *   <li>{@link EmailTemplateResolver} — resolves email notification templates
 *       using a flat JSON array of recipients with personalized placeholders</li>
 * </ul>
 *
 * @param <T> the type of {@link ResolvedTemplate} produced by this resolver
 */
public interface TemplateResolver<T extends ResolvedTemplate> {

    /**
     * Resolves the given JSON template using the runtime data stored in
     * {@link TaskTemplateData}.
     *
     * <p>Implementations are responsible for parsing the template's JSON structure,
     * parsing {@link TaskTemplateData#getData()} according to the task type's
     * expected format, resolving all placeholders, and returning a fully
     * populated {@link ResolvedTemplate} instance.</p>
     *
     * @param templateJson     the raw JSON template string stored in {@code templates.template}
     * @param taskTemplateData the {@link TaskTemplateData} entity containing the
     *                         task-specific runtime JSON payload in {@code data}
     * @return a fully resolved {@link ResolvedTemplate} of type {@code T}
     * @throws com.taskscheduler.exception.TemplateResolutionException if the JSON is malformed,
     *         a required field is missing, or a placeholder has no corresponding value
     */
    T resolve(String templateJson, TaskTemplateData taskTemplateData);
}