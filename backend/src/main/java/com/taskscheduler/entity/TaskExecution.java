package com.taskscheduler.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taskscheduler.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "task_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TaskExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "smtpConfiguration"})
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Logs excluded from default serialization to prevent infinite recursion.
     * Access logs via a dedicated endpoint if needed.
     */
    @JsonIgnoreProperties({"taskExecution"})
    @OneToMany(mappedBy = "taskExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TaskExecutionLog> logs;

    @JsonIgnoreProperties({"taskExecution"})
    @OneToMany(mappedBy = "taskExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EmailNotification> emailNotifications;

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
