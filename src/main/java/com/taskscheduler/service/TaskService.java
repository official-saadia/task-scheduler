package com.taskscheduler.service;

import com.taskscheduler.dto.request.CreateTaskRequest;
import com.taskscheduler.dto.response.TaskResponse;
import com.taskscheduler.entity.SmtpConfiguration;
import com.taskscheduler.entity.Task;
import com.taskscheduler.enums.TaskType;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.SmtpConfigurationRepository;
import com.taskscheduler.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for managing scheduled tasks.
 * Handles task creation, retrieval, and deactivation.
 * Tasks are picked up by the scheduler engine based on their cron expression and active status.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final SmtpConfigurationRepository smtpConfigurationRepository;

    /**
     * Creates a new scheduled task and persists it to the database.
     * The task will be eligible for execution once the scheduler engine picks it up.
     *
     * @param request the task creation request
     * @return the created task as a response DTO
     * @throws ResourceNotFoundException if the provided SMTP configuration ID does not exist
     * @throws IllegalArgumentException  if the task type is not a valid {@link TaskType}
     */
    public TaskResponse createTask(CreateTaskRequest request) {
        SmtpConfiguration smtpConfiguration = smtpConfigurationRepository.findById(request.smtpConfigurationId())
                .orElseThrow(() -> new ResourceNotFoundException("SMTP configuration not found with id: " + request.smtpConfigurationId()));

        Task task = Task.builder()
                .name(request.name())
                .type(TaskType.valueOf(request.type()))
                .cronExpression(request.cronExpression())
                .smtpConfiguration(smtpConfiguration)
                .isActive(true)
                .build();

        return toResponse(taskRepository.save(task));
    }

    /**
     * Retrieves all tasks regardless of their active status.
     *
     * @return list of all tasks as response DTOs
     */
    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves a single task by its ID.
     *
     * @param id the task ID
     * @return the task as a response DTO
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return toResponse(task);
    }

    /**
     * Deactivates a task by setting its active flag to false.
     * Deactivated tasks will no longer be executed by the scheduler engine.
     *
     * @param id the task ID to deactivate
     * @return the updated task as a response DTO
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    public TaskResponse deactivateTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        task.setIsActive(false);
        return toResponse(taskRepository.save(task));
    }

    /**
     * Maps a {@link Task} entity to a {@link TaskResponse} DTO.
     *
     * @param task the task entity
     * @return the mapped response DTO
     */
    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.getType().name(),
                task.getCronExpression(),
                task.getSmtpConfiguration().getId(),
                task.getIsActive(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
