package com.taskscheduler.repository;

import com.taskscheduler.entity.EmailNotification;
import com.taskscheduler.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {
    List<EmailNotification> findAllByTaskExecutionId(Long taskExecutionId);
    long countByStatus(NotificationStatus status);
}
