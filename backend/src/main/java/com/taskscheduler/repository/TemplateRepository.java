package com.taskscheduler.repository;

import com.taskscheduler.entity.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing {@link Template} entities with server-side pagination support.
 */
public interface TemplateRepository extends JpaRepository<Template, Long> {

    Page<Template> findAll(Pageable pageable);

    Page<Template> findAllByIsActiveTrue(Pageable pageable);
}
