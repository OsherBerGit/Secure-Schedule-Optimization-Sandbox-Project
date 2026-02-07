package com.example.mainbackend.constants;

/**
 * Constants for known Priority values.
 * These are the minimum required priorities that must exist in the database.
 * Additional priorities can be added dynamically via the database.
 */
public final class PriorityConstants {

    private PriorityConstants() {
        // Prevent instantiation
    }

    public static final String LOW = "LOW";
    public static final String MEDIUM = "MEDIUM";
    public static final String HIGH = "HIGH";
    public static final String CRITICAL = "CRITICAL";

    /**
     * Array of required priorities that must exist in the database at startup.
     */
    public static final String[] REQUIRED_PRIORITIES = {
        LOW, MEDIUM, HIGH, CRITICAL
    };
}
