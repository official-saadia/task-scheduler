package com.taskscheduler.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Task Scheduler API.
 *
 * <p>Intercepts exceptions thrown across all controllers and maps them
 * to standardised {@link ErrorResponse} objects with appropriate HTTP
 * status codes. This ensures consistent error responses for all API consumers.</p>
 *
 * <p>Handled exceptions and their HTTP status mappings:</p>
 * <ul>
 *   <li>{@link ResourceNotFoundException} → {@code 404 Not Found}</li>
 *   <li>{@link InvalidTaskTypeException} → {@code 400 Bad Request}</li>
 *   <li>{@link TemplateResolutionException} → {@code 422 Unprocessable Entity}</li>
 *   <li>{@link SmtpConfigurationException} → {@code 500 Internal Server Error}</li>
 *   <li>{@link TaskExecutionException} → {@code 500 Internal Server Error}</li>
 *   <li>{@link MethodArgumentNotValidException} → {@code 400 Bad Request}</li>
 *   <li>{@link Exception} → {@code 500 Internal Server Error}</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link ResourceNotFoundException} thrown when a requested
     * entity does not exist in the database.
     *
     * @param ex      the exception
     * @param request the HTTP request that triggered the exception
     * @return a {@code 404 Not Found} error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handles {@link InvalidTaskTypeException} thrown when an unsupported
     * task type is provided in the request.
     *
     * @param ex      the exception
     * @param request the HTTP request that triggered the exception
     * @return a {@code 400 Bad Request} error response
     */
    @ExceptionHandler(InvalidTaskTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTaskTypeException(
            InvalidTaskTypeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Task Type", ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handles {@link TemplateResolutionException} thrown when a template
     * placeholder cannot be resolved at execution time.
     *
     * @param ex      the exception
     * @param request the HTTP request that triggered the exception
     * @return a {@code 422 Unprocessable Entity} error response
     */
    @ExceptionHandler(TemplateResolutionException.class)
    public ResponseEntity<ErrorResponse> handleTemplateResolutionException(
            TemplateResolutionException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Template Resolution Failed", ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handles {@link SmtpConfigurationException} thrown when the SMTP
     * configuration is missing or invalid during email dispatch.
     *
     * @param ex      the exception
     * @param request the HTTP request that triggered the exception
     * @return a {@code 500 Internal Server Error} error response
     */
    @ExceptionHandler(SmtpConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleSmtpConfigurationException(
            SmtpConfigurationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SMTP Configuration Error", ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handles {@link TaskExecutionException} thrown when a task fails
     * during execution by the scheduler engine.
     *
     * @param ex      the exception
     * @param request the HTTP request that triggered the exception
     * @return a {@code 500 Internal Server Error} error response
     */
    @ExceptionHandler(TaskExecutionException.class)
    public ResponseEntity<ErrorResponse> handleTaskExecutionException(
            TaskExecutionException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Task Execution Failed", ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handles {@link MethodArgumentNotValidException} thrown when a request
     * body fails {@code @Valid} bean validation.
     *
     * <p>Aggregates all validation error messages into a single readable string.</p>
     *
     * @param ex      the exception containing validation errors
     * @param request the HTTP request that triggered the exception
     * @return a {@code 400 Bad Request} error response with all validation messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", validationErrors, request.getRequestURI());
    }

    /**
     * Handles {@link IllegalArgumentException} thrown when request parameters
     * are structurally valid but semantically wrong — for example a DLQ export
     * custom range whose {@code to} date precedes its {@code from} date.
     *
     * <p>Without this handler such errors fall through to
     * {@link #handleGenericException} and surface as a {@code 500} with a
     * generic message, hiding the actual reason from the caller.</p>
     *
     * @param ex      the exception carrying the caller-facing message
     * @param request the HTTP request that triggered the exception
     * @return a {@code 400 Bad Request} error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Request", ex.getMessage(), request.getRequestURI());
    }

    /**
     * Handles all unhandled exceptions as a fallback to prevent exposing
     * internal stack traces to API consumers.
     *
     * @param ex      the exception
     * @param request the HTTP request that triggered the exception
     * @return a {@code 500 Internal Server Error} error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", request.getRequestURI());
    }

    /**
     * Builds a standardised {@link ErrorResponse} wrapped in a {@link ResponseEntity}.
     *
     * @param status  the HTTP status to return
     * @param error   a short error type description
     * @param message a detailed error message
     * @param path    the request URI that triggered the error
     * @return the built {@link ResponseEntity} containing the {@link ErrorResponse}
     */
    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String error, String message, String path) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                path
        );
        return ResponseEntity.status(status).body(errorResponse);
    }
}
