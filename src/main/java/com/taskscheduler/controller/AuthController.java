package com.taskscheduler.controller;

import com.taskscheduler.dto.request.AuthRequest;
import com.taskscheduler.dto.response.AuthResponse;
import com.taskscheduler.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling authentication requests.
 *
 * <p>Exposes a public login endpoint that validates user credentials
 * and returns a signed JWT token on success. The token must be included
 * in the {@code Authorization} header of all subsequent API requests.</p>
 *
 * <p>This endpoint is explicitly permitted without authentication
 * in {@link com.taskscheduler.config.SecurityConfig}.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for obtaining and managing JWT tokens")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Authenticates a user and returns a signed JWT token.
     *
     * <p>Validates the provided credentials against the configured user store.
     * On success, generates and returns a JWT token to be used in subsequent
     * requests via the {@code Authorization: Bearer <token>} header.</p>
     *
     * @param request the authentication request containing username and password
     * @return an {@link AuthResponse} containing the JWT token with HTTP 200 status
     */
    @Operation(
            summary = "Login",
            description = "Authenticates user credentials and returns a JWT token for subsequent API requests"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful, JWT token returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails.getUsername());

        return ResponseEntity.ok(AuthResponse.of(token));
    }
}
