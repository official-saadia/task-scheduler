package com.taskscheduler.email;

import com.taskscheduler.entity.SmtpConfiguration;
import com.taskscheduler.exception.SmtpConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Service responsible for sending email notifications via SMTP.
 *
 * <p>Dynamically builds a {@link JavaMailSenderImpl} at runtime using the
 * {@link SmtpConfiguration} associated with the task. This allows different
 * tasks to use different SMTP configurations without requiring application restart.</p>
 *
 * <p>Currently supports plain text emails. HTML email support can be added
 * in a future iteration using {@code MimeMessageHelper}.</p>
 */
@Slf4j
@Service
public class EmailService {

    /**
     * Sends a plain text email using the given SMTP configuration.
     *
     * <p>Builds a {@link JavaMailSenderImpl} dynamically from the task's
     * {@link SmtpConfiguration} and sends the email. Logs success and failure
     * for audit and debugging purposes.</p>
     *
     * @param to                the recipient email address
     * @param subject           the email subject line
     * @param body              the resolved email body with all placeholders filled in
     * @param smtpConfiguration the SMTP configuration to use for sending
     * @throws SmtpConfigurationException if the SMTP configuration is invalid
     *                                    or the mail server cannot be reached
     */
    public void sendEmail(String to, String subject, String body, SmtpConfiguration smtpConfiguration) {
        try {
            JavaMailSenderImpl mailSender = buildMailSender(smtpConfiguration);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(smtpConfiguration.getUsername());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);

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
