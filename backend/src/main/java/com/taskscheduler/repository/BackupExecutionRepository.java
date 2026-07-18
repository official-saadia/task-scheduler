package com.taskscheduler.repository;

import com.taskscheduler.entity.BackupExecution;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing {@link BackupExecution} records.
 */
public interface BackupExecutionRepository extends JpaRepository<BackupExecution, Long> {
}
