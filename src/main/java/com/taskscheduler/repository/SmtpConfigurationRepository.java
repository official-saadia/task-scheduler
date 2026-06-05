package com.taskscheduler.repository;

import com.taskscheduler.entity.SmtpConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SmtpConfigurationRepository extends JpaRepository<SmtpConfiguration, Long> {
    Optional<SmtpConfiguration> findByIsActiveTrue();
}
