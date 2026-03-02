package com.example.mainbackend.constants;

/**
 * Constants for VacationStatus values.
 * These are seeded into the vacation_status table at startup.
 * Task statuses are managed separately in TaskStatusConstants.
 */
public final class VacationStatusConstants {

    private VacationStatusConstants() {}

    public static final String PENDING  = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    public static final String[] REQUIRED_STATUSES = {
        PENDING, APPROVED, REJECTED
    };
}

