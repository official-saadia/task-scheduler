package com.taskscheduler.repository;

import com.taskscheduler.entity.TemplateMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TemplateMapperRepository extends JpaRepository<TemplateMapper, Long> {
    List<TemplateMapper> findAllByTaskId(Long taskId);
}
