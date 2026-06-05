package com.taskscheduler.repository;

import com.taskscheduler.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findAllByIsActiveTrue();
}
