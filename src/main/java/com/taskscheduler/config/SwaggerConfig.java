package com.taskscheduler.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration for the Task Scheduler API.
 * Accessible at /swagger-ui.html once the application is running.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Configures the OpenAPI metadata for the Swagger UI.
     *
     * @return the OpenAPI configuration bean
     */
    @Bean
    public OpenAPI taskSchedulerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Scheduler API")
                        .description("A production-grade task scheduling system that supports email notification tasks with retry logic, dead letter queue, and analytics.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Task Scheduler")
                                .email("admin@taskscheduler.com")));
    }
}
