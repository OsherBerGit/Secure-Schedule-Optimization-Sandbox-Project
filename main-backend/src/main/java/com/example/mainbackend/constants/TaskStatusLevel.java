package com.example.mainbackend.constants;

import lombok.Getter;

/**
 * Task lifecycle  → stored in task_statuses table → used by TaskStatus entity.
 */
@Getter
public enum TaskStatusLevel {
    OPEN,
    LOCKED,
    SCHEDULED,
    CLOSED
}
