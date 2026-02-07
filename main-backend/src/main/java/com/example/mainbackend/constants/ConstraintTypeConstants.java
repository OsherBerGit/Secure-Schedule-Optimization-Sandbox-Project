package com.example.mainbackend.constants;

/**
 * Constants for known ConstraintType values.
 * Based on standard project management constraint types.
 */
public final class ConstraintTypeConstants {

    private ConstraintTypeConstants() {
        // Prevent instantiation
    }

    /**
     * Finish-to-Start: Successor cannot start until predecessor finishes.
     * Most common constraint type.
     */
    public static final String FINISH_TO_START = "FINISH_TO_START";

    /**
     * Start-to-Start: Successor cannot start until predecessor starts.
     */
    public static final String START_TO_START = "START_TO_START";

    /**
     * Finish-to-Finish: Successor cannot finish until predecessor finishes.
     */
    public static final String FINISH_TO_FINISH = "FINISH_TO_FINISH";

    /**
     * Start-to-Finish: Successor cannot finish until predecessor starts.
     * Rarely used.
     */
    public static final String START_TO_FINISH = "START_TO_FINISH";

    /**
     * Array of required constraint types that must exist in the database at startup.
     */
    public static final String[] REQUIRED_CONSTRAINT_TYPES = {
        FINISH_TO_START, START_TO_START, FINISH_TO_FINISH, START_TO_FINISH
    };
}

