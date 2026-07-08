package com.taskscheduler.exception;

import com.taskscheduler.enums.TaskType;

/**
 * Exception thrown when an unsupported or unrecognised task type is provided.
 *
 * <p>This exception is thrown when a request contains a task type string
 * that does not match any value in the {@link TaskType}
 * enum. It maps to an HTTP {@code 400 Bad Request} response via the
 * {@link GlobalExceptionHandler}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try {
 *     TaskType.valueOf(request.type());
 * } catch (IllegalArgumentException ex) {
 *     throw new InvalidTaskTypeException("Unsupported task type: " + request.type());
 * }
 * }</pre>
 */
public class InvalidTaskTypeException extends RuntimeException {

    /**
     * Constructs a new {@code InvalidTaskTypeException} with the given message.
     *
     * @param message a descriptive message indicating the invalid task type
     */
    public InvalidTaskTypeException(String message) {
        super(message);
    }
}
