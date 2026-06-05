package com.taskscheduler.config;

import com.taskscheduler.security.JwtAuthenticationEntryPoint;
import com.taskscheduler.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the Task Scheduler API.
 *
 * <p>Configures a stateless JWT-based security setup with the following rules:</p>
 * <ul>
 *   <li>Swagger UI and OpenAPI docs are publicly accessible</li>
 *   <li>Authentication endpoint {@code /api/v1/auth/**} is publicly accessible</li>
 *   <li>All other endpoints require a valid JWT token in the {@code Authorization} header</li>
 * </ul>
 *
 * <p>Session management is set to {@link SessionCreationPolicy#STATELESS} since
 * JWT tokens are self-contained and the server does not maintain session state.</p>
 *
 * <p>CSRF protection is disabled as this is a stateless REST API
 * that does not use browser-based session cookies.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;

    /**
     * Publicly accessible endpoints that bypass JWT authentication.
     * Includes Swagger UI paths and the authentication endpoint.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    /**
     * Configures the main security filter chain.
     *
     * <p>Sets up CSRF disabling, stateless session management, public endpoint
     * rules, JWT entry point for unauthorised access, and registers the
     * {@link JwtAuthenticationFilter} before the default username/password filter.</p>
     *
     * @param http the {@link HttpSecurity} builder to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Configures the {@link AuthenticationProvider} using DAO-based authentication.
     *
     * <p>Wires the {@link UserDetailsService} and {@link PasswordEncoder} together
     * so Spring Security can validate credentials during the login process.</p>
     *
     * @return the configured {@link DaoAuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} bean required by the auth controller
     * to programmatically authenticate user credentials during login.
     *
     * @param config the {@link AuthenticationConfiguration} provided by Spring
     * @return the {@link AuthenticationManager} bean
     * @throws Exception if an error occurs while retrieving the manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configures the {@link PasswordEncoder} using BCrypt hashing.
     *
     * <p>BCrypt is the industry standard for password hashing as it is
     * adaptive and computationally expensive, making brute-force attacks difficult.</p>
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
