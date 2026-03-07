package com.example.mainbackend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents the assignment of a worker to a task with execution tracking.
 * Execution states: PENDING → IN_PROGRESS → COMPLETED (or FAILED).
 * Uses a manual Builder for consistency with Task.java.
 */
@Entity
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    /**
     * Settlement execution status (PENDING → IN_PROGRESS → COMPLETED / FAILED).
     * Stored in the settlement_statuses table.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_status_id", nullable = false)
    private SettlementStatus status;

    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Settlement() {}

    private Settlement(Builder b) {
        this.id             = b.id;
        this.task           = b.task;
        this.worker         = b.worker;
        this.status         = b.status;
        this.settlementDate = b.settlementDate;
        this.completionDate = b.completionDate;
    }

    // ── Manual Builder ────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Task task;
        private User worker;
        private SettlementStatus status;
        private LocalDateTime settlementDate;
        private LocalDateTime completionDate;

        private Builder() {}

        public Builder id(Long id)                             { this.id = id; return this; }
        public Builder task(Task task)                         { this.task = task; return this; }
        public Builder worker(User worker)                     { this.worker = worker; return this; }
        public Builder status(SettlementStatus status)         { this.status = status; return this; }
        public Builder settlementDate(LocalDateTime date)      { this.settlementDate = date; return this; }
        public Builder completionDate(LocalDateTime date)      { this.completionDate = date; return this; }

        public Settlement build() { return new Settlement(this); }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                                    { return id; }
    public void setId(Long id)                             { this.id = id; }
    public Task getTask()                                  { return task; }
    public void setTask(Task task)                         { this.task = task; }
    public User getWorker()                                { return worker; }
    public void setWorker(User worker)                     { this.worker = worker; }
    public SettlementStatus getStatus()                    { return status; }
    public void setStatus(SettlementStatus status)         { this.status = status; }
    public LocalDateTime getSettlementDate()               { return settlementDate; }
    public void setSettlementDate(LocalDateTime date)      { this.settlementDate = date; }
    public LocalDateTime getCompletionDate()               { return completionDate; }
    public void setCompletionDate(LocalDateTime date)      { this.completionDate = date; }
}
