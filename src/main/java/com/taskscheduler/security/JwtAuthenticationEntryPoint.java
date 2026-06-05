package com.taskscheduler.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskscheduler.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Handles unauthorised access attempts to protected API endpoints.
 *
 * <p>Implements {@link AuthenticationEntryPoint} and is invoked by Spring Security
 * whenever an unauthenticated request attempts to access a secured resource.
 * Instead of redirecting to a login page, this entry point returns a structured
 * JSON {@link ErrorResponse} with HTTP {@code 401 Unauthorized}.</p>
 *
 * <p>This is the correct behaviour for a stateless REST API where redirects
 * are not applicable and clients expect JSON error responses.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Responds to an unauthorised access attempt with a structured JSON error response.
     *
     * <p>Sets the response content type to {@code application/json},
     * HTTP status to {@code 401 Unauthorized}, and writes a standardised
     * {@link ErrorResponse} body to the response output stream.</p>
     *
     * @param request       the HTTP request that resulted in an {@link AuthenticationException}
     * @param response      the HTTP response to write the error to
     * @param authException the exception that caused the invocation
     * @throws IOException if an I/O error occurs while writing the response
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Access denied. Valid JWT token is required to access this resource.",
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
