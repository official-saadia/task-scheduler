package com.taskscheduler.service;

import com.taskscheduler.dto.request.CreateTemplateRequest;
import com.taskscheduler.dto.response.TemplateResponse;
import com.taskscheduler.entity.Template;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for managing email templates.
 *
 * <p>Templates are reusable email structures that support dynamic placeholders
 * in the format {@code {{placeholderName}}}. Placeholder values are resolved
 * at task execution time via the {@code TemplateMapper} table.</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Creating and persisting new templates</li>
 *   <li>Retrieving active or specific templates</li>
 *   <li>Deactivating templates that are no longer needed</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;

    /**
     * Creates a new email template and persists it to the database.
     *
     * <p>The template body can contain dynamic placeholders such as
     * {@code {{recipientName}}} or {@code {{subject}}} which will be
     * resolved at execution time via the template mapper.</p>
     *
     * @param request the template creation request containing the name and template body
     * @return the created template as a {@link TemplateResponse} DTO
     */
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        Template template = Template.builder()
                .name(request.name())
                .template(request.template())
                .isActive(true)
                .build();

        return toResponse(templateRepository.save(template));
    }

    /**
     * Retrieves all active templates.
     *
     * <p>Only templates with {@code isActive = true} are returned.
     * Deactivated templates are excluded from this list.</p>
     *
     * @return a list of active templates as {@link TemplateResponse} DTOs,
     *         or an empty list if none exist
     */
    public List<TemplateResponse> getAllTemplates() {
        return templateRepository.findAllByIsActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves a single template by its ID regardless of its active status.
     *
     * @param id the unique identifier of the template
     * @return the template as a {@link TemplateResponse} DTO
     * @throws ResourceNotFoundException if no template exists with the given ID
     */
    public TemplateResponse getTemplateById(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));
        return toResponse(template);
    }

    /**
     * Deactivates a template by setting its {@code isActive} flag to {@code false}.
     *
     * <p>Deactivated templates can no longer be associated with new tasks.
     * Existing tasks that already reference this template are not affected
     * until their next execution.</p>
     *
     * @param id the unique identifier of the template to deactivate
     * @return the updated template as a {@link TemplateResponse} DTO
     * @throws ResourceNotFoundException if no template exists with the given ID
     */
    public TemplateResponse deactivateTemplate(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));
        template.setIsActive(false);
        return toResponse(templateRepository.save(template));
    }

    /**
     * Maps a {@link Template} entity to a {@link TemplateResponse} DTO.
     *
     * @param template the template entity to map
     * @return the mapped {@link TemplateResponse} DTO
     */
    private TemplateResponse toResponse(Template template) {
        return new TemplateResponse(
                template.getId(),
                template.getName(),
                template.getTemplate(),
                template.getIsActive(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
