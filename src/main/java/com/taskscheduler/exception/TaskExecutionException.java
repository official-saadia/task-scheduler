package com.taskscheduler.exception;

/**
 * Exception thrown when a task fails during execution by the scheduler engine.
 *
 * <p>This exception is used internally by the scheduler engine to signal
 * that a task could not be executed successfully. It triggers the retry
 * mechanism and, after exhausting all retry attempts, causes the task
 * to be moved to the Dead Letter Queue.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try {
 *     emailService.send(email);
 * } catch (MailException ex) {
 *     throw new TaskExecutionException("Failed to send email to: " + recipient, ex);
 * }
 * }</pre>
 */
public class TaskExecutionException extends RuntimeException {

    /**
     * Constructs a new {@code TaskExecutionException} with the given message.
     *
     * @param message a descriptive message indicating why the task failed
     */
    public TaskExecutionException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code TaskExecutionException} with the given message and cause.
     *
     * @param message a descriptive message indicating why the task failed
     * @param cause   the underlying exception that caused the failure
     */
    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
