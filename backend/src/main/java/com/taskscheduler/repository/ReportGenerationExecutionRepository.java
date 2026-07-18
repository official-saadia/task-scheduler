package com.taskscheduler.repository;

import com.taskscheduler.entity.ReportGenerationExecution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportGenerationExecutionRepository extends JpaRepository<ReportGenerationExecution, Long> {
}
