package com.taskscheduler.repository;

import com.taskscheduler.entity.TaskDlq;
import com.taskscheduler.enums.DlqStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * Fetches every DLQ entry created within the given window, oldest last,
     * for CSV export. Unpaginated by design — exports are expected to cover
     * bounded date ranges (a day, a month), not the entire table.
     */
    @EntityGraph(attributePaths = {"task", "taskExecution"})
    List<TaskDlq> findAllByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    /**
     * As {@link #findAllByCreatedAtBetweenOrderByCreatedAtDesc}, but restricted
     * to a single {@link DlqStatus} so the export can honour the status filter
     * applied on the DLQ page.
     */
    @EntityGraph(attributePaths = {"task", "taskExecution"})
    List<TaskDlq> findAllByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            DlqStatus status, LocalDateTime from, LocalDateTime to);
}
