package com.taskscheduler.repository;

import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, Long> {
    List<TaskExecution> findAllByTaskId(Long taskId);
    List<TaskExecution> findAllByStatus(ExecutionStatus status);
    long countByStatus(ExecutionStatus status);
}
