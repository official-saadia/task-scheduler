package com.taskscheduler.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to verify that a given string
 * is a valid Spring cron expression.
 *
 * <p>Applies the {@link CronExpressionValidator} to the annotated field.
 * A valid Spring cron expression must have exactly 6 fields in the format:</p>
 * <pre>
 *   second minute hour day-of-month month day-of-week
 * </pre>
 *
 * <p>Note: Spring cron differs from Unix cron by requiring a seconds field
 * as the first value. Unix cron expressions with 5 fields are not valid here.</p>
 *
 * <p>Valid examples:</p>
 * <ul>
 *   <li>{@code 0 0 8 * * ?} — every day at 8:00 AM</li>
 *   <li>{@code 0 30 9 * * MON-FRI} — weekdays at 9:30 AM</li>
 *   <li>{@code 0 0/15 * * * ?} — every 15 minutes</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @ValidCronExpression
 * private String cronExpression;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = CronExpressionValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCronExpression {

    /**
     * The validation error message returned when the cron expression is invalid.
     * Includes format guidance and a valid example to help the user correct their input.
     *
     * @return the error message
     */
    String message() default "Invalid Spring cron expression. " +
            "Spring cron requires 6 fields: 'second minute hour day-of-month month day-of-week'. " +
            "Example: '0 0 8 * * ?' runs every day at 8:00 AM. " +
            "Note: Unix cron expressions with 5 fields are not supported.";

    /**
     * Allows grouping of constraints.
     *
     * @return the constraint groups
     */
    Class<?>[] groups() default {};

    /**
     * Can be used by clients to assign custom payload objects to a constraint.
     *
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};
}