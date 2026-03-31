package com.example.mainbackend.constants;

import lombok.Getter;

/**
 * Constants for known Priority values.
 * These are the minimum required priorities that must exist in the database.
 * Additional priorities can be added dynamically via the database.
 */
@Getter
public enum TaskPriorityLevel {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int weight;

    TaskPriorityLevel(int weight) { this.weight = weight; }
}
