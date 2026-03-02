package com.example.mainbackend.constants;

/**
 * Constants for TaskStatus values.
 * These are seeded into the task_status table at startup.
 * Vacation statuses are managed separately in VacationStatusConstants.
 */
public final class TaskStatusConstants {

    private TaskStatusConstants() {}

    public static final String PENDING     = "PENDING";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED   = "COMPLETED";
    public static final String CANCELLED   = "CANCELLED";
    public static final String ON_HOLD     = "ON_HOLD";

    public static final String[] REQUIRED_STATUSES = {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED, ON_HOLD
    };
}

