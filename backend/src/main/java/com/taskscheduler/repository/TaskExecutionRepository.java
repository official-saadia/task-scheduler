package com.taskscheduler.repository;

import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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

    /**
     * Fetches every execution in any of the given statuses, task eagerly loaded.
     * Used once at startup to rebuild the in-memory retry queue from the rows a
     * previous run left behind ({@code IN_PROGRESS}, and any {@code FAILED} row
     * that still has retries left). The caller groups these by task and resumes
     * only the newest row per task, skipping the older stale ones. Unpaginated by
     * design: the set of interrupted runs is small and read exactly once.
     */
    @EntityGraph(attributePaths = {"task"})
    List<TaskExecution> findByStatusIn(Collection<ExecutionStatus> statuses);

    /**
     * Counts executions created on or after {@code since}, grouped by status.
     * Each row is {@code [ExecutionStatus, Long]}. Backs the per-period dashboard
     * cards (today / this week / this month) with a single query per window
     * instead of one count query per status.
     */
    @Query("SELECT te.status, COUNT(te) FROM TaskExecution te WHERE te.createdAt >= :since GROUP BY te.status")
    List<Object[]> countByStatusSince(@Param("since") LocalDateTime since);
}
