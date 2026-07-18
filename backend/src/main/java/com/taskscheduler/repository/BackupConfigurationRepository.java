package com.taskscheduler.repository;

import com.taskscheduler.entity.BackupConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for managing {@link BackupConfiguration} entities with pagination support.
 */
public interface BackupConfigurationRepository extends JpaRepository<BackupConfiguration, Long> {

    Page<BackupConfiguration> findAll(Pageable pageable);

    Optional<BackupConfiguration> findByIsActiveTrue();
}
