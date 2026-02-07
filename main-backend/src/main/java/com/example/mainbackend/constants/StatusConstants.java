package com.example.mainbackend.constants;

/**
 * Constants for known Status values.
 * These are the minimum required statuses that must exist in the database.
 * Additional statuses can be added dynamically via the database.
 */
public final class StatusConstants {

    private StatusConstants() {
        // Prevent instantiation
    }

    public static final String PENDING = "PENDING";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String ON_HOLD = "ON_HOLD";

    /**
     * Array of required statuses that must exist in the database at startup.
     */
    public static final String[] REQUIRED_STATUSES = {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    };
}

