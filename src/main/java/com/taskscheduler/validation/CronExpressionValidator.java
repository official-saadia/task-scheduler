package com.taskscheduler.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.scheduling.support.CronExpression;

/**
 * Validator implementation for the {@link ValidCronExpression} annotation.
 *
 * <p>Uses Spring's built-in {@link CronExpression#isValidExpression(String)}
 * to validate the cron expression format. This supports standard Spring cron
 * expressions with 6 fields:</p>
 * <pre>
 *   second minute hour day-of-month month day-of-week
 * </pre>
 *
 * <p>Valid examples:</p>
 * <ul>
 *   <li>{@code 0 0 8 * * ?} — every day at 8:00 AM</li>
 *   <li>{@code 0 30 9 * * MON-FRI} — weekdays at 9:30 AM</li>
 *   <li>{@code 0 0/15 * * * ?} — every 15 minutes</li>
 * </ul>
 *
 * <p>Invalid examples:</p>
 * <ul>
 *   <li>{@code 0 0 8 * *} — only 5 fields, missing day-of-week</li>
 *   <li>{@code * * * *} — only 4 fields</li>
 *   <li>{@code abc def ghi} — non-numeric, non-keyword values</li>
 * </ul>
 */
public class CronExpressionValidator implements ConstraintValidator<ValidCronExpression, String> {

    /**
     * Initialises the validator. No setup required for this implementation.
     *
     * @param constraintAnnotation the annotation instance for a given constraint declaration
     */
    @Override
    public void initialize(ValidCronExpression constraintAnnotation) {
        // No initialisation needed
    }

    /**
     * Validates whether the given string is a valid Spring cron expression.
     *
     * <p>Returns {@code true} if the value is {@code null} or blank, deferring
     * null/blank checks to {@code @NotBlank} to avoid duplicate validation messages.</p>
     *
     * @param value   the cron expression string to validate
     * @param context the constraint validator context
     * @return {@code true} if the expression is valid or blank, {@code false} otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return CronExpression.isValidExpression(value);
    }
}
