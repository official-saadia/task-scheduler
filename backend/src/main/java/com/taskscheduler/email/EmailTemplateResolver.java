package com.taskscheduler.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskscheduler.entity.TaskTemplateData;
import com.taskscheduler.exception.TemplateResolutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Resolves a JSON email template into a fully populated {@link EmailResolvedTemplate}
 * containing personalized messages for each recipient.
 *
 * <p>Implements {@link TemplateResolver} for the email notification task type.</p>
 *
 * <p>Expected JSON template structure:</p>
 * <pre>
 * {
 *   "subject": "Welcome {{recipientName}}",
 *   "body": "Dear {{recipientName}},\n\nYour username is {{username}}.",
 *   "to": "{{recipient}}"
 * }
 * </pre>
 *
 * <p>Expected {@link TaskTemplateData#getData()} structure — a flat JSON array
 * where {@code recipient} is reserved for the email address and all other
 * keys are placeholder values:</p>
 * <pre>
 * [
 *   {"recipient": "john@example.com", "recipientName": "John Doe", "username": "johndoe"},
 *   {"recipient": "jane@example.com", "recipientName": "Jane Smith", "username": "janesmith"}
 * ]
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailTemplateResolver implements com.taskscheduler.email.TemplateResolver<EmailResolvedTemplate> {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");
    private static final String FIELD_SUBJECT = "subject";
    private static final String FIELD_BODY = "body";
    private static final String FIELD_TO = "to";
    private static final String RECIPIENT_KEY = "recipient";

    private final ObjectMapper objectMapper;

    /**
     * Resolves the JSON email template into personalized messages for each recipient.
     *
     * @param templateJson    the raw JSON template string from the database
     * @param taskTemplateData the task template data containing the recipients JSON array
     * @return a fully resolved {@link EmailResolvedTemplate} with one message per recipient
     * @throws TemplateResolutionException if resolution fails for any reason
     */
    @Override
    public EmailResolvedTemplate resolve(String templateJson, TaskTemplateData taskTemplateData) {
        log.debug("EmailTemplateResolver: Resolving template for task [{}]",
                taskTemplateData.getTask().getId());

        JsonNode templateNode = parseJson(templateJson);
        validateRequiredFields(templateNode);

        List<RecipientData> recipients = parseRecipients(taskTemplateData.getData());

        String subjectTemplate = templateNode.get(FIELD_SUBJECT).asText();
        String bodyTemplate = templateNode.get(FIELD_BODY).asText();

        List<ResolvedEmailMessage> messages = recipients.stream()
                .map(recipient -> resolveForRecipient(recipient, subjectTemplate, bodyTemplate))
                .toList();

        log.info("EmailTemplateResolver: Resolved {} personalized message(s).", messages.size());
        return new EmailResolvedTemplate(messages);
    }

    /**
     * Resolves a personalized email message for a single recipient.
     *
     * @param recipient       the recipient data containing email and placeholder values
     * @param subjectTemplate the raw subject template
     * @param bodyTemplate    the raw body template
     * @return a fully resolved {@link ResolvedEmailMessage}
     */
    private ResolvedEmailMessage resolveForRecipient(RecipientData recipient,
                                                     String subjectTemplate,
                                                     String bodyTemplate) {
        String resolvedSubject = resolvePlaceholders(
                subjectTemplate, recipient.placeholders(), FIELD_SUBJECT);
        String resolvedBody = resolvePlaceholders(
                bodyTemplate, recipient.placeholders(), FIELD_BODY);

        return new ResolvedEmailMessage(recipient.recipient(), resolvedSubject, resolvedBody);
    }

    /**
     * Resolves all {@code {{placeholder}}} occurrences in a field value.
     *
     * @param fieldValue        the raw field value
     * @param placeholderValues the placeholder map for this recipient
     * @param fieldName         the field name used in error messages
     * @return the fully resolved field value
     * @throws TemplateResolutionException if a placeholder has no value
     */
    private String resolvePlaceholders(String fieldValue,
                                       Map<String, String> placeholderValues,
                                       String fieldName) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(fieldValue);
        StringBuilder resolved = new StringBuilder();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (!placeholderValues.containsKey(placeholder)) {
                throw new TemplateResolutionException(
                        "No value found for placeholder '{{" + placeholder + "}}'" +
                                " in template field '" + fieldName + "'");
            }
            matcher.appendReplacement(resolved,
                    Matcher.quoteReplacement(placeholderValues.get(placeholder)));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    /**
     * Parses the flat JSON array from {@link TaskTemplateData#getData()} into
     * a list of {@link RecipientData} objects.
     *
     * <p>Each JSON object in the array must contain a {@code recipient} key.
     * All other keys are collected as placeholder values.</p>
     *
     * @param dataJson the raw JSON array string
     * @return a list of parsed {@link RecipientData} objects
     * @throws TemplateResolutionException if the JSON is malformed or empty
     */
    private List<RecipientData> parseRecipients(String dataJson) {
        try {
            JsonNode arrayNode = objectMapper.readTree(dataJson);
            if (!arrayNode.isArray() || arrayNode.isEmpty()) {
                throw new TemplateResolutionException(
                        "Task template data must be a non-empty JSON array.");
            }

            return StreamSupport.stream(arrayNode.spliterator(), false)
                    .map(node -> {
                        String recipientEmail = Optional.ofNullable(node.get(RECIPIENT_KEY))
                                .map(JsonNode::asText)
                                .filter(s -> !s.isBlank())
                                .orElseThrow(() -> new TemplateResolutionException(
                                        "Each recipient entry must contain a non-blank 'recipient' field."));

                        Map<String, String> placeholders = new HashMap<>();
                        node.fields().forEachRemaining(entry -> {
                            if (!entry.getKey().equals(RECIPIENT_KEY)) {
                                placeholders.put(entry.getKey(), entry.getValue().asText());
                            }
                        });

                        return new RecipientData(recipientEmail, placeholders);
                    })
                    .collect(Collectors.toList());

        } catch (TemplateResolutionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TemplateResolutionException(
                    "Failed to parse task template data JSON. Reason: " + ex.getMessage());
        }
    }

    /**
     * Parses a JSON string into a {@link JsonNode}.
     *
     * @param json the raw JSON string
     * @return the parsed {@link JsonNode}
     * @throws TemplateResolutionException if the JSON is malformed
     */
    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new TemplateResolutionException(
                    "Failed to parse template JSON. Reason: " + ex.getMessage());
        }
    }

    /**
     * Validates that the template JSON contains all required fields.
     *
     * @param templateNode the parsed template JSON node
     * @throws TemplateResolutionException if any required field is missing
     */
    private void validateRequiredFields(JsonNode templateNode) {
        List.of(FIELD_SUBJECT, FIELD_BODY, FIELD_TO).forEach(field -> {
            if (!templateNode.has(field) || templateNode.get(field).asText().isBlank()) {
                throw new TemplateResolutionException(
                        "Template JSON is missing required field: '" + field + "'. " +
                                "Required fields: subject, body, to");
            }
        });
    }
}