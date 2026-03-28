package com.example.algorithm.model;

// TODO: REMOVE
/**
 * Represents a task that the scheduling algorithm could not assign to any worker,
 * along with a machine-readable explanation of why it was skipped.
 *
 * <p>Zero-Trust compliant: carries only the task ID (an opaque identifier) and
 * a reason string produced by the constraint pipeline. No human-readable names,
 * titles, or PII are stored here — textual enrichment is the sole responsibility
 * of the main-backend, which has access to the database.</p>
 *
 * <p>Pure Java record — no Spring, Jackson, or Lombok annotations.</p>
 *
 * @param taskId  the opaque ID of the task that could not be scheduled
 * @param reason  constraint-pipeline explanation of the scheduling failure
 */
public record UnscheduledTask(
        Long   taskId,
        String reason
) {}

