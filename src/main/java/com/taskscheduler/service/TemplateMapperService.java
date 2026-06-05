package com.taskscheduler.service;

import com.taskscheduler.dto.request.CreateTemplateMapperRequest;
import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.Template;
import com.taskscheduler.entity.TemplateMapper;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.TaskRepository;
import com.taskscheduler.repository.TemplateMapperRepository;
import com.taskscheduler.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for managing template mappers.
 *
 * <p>A {@link TemplateMapper} defines the mapping between a template placeholder
 * and its actual runtime value for a specific task. For example, a template
 * containing {@code {{recipientName}}} would have a corresponding mapper entry
 * with {@code keyName = "recipientName"} and {@code value = "John Doe"}.</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Creating placeholder-to-value mappings for a task and template</li>
 *   <li>Retrieving all mappings associated with a specific task</li>
 *   <li>Deleting individual mapper entries</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TemplateMapperService {

    private final TemplateMapperRepository templateMapperRepository;
    private final TaskRepository taskRepository;
    private final TemplateRepository templateRepository;

    /**
     * Creates a new template mapper entry linking a task to a template placeholder value.
     *
     * <p>Each mapper entry represents one placeholder resolution. If a template
     * has multiple placeholders, a separate mapper entry is required for each one.</p>
     *
     * @param request the mapper creation request containing taskId, templateId, keyName, and value
     * @return the persisted {@link TemplateMapper} entity
     * @throws ResourceNotFoundException if the specified task or template does not exist
     */
    public TemplateMapper createTemplateMapper(CreateTemplateMapperRequest request) {
        Task task = taskRepository.findById(request.taskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + request.taskId()));

        Template template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + request.templateId()));

        TemplateMapper mapper = TemplateMapper.builder()
                .task(task)
                .template(template)
                .keyName(request.keyName())
                .value(request.value())
                .build();

        return templateMapperRepository.save(mapper);
    }

    /**
     * Retrieves all template mapper entries associated with a specific task.
     *
     * <p>Used by the scheduler engine at execution time to resolve all
     * placeholder values for a task's email template.</p>
     *
     * @param taskId the unique identifier of the task
     * @return a list of {@link TemplateMapper} entities for the given task,
     *         or an empty list if no mappings exist
     */
    public List<TemplateMapper> getMappersByTaskId(Long taskId) {
        return templateMapperRepository.findAllByTaskId(taskId);
    }

    /**
     * Deletes a template mapper entry by its ID.
     *
     * <p>This is a hard delete. Once removed, the placeholder will no longer
     * be resolved for the associated task unless a new mapper entry is created.</p>
     *
     * @param id the unique identifier of the mapper entry to delete
     * @throws ResourceNotFoundException if no mapper entry exists with the given ID
     */
    public void deleteMapper(Long id) {
        if (!templateMapperRepository.existsById(id)) {
            throw new ResourceNotFoundException("Template mapper not found with id: " + id);
        }
        templateMapperRepository.deleteById(id);
    }
}
