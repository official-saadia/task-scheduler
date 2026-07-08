package com.taskscheduler.service;

import com.taskscheduler.dto.request.CreateTemplateRequest;
import com.taskscheduler.dto.response.TemplateResponse;
import com.taskscheduler.entity.Template;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateService {

    private final TemplateRepository templateRepository;

    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        Template template = Template.builder()
                .name(request.name())
                .template(request.template())
                .isActive(true)
                .build();
        return toResponse(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public Page<TemplateResponse> getAllTemplates(int page, int size) {
        return templateRepository
                .findAllByIsActiveTrue(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplateById(Long id) {
        return toResponse(findById(id));
    }

    public TemplateResponse updateTemplate(Long id, CreateTemplateRequest request) {
        Template template = findById(id);
        template.setName(request.name());
        template.setTemplate(request.template());
        return toResponse(templateRepository.save(template));
    }

    public TemplateResponse deactivateTemplate(Long id) {
        Template template = findById(id);
        template.setIsActive(false);
        return toResponse(templateRepository.save(template));
    }

    private Template findById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));
    }

    private TemplateResponse toResponse(Template t) {
        return new TemplateResponse(t.getId(), t.getName(), t.getTemplate(),
                t.getIsActive(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
