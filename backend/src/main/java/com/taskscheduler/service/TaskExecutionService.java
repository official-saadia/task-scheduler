package com.taskscheduler.service;

import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.enums.ExecutionStatus;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.TaskExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskExecutionService {

    private final TaskExecutionRepository taskExecutionRepository;

    public Page<TaskExecution> getAllExecutions(int page, int size) {
        return taskExecutionRepository
                .findAll(PageRequest.of(page, size, Sort.by("startedAt").descending()));
    }

    public TaskExecution getExecutionById(Long id) {
        return taskExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task execution not found with id: " + id));
    }

    public Page<TaskExecution> getExecutionsByTaskId(Long taskId, int page, int size) {
        return taskExecutionRepository
                .findAllByTaskId(taskId, PageRequest.of(page, size, Sort.by("startedAt").descending()));
    }

    public Page<TaskExecution> getExecutionsByStatus(ExecutionStatus status, int page, int size) {
        return taskExecutionRepository
                .findAllByStatus(status, PageRequest.of(page, size, Sort.by("startedAt").descending()));
    }
}
