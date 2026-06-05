package com.taskscheduler.repository;

import com.taskscheduler.entity.TaskDlq;
import com.taskscheduler.enums.DlqStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskDlqRepository extends JpaRepository<TaskDlq, Long> {
    List<TaskDlq> findAllByStatus(DlqStatus status);
    List<TaskDlq> findAllByTaskId(Long taskId);
}
