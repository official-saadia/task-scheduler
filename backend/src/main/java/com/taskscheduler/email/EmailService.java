package com.taskscheduler.email;

import com.taskscheduler.entity.SmtpConfiguration;
import com.taskscheduler.exception.SmtpConfigurationException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Properties;

/**
 * Service responsible for sending email notifications via SMTP.
 *
 * <p>Dynamically builds a {@link JavaMailSenderImpl} at runtime using the
 * {@link SmtpConfiguration} associated with the task. This allows different
 * tasks to use different SMTP configurations without requiring application restart.</p>
 */
@Slf4j
@Service
public class EmailService {

    /**
     * Sends a plain text email using the given SMTP configuration.
     *
     * @param to                the recipient email address
     * @param subject           the email subject line
     * @param body              the resolved email body with all placeholders filled in
     * @param smtpConfiguration the SMTP configuration to use for sending
     * @throws SmtpConfigurationException if the SMTP configuration is invalid
     *                                    or the mail server cannot be reached
     */
    public void sendEmail(String to, String subject, String body, SmtpConfiguration smtpConfiguration) {
        sendEmail(to, subject, body, smtpConfiguration, null);
    }

    /**
     * Sends an email using the given SMTP configuration, optionally attaching a
     * single file from disk.
     *
     * <p>Task Scheduler does not generate or validate the contents of the
     * attached file — it simply attaches whatever is at {@code attachmentPath}
     * at send time. If the path is set but the file does not exist, the send
     * fails loudly rather than silently sending without it, since a report
     * email with a missing report is not a successful send.</p>
     *
     * @param to                the recipient email address
     * @param subject           the email subject line
     * @param body              the resolved email body with all placeholders filled in
     * @param smtpConfiguration the SMTP configuration to use for sending
     * @param attachmentPath    optional path to a file to attach; null/blank for no attachment
     * @throws SmtpConfigurationException if the SMTP configuration is invalid, the mail
     *                                    server cannot be reached, or the attachment is missing
     */
    public void sendEmail(String to, String subject, String body, SmtpConfiguration smtpConfiguration,
                           String attachmentPath) {
        boolean hasAttachment = attachmentPath != null && !attachmentPath.isBlank();
        try {
            JavaMailSenderImpl mailSender = buildMailSender(smtpConfiguration);

            if (!hasAttachment) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(smtpConfiguration.getUsername());
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
            } else {
                File file = new File(attachmentPath);
                if (!file.exists() || !file.isFile()) {
                    throw new SmtpConfigurationException(
                            "Attachment not found at path: " + attachmentPath);
                }

                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.setFrom(smtpConfiguration.getUsername());
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(body);
                helper.addAttachment(file.getName(), file);
                mailSender.send(mimeMessage);
            }

            log.info("Email sent successfully to: {}{}", to, hasAttachment ? " with attachment: " + attachmentPath : "");

        } catch (SmtpConfigurationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to send email to: {}. Reason: {}", to, ex.getMessage());
            throw new SmtpConfigurationException(
                    "Failed to send email to: " + to + ". Reason: " + ex.getMessage(), ex);
        }
    }

    /**
     * Dynamically builds a {@link JavaMailSenderImpl} from the given SMTP configuration.
     *
     * <p>Configures SMTP authentication and TLS properties for Gmail compatibility.
     * These settings can be extended to support other providers in future iterations.</p>
     *
     * @param config the {@link SmtpConfiguration} containing host, port, and credentials
     * @return a configured and ready-to-use {@link JavaMailSenderImpl}
     */
    private JavaMailSenderImpl buildMailSender(SmtpConfiguration config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }
}
