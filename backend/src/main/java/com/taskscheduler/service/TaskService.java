package com.taskscheduler.service;

import com.taskscheduler.dto.request.CreateTaskRequest;
import com.taskscheduler.dto.request.UpdateTaskRequest;
import com.taskscheduler.dto.response.TaskResponse;
import com.taskscheduler.entity.SmtpConfiguration;
import com.taskscheduler.entity.Task;
import com.taskscheduler.enums.TaskType;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.SmtpConfigurationRepository;
import com.taskscheduler.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final SmtpConfigurationRepository smtpConfigurationRepository;

    public TaskResponse createTask(CreateTaskRequest request) {
        SmtpConfiguration smtp = findSmtpConfigurationById(request.smtpConfigurationId());
        Task task = Task.builder()
                .name(request.name())
                .type(TaskType.valueOf(request.type()))
                .cronExpression(request.cronExpression())
                .smtpConfiguration(smtp)
                .isActive(true)
                .build();
        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(int page, int size) {
        return taskRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        return toResponse(findTaskById(id));
    }

    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = findTaskById(id);
        SmtpConfiguration smtp = findSmtpConfigurationById(request.smtpConfigurationId());
        task.setName(request.name());
        task.setType(TaskType.valueOf(request.type()));
        task.setCronExpression(request.cronExpression());
        task.setSmtpConfiguration(smtp);
        task.setIsActive(request.isActive());
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse deactivateTask(Long id) {
        Task task = findTaskById(id);
        task.setIsActive(false);
        return toResponse(taskRepository.save(task));
    }

    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private SmtpConfiguration findSmtpConfigurationById(Long id) {
        return smtpConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMTP configuration not found with id: " + id));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(), task.getName(), task.getType().name(),
                task.getCronExpression(), task.getSmtpConfiguration().getId(),
                task.getIsActive(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
