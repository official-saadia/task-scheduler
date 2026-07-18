package com.taskscheduler.service;

import com.taskscheduler.dto.request.CreateReportGenerationConfigurationRequest;
import com.taskscheduler.entity.ReportGenerationConfiguration;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.ReportGenerationConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportGenerationConfigurationService {

    private static final int DEFAULT_TIMEOUT_SECONDS = 300;

    private final ReportGenerationConfigurationRepository reportGenerationConfigurationRepository;

    public ReportGenerationConfiguration createConfiguration(CreateReportGenerationConfigurationRequest request) {
        ReportGenerationConfiguration config = ReportGenerationConfiguration.builder()
                .name(request.name())
                .command(request.command())
                .workingDirectory(request.workingDirectory())
                .outputFilePath(request.outputFilePath())
                .timeoutSeconds(request.timeoutSeconds() != null ? request.timeoutSeconds() : DEFAULT_TIMEOUT_SECONDS)
                .isActive(true)
                .build();
        return reportGenerationConfigurationRepository.save(config);
    }

    @Transactional(readOnly = true)
    public Page<ReportGenerationConfiguration> getAllConfigurations(int page, int size) {
        return reportGenerationConfigurationRepository
                .findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public ReportGenerationConfiguration getConfigurationById(Long id) {
        return findById(id);
    }

    public ReportGenerationConfiguration updateConfiguration(Long id, CreateReportGenerationConfigurationRequest request) {
        ReportGenerationConfiguration config = findById(id);
        config.setName(request.name());
        config.setCommand(request.command());
        config.setWorkingDirectory(request.workingDirectory());
        config.setOutputFilePath(request.outputFilePath());
        config.setTimeoutSeconds(request.timeoutSeconds() != null ? request.timeoutSeconds() : DEFAULT_TIMEOUT_SECONDS);
        return reportGenerationConfigurationRepository.save(config);
    }

    public void deactivateConfiguration(Long id) {
        ReportGenerationConfiguration config = findById(id);
        config.setIsActive(false);
        reportGenerationConfigurationRepository.save(config);
    }

    private ReportGenerationConfiguration findById(Long id) {
        return reportGenerationConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report generation configuration not found with id: " + id));
    }
}
