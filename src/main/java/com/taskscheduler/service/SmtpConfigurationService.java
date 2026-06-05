package com.taskscheduler.service;

import com.taskscheduler.dto.request.CreateSmtpConfigurationRequest;
import com.taskscheduler.entity.SmtpConfiguration;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.SmtpConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for managing SMTP configurations.
 *
 * <p>SMTP configurations define the email server settings used by the scheduler
 * engine when sending email notifications. Each task references an SMTP
 * configuration, allowing future support for multiple configurations
 * without code changes.</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Creating and persisting SMTP configurations</li>
 *   <li>Retrieving all or specific SMTP configurations</li>
 *   <li>Deactivating configurations that are no longer in use</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SmtpConfigurationService {

    private final SmtpConfigurationRepository smtpConfigurationRepository;

    /**
     * Creates a new SMTP configuration and persists it to the database.
     *
     * <p>The configuration is set to active by default. Only one configuration
     * should be active at a time in the current single-client setup.</p>
     *
     * @param request the SMTP configuration creation request containing host, port, username, and password
     * @return the persisted {@link SmtpConfiguration} entity
     */
    public SmtpConfiguration createSmtpConfiguration(CreateSmtpConfigurationRequest request) {
        SmtpConfiguration config = SmtpConfiguration.builder()
                .host(request.host())
                .port(request.port())
                .username(request.username())
                .password(request.password())
                .isActive(true)
                .build();

        return smtpConfigurationRepository.save(config);
    }

    /**
     * Retrieves all SMTP configurations regardless of their active status.
     *
     * @return a list of all {@link SmtpConfiguration} entities,
     *         or an empty list if none exist
     */
    public List<SmtpConfiguration> getAllConfigurations() {
        return smtpConfigurationRepository.findAll();
    }

    /**
     * Retrieves a single SMTP configuration by its ID.
     *
     * @param id the unique identifier of the SMTP configuration
     * @return the {@link SmtpConfiguration} entity
     * @throws ResourceNotFoundException if no configuration exists with the given ID
     */
    public SmtpConfiguration getConfigurationById(Long id) {
        return smtpConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMTP configuration not found with id: " + id));
    }

    /**
     * Retrieves the currently active SMTP configuration.
     *
     * <p>Used internally by the scheduler engine to resolve the correct
     * email server settings at task execution time.</p>
     *
     * @return the active {@link SmtpConfiguration} entity
     * @throws ResourceNotFoundException if no active configuration is found
     */
    public SmtpConfiguration getActiveConfiguration() {
        return smtpConfigurationRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active SMTP configuration found"));
    }

    /**
     * Deactivates an SMTP configuration by setting its {@code isActive} flag to {@code false}.
     *
     * <p>Tasks referencing this configuration will fail on their next execution
     * if no other active configuration is available.</p>
     *
     * @param id the unique identifier of the SMTP configuration to deactivate
     * @throws ResourceNotFoundException if no configuration exists with the given ID
     */
    public void deactivateConfiguration(Long id) {
        SmtpConfiguration config = smtpConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMTP configuration not found with id: " + id));
        config.setIsActive(false);
        smtpConfigurationRepository.save(config);
    }
}
