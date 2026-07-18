package com.taskscheduler.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taskscheduler.enums.TaskType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType type;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    /**
     * Required when {@link #type} is {@code EMAIL_NOTIFICATION}, otherwise null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "smtp_configuration_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private SmtpConfiguration smtpConfiguration;

    /**
     * Required when {@link #type} is {@code DATABASE_BACKUP}, otherwise null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backup_configuration_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BackupConfiguration backupConfiguration;

    /**
     * Required when {@link #type} is {@code REPORT_GENERATION}, otherwise null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_configuration_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ReportGenerationConfiguration reportConfiguration;

    /**
     * Optional, only meaningful when {@link #type} is {@code EMAIL_NOTIFICATION}.
     * Path to a file on disk (typically a {@link ReportGenerationConfiguration}'s
     * output file) to attach to every email this task sends. Null means no
     * attachment.
     */
    @Column(name = "attachment_path")
    private String attachmentPath;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * When this task is next due to run.
     *
     * <p>Owned by {@code TaskSchedulerEngine}, which advances it to the
     * following cron occurrence as soon as the task is queued. This is what
     * makes a task stop being due the moment it is picked up, rather than
     * remaining due for as long as the poll's lookback window.</p>
     *
     * <p>{@code null} means "not yet initialised" — the scheduler computes the
     * next occurrence and stores it without running the task.</p>
     */
    @Column(name = "next_execution_time")
    private LocalDateTime nextExecutionTime;

    /**
     * When this task was last queued for execution. Diagnostic only —
     * the scheduler does not read it when deciding what is due.
     */
    @Column(name = "last_execution_time")
    private LocalDateTime lastExecutionTime;

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
