package com.taskscheduler.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class for standalone utility beans.
 *
 * <p>Defines {@link PasswordEncoder} and {@link ObjectMapper} beans in a
 * dedicated configuration class to avoid circular dependencies between
 * {@link SecurityConfig}, {@link com.taskscheduler.security.CustomUserDetailsService},
 * and {@link com.taskscheduler.security.JwtAuthenticationFilter}.</p>
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Creates a {@link BCryptPasswordEncoder} bean for password hashing.
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates a configured {@link ObjectMapper} bean for JSON serialisation.
     *
     * <p>Registers the {@link JavaTimeModule} to correctly serialise
     * {@code LocalDateTime} and other Java 8 date/time types as ISO strings
     * rather than timestamps.</p>
     *
     * @return a configured {@link ObjectMapper} instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}