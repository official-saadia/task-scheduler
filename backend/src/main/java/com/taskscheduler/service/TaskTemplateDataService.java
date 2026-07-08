package com.taskscheduler.service;

import com.taskscheduler.dto.request.CreateTaskTemplateDataRequest;
import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskTemplateData;
import com.taskscheduler.entity.Template;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.TaskRepository;
import com.taskscheduler.repository.TaskTemplateDataRepository;
import com.taskscheduler.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for managing task template data.
 *
 * <p>All methods are wrapped in {@link Transactional} to keep the Hibernate
 * session open for the duration of each operation, preventing lazy loading
 * issues when accessing {@code task}, {@code template}, and
 * {@code task.smtpConfiguration} relationships. The repository uses
 * {@link org.springframework.data.jpa.repository.EntityGraph} to fetch
 * these eagerly in a single query.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TaskTemplateDataService {

    private final TaskTemplateDataRepository taskTemplateDataRepository;
    private final TaskRepository taskRepository;
    private final TemplateRepository templateRepository;

    /**
     * Creates a new task template data record.
     *
     * @param request the creation request containing taskId, templateId, and JSON data
     * @return the persisted {@link TaskTemplateData} entity
     * @throws ResourceNotFoundException if the task or template does not exist
     */
    public TaskTemplateData create(CreateTaskTemplateDataRequest request) {
        Task task = taskRepository.findById(request.taskId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task not found with id: " + request.taskId()));

        Template template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Template not found with id: " + request.templateId()));

        TaskTemplateData taskTemplateData = TaskTemplateData.builder()
                .task(task)
                .template(template)
                .data(request.data())
                .build();

        return taskTemplateDataRepository.save(taskTemplateData);
    }

    /**
     * Retrieves the template data for a specific task, with {@code task},
     * {@code template}, and {@code task.smtpConfiguration} eagerly loaded
     * via {@link org.springframework.data.jpa.repository.EntityGraph}.
     *
     * @param taskId the unique identifier of the task
     * @return the {@link TaskTemplateData} entity
     * @throws ResourceNotFoundException if no template data exists for the given task
     */
    @Transactional(readOnly = true)
    public TaskTemplateData getByTaskId(Long taskId) {
        return taskTemplateDataRepository.findByTaskId(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task template data not found for task id: " + taskId));
    }

    /**
     * Updates the JSON data of an existing task template data record.
     *
     * @param id   the unique identifier of the task template data record
     * @param data the new JSON payload
     * @return the updated {@link TaskTemplateData} entity
     * @throws ResourceNotFoundException if no record exists with the given ID
     */
    public TaskTemplateData updateData(Long id, String data) {
        TaskTemplateData taskTemplateData = findById(id);
        taskTemplateData.setData(data);
        return taskTemplateDataRepository.save(taskTemplateData);
    }

    /**
     * Deletes a task template data record by its ID.
     *
     * @param id the unique identifier of the record to delete
     * @throws ResourceNotFoundException if no record exists with the given ID
     */
    public void delete(Long id) {
        if (!taskTemplateDataRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Task template data not found with id: " + id);
        }
        taskTemplateDataRepository.deleteById(id);
    }

    /**
     * Finds a task template data record by ID or throws {@link ResourceNotFoundException}.
     *
     * @param id the task template data ID
     * @return the {@link TaskTemplateData} entity
     * @throws ResourceNotFoundException if no record exists with the given ID
     */
    private TaskTemplateData findById(Long id) {
        return taskTemplateDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task template data not found with id: " + id));
    }
}
