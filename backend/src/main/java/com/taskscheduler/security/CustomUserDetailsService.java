package com.taskscheduler.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Custom implementation of {@link UserDetailsService} for JWT authentication.
 *
 * <p>For the current single-client setup, credentials are loaded from
 * {@code application.yml} rather than a database. This keeps the implementation
 * simple while the core scheduler functionality is being built.</p>
 *
 * <p>The {@link PasswordEncoder} is injected via field injection here intentionally
 * to avoid a circular dependency with {@link com.taskscheduler.config.SecurityConfig}.</p>
 *
 * <p>When multi-tenancy or full user management is added in a future iteration,
 * this class can be updated to load user details from a {@code users} database
 * table without affecting the rest of the security configuration.</p>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a new {@code CustomUserDetailsService} with the given password encoder.
     * The {@link PasswordEncoder} bean is sourced from {@link com.taskscheduler.config.PasswordEncoderConfig}
     * to avoid circular dependency.
     *
     * @param passwordEncoder the encoder used to hash the configured admin password
     */
    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Loads user details by username for JWT authentication.
     *
     * <p>Currently validates against a single admin user configured in
     * {@code application.yml}. Returns a Spring Security {@link User} object
     * with the {@code ROLE_ADMIN} authority.</p>
     *
     * @param username the username to look up
     * @return a {@link UserDetails} object containing the user's credentials and authorities
     * @throws UsernameNotFoundException if the username does not match the configured admin username
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!username.equals(adminUsername)) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        return User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();
    }
}