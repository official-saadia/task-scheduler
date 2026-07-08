package com.taskscheduler.repository;

import com.taskscheduler.entity.TaskExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskExecutionLogRepository extends JpaRepository<TaskExecutionLog, Long> {
    List<TaskExecutionLog> findAllByTaskExecutionId(Long taskExecutionId);
}
