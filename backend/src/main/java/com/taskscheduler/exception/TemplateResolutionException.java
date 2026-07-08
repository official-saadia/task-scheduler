package com.taskscheduler.exception;

/**
 * Exception thrown when a template placeholder cannot be resolved at execution time.
 *
 * <p>This exception is raised by the scheduler engine when a template contains
 * a placeholder for which no corresponding {@code TemplateMapper} entry exists.
 * It prevents sending a partially populated email with unresolved placeholders.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * if (!placeholderValues.containsKey(placeholder)) {
 *     throw new TemplateResolutionException(
 *         "No mapper found for placeholder: " + placeholder + " in task id: " + taskId);
 * }
 * }</pre>
 */
public class TemplateResolutionException extends RuntimeException {

    /**
     * Constructs a new {@code TemplateResolutionException} with the given message.
     *
     * @param message a descriptive message indicating which placeholder could not be resolved
     */
    public TemplateResolutionException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code TemplateResolutionException} with the given message and cause.
     *
     * @param message a descriptive message indicating which placeholder could not be resolved
     * @param cause   the underlying exception that caused the failure
     */
    public TemplateResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
