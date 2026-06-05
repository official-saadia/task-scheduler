package com.taskscheduler.repository;

import com.taskscheduler.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByIsActiveTrue();
}
