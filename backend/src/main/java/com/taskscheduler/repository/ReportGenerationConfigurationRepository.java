package com.taskscheduler.repository;

import com.taskscheduler.entity.ReportGenerationConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportGenerationConfigurationRepository extends JpaRepository<ReportGenerationConfiguration, Long> {

    Page<ReportGenerationConfiguration> findAll(Pageable pageable);
}
