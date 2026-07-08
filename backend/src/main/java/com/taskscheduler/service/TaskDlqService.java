package com.taskscheduler.service;

import com.taskscheduler.entity.TaskDlq;
import com.taskscheduler.enums.DlqStatus;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.TaskDlqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskDlqService {

    private final TaskDlqRepository taskDlqRepository;

    @Transactional(readOnly = true)
    public Page<TaskDlq> getAllDlqEntries(int page, int size) {
        return taskDlqRepository
                .findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public TaskDlq getDlqEntryById(Long id) {
        return findById(id);
    }

    @Transactional(readOnly = true)
    public Page<TaskDlq> getDlqEntriesByStatus(DlqStatus status, int page, int size) {
        return taskDlqRepository
                .findAllByStatus(status, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public TaskDlq updateDlqStatus(Long id, DlqStatus status) {
        TaskDlq entry = findById(id);
        entry.setStatus(status);
        return taskDlqRepository.save(entry);
    }

    private TaskDlq findById(Long id) {
        return taskDlqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DLQ entry not found with id: " + id));
    }
}
