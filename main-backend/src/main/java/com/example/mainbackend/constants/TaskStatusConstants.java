package com.example.mainbackend.constants;

/**
 * Status name constants — two completely separate domains.
 *
 * Task lifecycle  → stored in task_statuses table → used by TaskStatus entity.
 * Settlement exec → stored in settlement_statuses table → used by SettlementStatus entity.
 */
public final class TaskStatusConstants {

    private TaskStatusConstants() {}

    // ── Task lifecycle (task_statuses table) ────────────────────────────────
    /** Task is available for the algorithm to pick up. */
    public static final String TASK_OPEN   = "OPEN";
    /** Task has been assigned; algorithm will not reassign it. */
    public static final String TASK_LOCKED = "LOCKED";
    /** All settlements for this task are completed. */
    public static final String TASK_CLOSED = "CLOSED";

    // ── Settlement execution (settlement_statuses table) ────────────────────
    /** Settlement created; worker has not started yet. */
    public static final String SETTLEMENT_PENDING    = "PENDING";
    /** Worker has started working on the task. */
    public static final String SETTLEMENT_IN_PROGRESS = "IN_PROGRESS";
    /** Worker has finished the task. */
    public static final String SETTLEMENT_COMPLETED = "COMPLETED";
    /** Assignment failed or was rejected. */
    public static final String SETTLEMENT_FAILED    = "FAILED";
}
