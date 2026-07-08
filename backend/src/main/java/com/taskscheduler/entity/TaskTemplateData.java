package com.taskscheduler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing the runtime data required to execute a task's template.
 *
 * <p>The {@code data} field stores a JSON payload whose structure depends
 * on the task type:</p>
 * <ul>
 *   <li><b>EMAIL_NOTIFICATION</b> — a JSON array of recipient objects,
 *       each containing a {@code recipient} field (email address) and
 *       any number of placeholder key-value pairs for template resolution</li>
 *   <li><b>Future task types</b> — task-specific JSON structure parsed
 *       by their respective {@link com.taskscheduler.scheduler.handler.TaskHandler}</li>
 * </ul>
 *
 * <p>Example {@code data} for EMAIL_NOTIFICATION:</p>
 * <pre>
 * [
 *   {"recipient": "john@example.com", "recipientName": "John Doe", "username": "johndoe"},
 *   {"recipient": "jane@example.com", "recipientName": "Jane Smith", "username": "janesmith"}
 * ]
 * </pre>
 */
@Entity
@Table(name = "task_template_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTemplateData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    /**
     * JSON payload containing the runtime data for this task's template execution.
     * Structure varies by task type — see class-level documentation for details.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String data;

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
