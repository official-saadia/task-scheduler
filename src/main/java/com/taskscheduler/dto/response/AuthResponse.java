package com.taskscheduler.dto.response;

/**
 * Response DTO returned after successful authentication.
 *
 * <p>Contains the generated JWT token and its type.
 * The token should be included in subsequent requests
 * in the {@code Authorization} header as:</p>
 * <pre>
 *   Authorization: Bearer {@code <token>}
 * </pre>
 *
 * @param token     the generated JWT token string
 * @param tokenType the token type, always {@code "Bearer"}
 */
public record AuthResponse(
        String token,
        String tokenType
) {
    /**
     * Convenience factory method that creates an {@code AuthResponse}
     * with the default {@code "Bearer"} token type.
     *
     * @param token the generated JWT token string
     * @return a new {@code AuthResponse} with token type set to {@code "Bearer"}
     */
    public static AuthResponse of(String token) {
        return new AuthResponse(token, "Bearer");
    }
}
