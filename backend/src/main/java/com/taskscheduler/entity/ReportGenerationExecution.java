package com.taskscheduler.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taskscheduler.enums.BackupStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Records a single attempt of a {@code REPORT_GENERATION} task — the
 * command that was run, its exit code, whether the expected output file
 * was actually produced, and the resulting status.
 *
 * <p>Analogous to {@link BackupExecution} for {@code DATABASE_BACKUP}
 * tasks. Reuses {@link BackupStatus} since the SUCCESS/FAILED semantics
 * are identical.</p>
 */
@Entity
@Table(name = "report_generation_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReportGenerationExecution {

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

    @Column(name = "output_file_path", nullable = false)
    private String outputFilePath;

    /**
     * Whether {@link #outputFilePath} existed on disk after the command
     * finished. A false value here fails the execution even if
     * {@link #exitCode} was 0.
     */
    @Column(name = "file_produced", nullable = false)
    private Boolean fileProduced = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupStatus status;

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
