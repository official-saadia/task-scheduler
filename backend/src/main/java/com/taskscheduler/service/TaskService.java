package com.taskscheduler.service;

import com.taskscheduler.dto.request.CreateTaskRequest;
import com.taskscheduler.dto.request.UpdateTaskRequest;
import com.taskscheduler.dto.response.TaskResponse;
import com.taskscheduler.entity.BackupConfiguration;
import com.taskscheduler.entity.ReportGenerationConfiguration;
import com.taskscheduler.entity.SmtpConfiguration;
import com.taskscheduler.entity.Task;
import com.taskscheduler.enums.TaskType;
import com.taskscheduler.exception.InvalidTaskTypeException;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.BackupConfigurationRepository;
import com.taskscheduler.repository.ReportGenerationConfigurationRepository;
import com.taskscheduler.repository.SmtpConfigurationRepository;
import com.taskscheduler.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final SmtpConfigurationRepository smtpConfigurationRepository;
    private final BackupConfigurationRepository backupConfigurationRepository;
    private final ReportGenerationConfigurationRepository reportGenerationConfigurationRepository;

    public TaskResponse createTask(CreateTaskRequest request) {
        TaskType type = parseTaskType(request.type());
        Task.TaskBuilder builder = Task.builder()
                .name(request.name())
                .type(type)
                .cronExpression(request.cronExpression())
                .isActive(true)
                .nextExecutionTime(resolveNextExecutionTime(request.cronExpression()));

        applyConfiguration(builder, type, request.smtpConfigurationId(),
                request.backupConfigurationId(), request.reportConfigurationId());

        if (type == TaskType.EMAIL_NOTIFICATION) {
            builder.attachmentPath(request.attachmentPath());
        }

        return toResponse(taskRepository.save(builder.build()));
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
        TaskType type = parseTaskType(request.type());

        task.setName(request.name());
        task.setType(type);
        task.setCronExpression(request.cronExpression());
        task.setIsActive(request.isActive());

        // The cron may have changed, so any previously computed nextExecutionTime
        // is stale. Recomputing also re-arms a task that is being reactivated.
        task.setNextExecutionTime(resolveNextExecutionTime(request.cronExpression()));

        switch (type) {
            case EMAIL_NOTIFICATION -> {
                task.setSmtpConfiguration(findSmtpConfigurationById(requireId(
                        request.smtpConfigurationId(), "smtpConfigurationId", type)));
                task.setBackupConfiguration(null);
                task.setReportConfiguration(null);
                task.setAttachmentPath(request.attachmentPath());
            }
            case DATABASE_BACKUP -> {
                task.setBackupConfiguration(findBackupConfigurationById(requireId(
                        request.backupConfigurationId(), "backupConfigurationId", type)));
                task.setSmtpConfiguration(null);
                task.setReportConfiguration(null);
                task.setAttachmentPath(null);
            }
            case REPORT_GENERATION -> {
                task.setReportConfiguration(findReportConfigurationById(requireId(
                        request.reportConfigurationId(), "reportConfigurationId", type)));
                task.setSmtpConfiguration(null);
                task.setBackupConfiguration(null);
                task.setAttachmentPath(null);
            }
        }

        return toResponse(taskRepository.save(task));
    }

    public TaskResponse deactivateTask(Long id) {
        Task task = findTaskById(id);
        task.setIsActive(false);
        return toResponse(taskRepository.save(task));
    }

    private void applyConfiguration(Task.TaskBuilder builder, TaskType type, Long smtpConfigurationId,
                                     Long backupConfigurationId, Long reportConfigurationId) {
        switch (type) {
            case EMAIL_NOTIFICATION -> builder.smtpConfiguration(
                    findSmtpConfigurationById(requireId(smtpConfigurationId, "smtpConfigurationId", type)));
            case DATABASE_BACKUP -> builder.backupConfiguration(
                    findBackupConfigurationById(requireId(backupConfigurationId, "backupConfigurationId", type)));
            case REPORT_GENERATION -> builder.reportConfiguration(
                    findReportConfigurationById(requireId(reportConfigurationId, "reportConfigurationId", type)));
        }
    }

    private Long requireId(Long id, String fieldName, TaskType type) {
        if (id == null) {
            throw new InvalidTaskTypeException(fieldName + " is required for task type " + type.name());
        }
        return id;
    }

    /**
     * Resolves the first execution time for a cron expression, relative to now.
     *
     * <p>Setting this up front means {@code TaskSchedulerEngine} never has to
     * treat a freshly created task as uninitialised. The expression has already
     * passed {@code @ValidCronExpression} validation by this point; {@code null}
     * is returned only if it has no future occurrence, which leaves the task
     * dormant rather than failing the request.</p>
     *
     * @param cronExpression the task's cron expression
     * @return the next occurrence after now, or {@code null} if there is none
     */
    private LocalDateTime resolveNextExecutionTime(String cronExpression) {
        return CronExpression.parse(cronExpression).next(LocalDateTime.now());
    }

    private TaskType parseTaskType(String type) {
        try {
            return TaskType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTaskTypeException("Unsupported task type: " + type);
        }
    }

    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private SmtpConfiguration findSmtpConfigurationById(Long id) {
        return smtpConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SMTP configuration not found with id: " + id));
    }

    private BackupConfiguration findBackupConfigurationById(Long id) {
        return backupConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Backup configuration not found with id: " + id));
    }

    private ReportGenerationConfiguration findReportConfigurationById(Long id) {
        return reportGenerationConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report generation configuration not found with id: " + id));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(), task.getName(), task.getType().name(),
                task.getCronExpression(),
                task.getSmtpConfiguration() != null ? task.getSmtpConfiguration().getId() : null,
                task.getBackupConfiguration() != null ? task.getBackupConfiguration().getId() : null,
                task.getReportConfiguration() != null ? task.getReportConfiguration().getId() : null,
                task.getAttachmentPath(),
                task.getIsActive(),
                task.getNextExecutionTime(), task.getLastExecutionTime(),
                task.getCreatedAt(), task.getUpdatedAt());
    }
}
