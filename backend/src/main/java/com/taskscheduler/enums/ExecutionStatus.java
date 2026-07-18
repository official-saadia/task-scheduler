package com.taskscheduler.enums;

public enum ExecutionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    PARTIAL_SUCCESS,
    FAILED,

    /**
     * A scheduled retry was abandoned before it ran because the task's next
     * scheduled occurrence had already arrived. The retry would have collided
     * with a fresh run, so it is dropped and the task continues on its normal
     * schedule. Not a success and not a failure — an intentionally skipped retry.
     */
    SKIPPED
}
