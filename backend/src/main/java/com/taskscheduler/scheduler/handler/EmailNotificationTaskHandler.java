package com.taskscheduler.scheduler.handler;

import com.taskscheduler.email.EmailResolvedTemplate;
import com.taskscheduler.email.EmailService;
import com.taskscheduler.email.EmailTemplateResolver;
import com.taskscheduler.email.ResolvedEmailMessage;
import com.taskscheduler.entity.*;
import com.taskscheduler.enums.NotificationStatus;
import com.taskscheduler.enums.TaskType;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.exception.TaskExecutionException;
import com.taskscheduler.repository.EmailNotificationRepository;
import com.taskscheduler.repository.TaskTemplateDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;

/**
 * Task handler responsible for executing email notification tasks.
 *
 * <p>Implements {@link TaskHandler} for the {@link TaskType#EMAIL_NOTIFICATION} type.
 * Fetches {@link TaskTemplateData}, delegates resolution to {@link EmailTemplateResolver},
 * dispatches personalized emails via {@link EmailService}, and persists one
 * {@link EmailNotification} record per recipient.</p>
 *
 * <p>Partial success: if some recipients succeed and others fail, the task
 * completes normally. Only if ALL recipients fail is a {@link TaskExecutionException}
 * thrown to trigger the retry mechanism.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationTaskHandler implements TaskHandler {

    private final EmailService emailService;
    private final EmailTemplateResolver emailTemplateResolver;
    private final TaskTemplateDataRepository taskTemplateDataRepository;
    private final EmailNotificationRepository emailNotificationRepository;

    /**
     * Executes an email notification task.
     *
     * @param task          the task entity containing SMTP configuration
     * @param taskExecution the current execution record
     * @throws TaskExecutionException    if a configured attachment is missing, or
     *                                   if all recipient emails fail to send
     * @throws ResourceNotFoundException if no template data exists for the task
     */
    @Override
    public void execute(Task task, TaskExecution taskExecution) {
        log.info("EmailNotificationTaskHandler: Executing task [{}] '{}'",
                task.getId(), task.getName());

        verifyAttachmentIsReadable(task);

        TaskTemplateData taskTemplateData = taskTemplateDataRepository
                .findByTaskId(task.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No template data found for task: " + task.getId()));

        EmailResolvedTemplate resolved = emailTemplateResolver.resolve(
                taskTemplateData.getTemplate().getTemplate(), taskTemplateData);

        log.info("EmailNotificationTaskHandler: Sending {} personalized email(s) for task [{}]",
                resolved.messages().size(), task.getId());

        int successCount = 0;
        int failureCount = 0;

        for (ResolvedEmailMessage message : resolved.messages()) {
            try {
                emailService.sendEmail(
                        message.recipient(),
                        message.subject(),
                        message.body(),
                        task.getSmtpConfiguration(),
                        task.getAttachmentPath()
                );

                persistNotification(taskExecution, message.body(), message.recipient(),
                        NotificationStatus.SUCCESS, taskExecution.getRetryCount() + 1);

                successCount++;
                log.info("EmailNotificationTaskHandler: Email sent to [{}]", message.recipient());

            } catch (Exception ex) {
                persistNotification(taskExecution, ex.getMessage(), message.recipient(),
                        NotificationStatus.FAILED, taskExecution.getRetryCount() + 1);

                failureCount++;
                log.error("EmailNotificationTaskHandler: Failed to send to [{}]. Reason: {}",
                        message.recipient(), ex.getMessage());
            }
        }

        log.info("EmailNotificationTaskHandler: Task [{}] done. Success: {} | Failed: {}",
                task.getId(), successCount, failureCount);

        if (successCount == 0) {
            throw new TaskExecutionException(
                    "All " + failureCount + " email(s) failed for task: " + task.getId());
        }
    }

    /**
     * Fails the task if it configures an attachment that is not on disk.
     *
     * <p>An attachment path is only ever set deliberately — typically pointing at
     * a REPORT_GENERATION task's output file — so a missing file means the report
     * never ran, failed, or wrote somewhere else. Sending the email anyway would
     * deliver a message whose whole purpose was the attachment, and the recipient
     * would have no way of knowing anything went wrong. Failing instead routes the
     * task through the normal retry/DLQ path, where the missing path is visible
     * and actionable.</p>
     *
     * <p>Checked before any email is dispatched, so a failure here means nothing
     * was sent rather than some recipients getting an incomplete message.</p>
     *
     * @param task the task being executed
     * @throws TaskExecutionException if the attachment is missing or unreadable
     */
    private void verifyAttachmentIsReadable(Task task) {
        String attachmentPath = task.getAttachmentPath();

        if (attachmentPath == null || attachmentPath.isBlank()) {
            return;
        }

        File attachment = new File(attachmentPath);

        if (!attachment.isFile()) {
            throw new TaskExecutionException(
                    "Attachment not found at '" + attachmentPath + "' for task: " + task.getId()
                            + ". No emails were sent. The task that produces this file may have failed "
                            + "or not run yet.");
        }

        if (!attachment.canRead()) {
            throw new TaskExecutionException(
                    "Attachment at '" + attachmentPath + "' exists but is not readable for task: "
                            + task.getId() + ". No emails were sent.");
        }
    }

    /**
     * Returns the task type this handler supports.
     *
     * @return {@link TaskType#EMAIL_NOTIFICATION}
     */
    @Override
    public TaskType getTaskType() {
        return TaskType.EMAIL_NOTIFICATION;
    }

    /**
     * Persists an {@link EmailNotification} record for a single recipient.
     *
     * @param taskExecution the execution record this notification belongs to
     * @param emailMessage  the resolved email body or failure reason
     * @param recipient     the recipient email address
     * @param status        the notification status
     * @param attemptNo     the attempt number
     */
    private void persistNotification(TaskExecution taskExecution, String emailMessage,
                                     String recipient, NotificationStatus status, int attemptNo) {
        EmailNotification notification = EmailNotification.builder()
                .taskExecution(taskExecution)
                .emailMessage(emailMessage)
                .status(status)
                .attemptNo(attemptNo)
                .sentAt(status == NotificationStatus.SUCCESS ? LocalDateTime.now() : null)
                .build();
        emailNotificationRepository.save(notification);
    }
}
