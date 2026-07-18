package com.taskscheduler.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Configuration describing how Task Scheduler should trigger an external
 * report-generation process.
 *
 * <p>Identical model to {@link BackupConfiguration}: Task Scheduler runs
 * {@link #command} and checks its exit code. It has no knowledge of what
 * the report actually contains or how it's built (a DB query, a script
 * calling this app's own APIs, a BI tool export, etc). The one extra
 * check it performs is confirming {@link #outputFilePath} exists after
 * the command finishes, since a report task that exits 0 without
 * producing a file is still a failure.</p>
 */
@Entity
@Table(name = "report_generation_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReportGenerationConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * The shell command executed to generate the report, e.g.
     * {@code /opt/scripts/dlq_report.sh} or a script that calls this
     * app's own {@code /api/v1/task-dlq} endpoint and writes a CSV.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String command;

    /**
     * Optional working directory the command is executed from.
     */
    @Column(name = "working_directory")
    private String workingDirectory;

    /**
     * Path Task Scheduler checks for existence after the command runs.
     * Typically the file the command is expected to write the report to.
     * This is also the path you'd point an EMAIL_NOTIFICATION task's
     * attachmentPath at to email the generated report.
     */
    @Column(name = "output_file_path", nullable = false)
    private String outputFilePath;

    @Column(name = "timeout_seconds", nullable = false)
    @Builder.Default
    private Integer timeoutSeconds = 300;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
