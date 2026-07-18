package com.taskscheduler.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Configuration describing how Task Scheduler should trigger an external
 * backup process.
 *
 * <p>Task Scheduler only knows how to run {@link #command} and observe its
 * exit code — it has no knowledge of what the command actually does (e.g.
 * {@code pg_dump}, a custom script, an S3 sync). Correctness of the backup
 * itself is entirely the responsibility of that external command.</p>
 */
@Entity
@Table(name = "backup_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BackupConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * The shell command executed to trigger the backup, e.g.
     * {@code /opt/scripts/backup.sh} or {@code pg_dump -Fc mydb > /backups/mydb.dump}.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String command;

    /**
     * Optional working directory the command is executed from.
     * Defaults to the application's working directory if not set.
     */
    @Column(name = "working_directory")
    private String workingDirectory;

    /**
     * Maximum time to wait for the command to finish before treating
     * it as a failed/timed-out execution.
     */
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
