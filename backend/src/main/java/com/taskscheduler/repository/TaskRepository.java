package com.taskscheduler.repository;

import com.taskscheduler.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Task} entities with server-side pagination support.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"smtpConfiguration"})
    Page<Task> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"smtpConfiguration"})
    Page<Task> findAllByIsActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"smtpConfiguration"})
    Optional<Task> findById(Long id);

    @EntityGraph(attributePaths = {"smtpConfiguration"})
    List<Task> findAllByIsActiveTrue();
}
