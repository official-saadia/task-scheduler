package com.taskscheduler.repository;

import com.taskscheduler.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Task} entities with server-side pagination support.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"smtpConfiguration", "backupConfiguration", "reportConfiguration"})
    Page<Task> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"smtpConfiguration", "backupConfiguration", "reportConfiguration"})
    Page<Task> findAllByIsActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"smtpConfiguration", "backupConfiguration", "reportConfiguration"})
    Optional<Task> findById(Long id);

    @EntityGraph(attributePaths = {"smtpConfiguration", "backupConfiguration", "reportConfiguration"})
    List<Task> findAllByIsActiveTrue();

    /**
     * Selects active tasks that are due to run, plus any whose
     * {@code nextExecutionTime} has not been initialised yet.
     *
     * <p>Replaces the old approach of loading every active task and
     * re-deriving dueness from the cron expression on each poll, which had
     * no way of knowing a task had already run.</p>
     *
     * @param now the current time
     * @return tasks due at or before {@code now}, and uninitialised ones
     */
    @EntityGraph(attributePaths = {"smtpConfiguration", "backupConfiguration", "reportConfiguration"})
    @Query("SELECT t FROM Task t WHERE t.isActive = true "
            + "AND (t.nextExecutionTime IS NULL OR t.nextExecutionTime <= :now)")
    List<Task> findDueTasks(@Param("now") LocalDateTime now);
}
