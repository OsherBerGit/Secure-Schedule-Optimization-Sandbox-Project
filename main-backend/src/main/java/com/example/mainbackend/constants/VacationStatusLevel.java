package com.example.mainbackend.constants;

import lombok.Getter;

/**
 * Constants for VacationStatus values.
 * These are seeded into the vacation_status table at startup.
 * Task statuses are managed separately in TaskStatusConstants.
 */
@Getter
public enum VacationStatusLevel {
    PENDING,
    APPROVED,
    REJECTED
}
