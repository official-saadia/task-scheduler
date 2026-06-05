package com.taskscheduler.service;

import com.taskscheduler.entity.TaskDlq;
import com.taskscheduler.enums.DlqStatus;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.repository.TaskDlqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for managing the Dead Letter Queue (DLQ).
 *
 * <p>The Dead Letter Queue holds task execution records that have exhausted
 * all retry attempts (default: 3) and could not be completed successfully.
 * Each entry represents a task that requires manual investigation and resolution.</p>
 *
 * <p>DLQ entries progress through the following statuses:</p>
 * <ul>
 *   <li>{@link DlqStatus#NEW} — entry just arrived in the DLQ, not yet reviewed</li>
 *   <li>{@link DlqStatus#IN_PROGRESS} — currently being investigated by a developer</li>
 *   <li>{@link DlqStatus#ANALYSED} — root cause identified, fix pending</li>
 *   <li>{@link DlqStatus#FIXED} — issue resolved, no further action required</li>
 * </ul>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Retrieving DLQ entries for manual review</li>
 *   <li>Updating the status of DLQ entries as they are investigated</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TaskDlqService {

    private final TaskDlqRepository taskDlqRepository;

    /**
     * Retrieves all DLQ entries regardless of their status.
     *
     * @return a list of all {@link TaskDlq} entities,
     *         or an empty list if the DLQ is empty
     */
    public List<TaskDlq> getAllDlqEntries() {
        return taskDlqRepository.findAll();
    }

    /**
     * Retrieves a single DLQ entry by its ID.
     *
     * @param id the unique identifier of the DLQ entry
     * @return the {@link TaskDlq} entity
     * @throws ResourceNotFoundException if no DLQ entry exists with the given ID
     */
    public TaskDlq getDlqEntryById(Long id) {
        return taskDlqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DLQ entry not found with id: " + id));
    }

    /**
     * Retrieves all DLQ entries matching a specific status.
     *
     * <p>Commonly used to fetch all {@link DlqStatus#NEW} entries
     * that are pending review.</p>
     *
     * @param status the DLQ status to filter by
     * @return a list of {@link TaskDlq} entities with the given status,
     *         or an empty list if none match
     */
    public List<TaskDlq> getDlqEntriesByStatus(DlqStatus status) {
        return taskDlqRepository.findAllByStatus(status);
    }

    /**
     * Updates the status of a DLQ entry to reflect its current investigation state.
     *
     * <p>This method is used by developers to track the progress of manual
     * investigation and resolution of failed tasks.</p>
     *
     * @param id     the unique identifier of the DLQ entry to update
     * @param status the new {@link DlqStatus} to set
     * @return the updated {@link TaskDlq} entity
     * @throws ResourceNotFoundException if no DLQ entry exists with the given ID
     */
    public TaskDlq updateDlqStatus(Long id, DlqStatus status) {
        TaskDlq dlqEntry = taskDlqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DLQ entry not found with id: " + id));
        dlqEntry.setStatus(status);
        return taskDlqRepository.save(dlqEntry);
    }
}
