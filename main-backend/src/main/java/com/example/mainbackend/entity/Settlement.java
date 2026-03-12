package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents the assignment of a worker to a task with execution tracking.
 * Execution states: PENDING → IN_PROGRESS → COMPLETED (or FAILED).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
