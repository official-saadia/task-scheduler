package com.taskscheduler.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to verify that a given string
 * is a valid Spring cron expression.
 *
 * <p>Applies the {@link CronExpressionValidator} to the annotated field.
 * A valid cron expression must have exactly 6 fields in the format:</p>
 * <pre>
 *   second minute hour day-of-month month day-of-week
 * </pre>
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
     * The default validation error message returned when the cron expression is invalid.
     *
     * @return the error message
     */
    String message() default "Invalid cron expression. Expected format: 'second minute hour day-of-month month day-of-week'";

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
