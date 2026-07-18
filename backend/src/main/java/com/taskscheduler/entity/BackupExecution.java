package com.taskscheduler.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taskscheduler.enums.BackupStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Records a single attempt of a {@code DATABASE_BACKUP} task — the command
 * that was run, its exit code, captured output, and the resulting status.
 *
 * <p>Analogous to {@link EmailNotification} for {@code EMAIL_NOTIFICATION}
 * tasks. One row is persisted per execution attempt.</p>
 */
@Entity
@Table(name = "backup_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BackupExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_execution_id", nullable = false)
    private TaskExecution taskExecution;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String command;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupStatus status;

    /**
     * Combined stdout/stderr captured from the command, truncated to a
     * reasonable size before persisting.
     */
    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo = 1;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
