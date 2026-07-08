package com.taskscheduler.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user authentication.
 *
 * <p>Contains the credentials required to authenticate a user
 * and obtain a JWT token. Both fields are mandatory.</p>
 *
 * @param username the username of the user attempting to authenticate
 * @param password the plain-text password of the user
 */
public record AuthRequest(
        @NotBlank String username,
        @NotBlank String password
) {}
