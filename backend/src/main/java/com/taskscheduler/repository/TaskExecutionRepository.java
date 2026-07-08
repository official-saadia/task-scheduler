package com.taskscheduler.repository;

import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for managing {@link TaskExecution} entities with pagination support.
 */
public interface TaskExecutionRepository extends JpaRepository<TaskExecution, Long> {

    @EntityGraph(attributePaths = {"task"})
    Page<TaskExecution> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"task"})
    Page<TaskExecution> findAllByTaskId(Long taskId, Pageable pageable);

    @EntityGraph(attributePaths = {"task"})
    Page<TaskExecution> findAllByStatus(ExecutionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"task"})
    Optional<TaskExecution> findById(Long id);

    long countByStatus(ExecutionStatus status);
}
