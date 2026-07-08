package com.taskscheduler.service;

import com.taskscheduler.dto.request.CreateSmtpConfigurationRequest;
import com.taskscheduler.entity.SmtpConfiguration;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.SmtpConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SmtpConfigurationService {

    private final SmtpConfigurationRepository smtpConfigurationRepository;

    public SmtpConfiguration createSmtpConfiguration(CreateSmtpConfigurationRequest request) {
        SmtpConfiguration config = SmtpConfiguration.builder()
                .host(request.host()).port(request.port())
                .username(request.username()).password(request.password())
                .isActive(true).build();
        return smtpConfigurationRepository.save(config);
    }

    @Transactional(readOnly = true)
    public Page<SmtpConfiguration> getAllConfigurations(int page, int size) {
        return smtpConfigurationRepository
                .findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public SmtpConfiguration getConfigurationById(Long id) {
        return findById(id);
    }

    @Transactional(readOnly = true)
    public SmtpConfiguration getActiveConfiguration() {
        return smtpConfigurationRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active SMTP configuration found"));
    }

    public SmtpConfiguration updateConfiguration(Long id, CreateSmtpConfigurationRequest request) {
        SmtpConfiguration config = findById(id);
        config.setHost(request.host());
        config.setPort(request.port());
        config.setUsername(request.username());
        config.setPassword(request.password());
        return smtpConfigurationRepository.save(config);
    }

    public void deactivateConfiguration(Long id) {
        SmtpConfiguration config = findById(id);
        config.setIsActive(false);
        smtpConfigurationRepository.save(config);
    }

    private SmtpConfiguration findById(Long id) {
        return smtpConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMTP configuration not found with id: " + id));
    }
}
