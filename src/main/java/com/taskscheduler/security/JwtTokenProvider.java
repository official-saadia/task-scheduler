package com.taskscheduler.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Component responsible for generating, parsing, and validating JWT tokens.
 *
 * <p>Uses HMAC-SHA256 signing with a configurable secret key and expiration time.
 * The secret key and expiration are loaded from {@code application.yml} to keep
 * sensitive values out of the codebase.</p>
 *
 * <p>Token lifecycle:</p>
 * <ol>
 *   <li>Token is generated on successful authentication via {@link #generateToken(String)}</li>
 *   <li>Token is validated on every request via {@link #validateToken(String)}</li>
 *   <li>Username is extracted from token via {@link #getUsernameFromToken(String)}</li>
 * </ol>
 */
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Builds a {@link SecretKey} from the configured JWT secret string.
     *
     * @return a {@link SecretKey} instance for HMAC-SHA signing
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT token for the given username.
     *
     * <p>The token contains the username as the subject, the issue timestamp,
     * and an expiration timestamp calculated from the configured expiration duration.</p>
     *
     * @param username the authenticated user's username to embed as the token subject
     * @return a signed JWT token string
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the username (subject) from a valid JWT token.
     *
     * @param token the JWT token string to parse
     * @return the username embedded in the token's subject claim
     * @throws JwtException if the token is malformed, expired, or has an invalid signature
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Validates a JWT token by verifying its signature and checking its expiration.
     *
     * @param token the JWT token string to validate
     * @return {@code true} if the token is valid and not expired, {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            System.err.println("JWT token expired: " + ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            System.err.println("JWT token unsupported: " + ex.getMessage());
        } catch (MalformedJwtException ex) {
            System.err.println("JWT token malformed: " + ex.getMessage());
        } catch (SecurityException ex) {
            System.err.println("JWT signature invalid: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            System.err.println("JWT token empty or null: " + ex.getMessage());
        }
        return false;
    }
}
