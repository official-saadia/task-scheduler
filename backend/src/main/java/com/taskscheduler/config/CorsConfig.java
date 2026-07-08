package com.taskscheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration allowing the React frontend running on localhost:5173
 * to communicate with the Spring Boot backend during local development.
 *
 * <p>In production, replace the allowed origin with your actual frontend domain.</p>
 */
@Configuration
public class CorsConfig {

    /**
     * Configures CORS to allow requests from the React dev server.
     *
     * @return the configured {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow the React dev server origin
        config.setAllowedOrigins(List.of("http://localhost:5173"));

        // Allow all standard HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow all headers including Authorization for JWT
        config.setAllowedHeaders(List.of("*"));

        // Allow the Authorization header to be read by the browser
        config.setExposedHeaders(List.of("Authorization"));

        // Allow cookies and Authorization headers on cross-origin requests
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}