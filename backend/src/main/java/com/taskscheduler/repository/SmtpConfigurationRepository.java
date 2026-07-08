package com.taskscheduler.repository;

import com.taskscheduler.entity.SmtpConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for managing {@link SmtpConfiguration} entities with pagination support.
 */
public interface SmtpConfigurationRepository extends JpaRepository<SmtpConfiguration, Long> {

    Page<SmtpConfiguration> findAll(Pageable pageable);

    Optional<SmtpConfiguration> findByIsActiveTrue();
}
