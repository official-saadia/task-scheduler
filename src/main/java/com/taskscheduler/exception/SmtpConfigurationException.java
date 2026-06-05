package com.taskscheduler.exception;

/**
 * Exception thrown when there is a problem with the SMTP configuration
 * during email dispatch.
 *
 * <p>This exception is raised by the scheduler engine when the SMTP
 * configuration is missing, inactive, or contains invalid credentials
 * that prevent a successful connection to the mail server. It maps to
 * an HTTP {@code 500 Internal Server Error} response via the
 * {@link GlobalExceptionHandler}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * SmtpConfiguration config = smtpConfigurationRepository.findByIsActiveTrue()
 *     .orElseThrow(() -> new SmtpConfigurationException("No active SMTP configuration found"));
 * }</pre>
 */
public class SmtpConfigurationException extends RuntimeException {

    /**
     * Constructs a new {@code SmtpConfigurationException} with the given message.
     *
     * @param message a descriptive message indicating the SMTP configuration issue
     */
    public SmtpConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code SmtpConfigurationException} with the given message and cause.
     *
     * @param message a descriptive message indicating the SMTP configuration issue
     * @param cause   the underlying exception that caused the failure
     */
    public SmtpConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
