package com.taskscheduler.repository;

import com.taskscheduler.entity.TaskDlq;
import com.taskscheduler.enums.DlqStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for managing {@link TaskDlq} entities with pagination support.
 */
public interface TaskDlqRepository extends JpaRepository<TaskDlq, Long> {

    @EntityGraph(attributePaths = {"task", "taskExecution"})
    Page<TaskDlq> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"task", "taskExecution"})
    Page<TaskDlq> findAllByStatus(DlqStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "taskExecution"})
    Page<TaskDlq> findAllByTaskId(Long taskId, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "taskExecution"})
    Optional<TaskDlq> findById(Long id);
}
