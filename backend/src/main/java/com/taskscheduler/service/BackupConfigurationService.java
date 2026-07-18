package com.taskscheduler.service;

import com.taskscheduler.dto.request.CreateBackupConfigurationRequest;
import com.taskscheduler.entity.BackupConfiguration;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.BackupConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BackupConfigurationService {

    private static final int DEFAULT_TIMEOUT_SECONDS = 300;

    private final BackupConfigurationRepository backupConfigurationRepository;

    public BackupConfiguration createBackupConfiguration(CreateBackupConfigurationRequest request) {
        BackupConfiguration config = BackupConfiguration.builder()
                .name(request.name())
                .command(request.command())
                .workingDirectory(request.workingDirectory())
                .timeoutSeconds(request.timeoutSeconds() != null ? request.timeoutSeconds() : DEFAULT_TIMEOUT_SECONDS)
                .isActive(true)
                .build();
        return backupConfigurationRepository.save(config);
    }

    @Transactional(readOnly = true)
    public Page<BackupConfiguration> getAllConfigurations(int page, int size) {
        return backupConfigurationRepository
                .findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public BackupConfiguration getConfigurationById(Long id) {
        return findById(id);
    }

    @Transactional(readOnly = true)
    public BackupConfiguration getActiveConfiguration() {
        return backupConfigurationRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active backup configuration found"));
    }

    public BackupConfiguration updateConfiguration(Long id, CreateBackupConfigurationRequest request) {
        BackupConfiguration config = findById(id);
        config.setName(request.name());
        config.setCommand(request.command());
        config.setWorkingDirectory(request.workingDirectory());
        config.setTimeoutSeconds(request.timeoutSeconds() != null ? request.timeoutSeconds() : DEFAULT_TIMEOUT_SECONDS);
        return backupConfigurationRepository.save(config);
    }

    public void deactivateConfiguration(Long id) {
        BackupConfiguration config = findById(id);
        config.setIsActive(false);
        backupConfigurationRepository.save(config);
    }

    private BackupConfiguration findById(Long id) {
        return backupConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Backup configuration not found with id: " + id));
    }
}
