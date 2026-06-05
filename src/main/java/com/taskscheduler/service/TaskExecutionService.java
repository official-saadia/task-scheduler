package com.taskscheduler.service;

import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.enums.ExecutionStatus;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.TaskExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for managing task executions.
 *
 * <p>A {@link TaskExecution} record is created each time the scheduler engine
 * triggers a task. It tracks the lifecycle of a single execution including
 * its status, retry count, and timestamps.</p>
 *
 * <p>This service is primarily used for:</p>
 * <ul>
 *   <li>Querying execution history for a specific task</li>
 *   <li>Retrieving executions by status for monitoring and alerting</li>
 *   <li>Supporting analytics and audit requirements</li>
 * </ul>
 *
 * <p>Note: Task execution records are created and updated by the scheduler engine,
 * not through the API. This service exposes read-only operations for external consumers.</p>
 */
@Service
@RequiredArgsConstructor
public class TaskExecutionService {

    private final TaskExecutionRepository taskExecutionRepository;

    /**
     * Retrieves all task execution records.
     *
     * @return a list of all {@link TaskExecution} entities,
     *         or an empty list if none exist
     */
    public List<TaskExecution> getAllExecutions() {
        return taskExecutionRepository.findAll();
    }

    /**
     * Retrieves a single task execution record by its ID.
     *
     * @param id the unique identifier of the task execution
     * @return the {@link TaskExecution} entity
     * @throws ResourceNotFoundException if no execution record exists with the given ID
     */
    public TaskExecution getExecutionById(Long id) {
        return taskExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task execution not found with id: " + id));
    }

    /**
     * Retrieves all execution records for a specific task.
     *
     * <p>Useful for reviewing the full execution history of a task,
     * including all retries and their outcomes.</p>
     *
     * @param taskId the unique identifier of the task
     * @return a list of {@link TaskExecution} entities for the given task,
     *         or an empty list if no executions exist
     */
    public List<TaskExecution> getExecutionsByTaskId(Long taskId) {
        return taskExecutionRepository.findAllByTaskId(taskId);
    }

    /**
     * Retrieves all task execution records matching a specific status.
     *
     * <p>Commonly used to query {@link ExecutionStatus#FAILED} or
     * {@link ExecutionStatus#IN_PROGRESS} executions for monitoring purposes.</p>
     *
     * @param status the execution status to filter by
     * @return a list of {@link TaskExecution} entities with the given status,
     *         or an empty list if none match
     */
    public List<TaskExecution> getExecutionsByStatus(ExecutionStatus status) {
        return taskExecutionRepository.findAllByStatus(status);
    }
}
