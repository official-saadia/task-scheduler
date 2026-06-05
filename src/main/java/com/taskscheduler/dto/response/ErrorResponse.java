package com.taskscheduler.dto.response;

import java.time.LocalDateTime;

/**
 * Standardised error response returned by the {@code GlobalExceptionHandler}
 * for all API error scenarios.
 *
 * <p>Provides a consistent error structure across all endpoints so that
 * API consumers can reliably parse and handle error responses.</p>
 *
 * @param timestamp the date and time the error occurred
 * @param status    the HTTP status code
 * @param error     a short description of the error type
 * @param message   a detailed message describing what went wrong
 * @param path      the request path that triggered the error
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {}
