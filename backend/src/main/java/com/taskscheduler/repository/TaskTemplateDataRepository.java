package com.taskscheduler.repository;

import com.taskscheduler.entity.TaskTemplateData;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for managing {@link TaskTemplateData} entities.
 *
 * <p>Uses {@link EntityGraph} to eagerly fetch {@code template}, {@code task},
 * and {@code task.smtpConfiguration} relationships in a single query,
 * avoiding lazy loading issues during template resolution and serialization.</p>
 */
public interface TaskTemplateDataRepository extends JpaRepository<TaskTemplateData, Long> {

    /**
     * Finds the template data for a specific task with {@code template}, {@code task},
     * and {@code task.smtpConfiguration} loaded eagerly.
     *
     * @param taskId the unique identifier of the task
     * @return an {@link Optional} containing the {@link TaskTemplateData} if found
     */
    @EntityGraph(attributePaths = {"template", "task", "task.smtpConfiguration"})
    Optional<TaskTemplateData> findByTaskId(Long taskId);

    /**
     * Finds a task template data record by ID with {@code template}, {@code task},
     * and {@code task.smtpConfiguration} loaded eagerly.
     *
     * @param id the unique identifier of the task template data record
     * @return an {@link Optional} containing the {@link TaskTemplateData} if found
     */
    @EntityGraph(attributePaths = {"template", "task", "task.smtpConfiguration"})
    Optional<TaskTemplateData> findById(Long id);
}
