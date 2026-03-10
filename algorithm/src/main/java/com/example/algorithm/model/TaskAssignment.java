package com.example.algorithm.model;

import java.time.LocalDateTime;

/**
 * Represents the result of assigning one task to one employee.
 * Produced by a {@link com.example.algorithm.engine.SchedulingStrategy}.
 *
 * <p>Pure Java: no Spring, Jackson, or Lombok annotations.
 * Use {@link TaskAssignment#builder()} for convenient construction.</p>
 */
public final class TaskAssignment {

    private final AlgoTask      task;
    private final AlgoUser      assignedEmployee;
    private final LocalDateTime scheduledStart;
    private final LocalDateTime scheduledEnd;
    private final String        reason;

    private TaskAssignment(Builder b) {
        this.task             = b.task;
        this.assignedEmployee = b.assignedEmployee;
        this.scheduledStart   = b.scheduledStart;
        this.scheduledEnd     = b.scheduledEnd;
        this.reason           = b.reason;
    }

    public AlgoTask      getTask()             { return task; }
    public AlgoUser      getAssignedEmployee() { return assignedEmployee; }
    public LocalDateTime getScheduledStart()   { return scheduledStart; }
    public LocalDateTime getScheduledEnd()     { return scheduledEnd; }
    public String        getReason()           { return reason; }

    public static Builder builder() { return new Builder(); }

    @Override
    public String toString() {
        return "TaskAssignment{task=" + task
                + ", assignedEmployee=" + assignedEmployee
                + ", scheduledStart=" + scheduledStart
                + ", scheduledEnd=" + scheduledEnd
                + ", reason='" + reason + "'}";
    }

    // ── Fluent builder ────────────────────────────────────────────────────────

    public static final class Builder {
        private AlgoTask      task;
        private AlgoUser      assignedEmployee;
        private LocalDateTime scheduledStart;
        private LocalDateTime scheduledEnd;
        private String        reason;

        private Builder() {}

        public Builder task(AlgoTask task)                       { this.task = task; return this; }
        public Builder assignedEmployee(AlgoUser assignedEmployee){ this.assignedEmployee = assignedEmployee; return this; }
        public Builder scheduledStart(LocalDateTime scheduledStart){ this.scheduledStart = scheduledStart; return this; }
        public Builder scheduledEnd(LocalDateTime scheduledEnd)  { this.scheduledEnd = scheduledEnd; return this; }
        public Builder reason(String reason)                     { this.reason = reason; return this; }

        public TaskAssignment build() { return new TaskAssignment(this); }
    }
}
