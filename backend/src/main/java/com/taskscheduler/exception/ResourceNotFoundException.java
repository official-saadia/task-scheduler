package com.taskscheduler.exception;

/**
 * Exception thrown when a requested resource cannot be found in the database.
 *
 * <p>This exception is typically thrown by service layer methods when a
 * lookup by ID or unique field returns no result. It maps to an HTTP
 * {@code 404 Not Found} response via the {@link GlobalExceptionHandler}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Task task = taskRepository.findById(id)
 *     .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
 * }</pre>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code ResourceNotFoundException} with the given message.
     *
     * @param message a descriptive message indicating which resource was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
