package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Lookup table for Settlement execution statuses: PENDING, IN_PROGRESS, COMPLETED, FAILED.
 * PENDING     = assignment created; worker has not started yet.
 * IN_PROGRESS = worker has started working on the task.
 * COMPLETED   = worker has finished the task.
 * FAILED      = assignment failed or was rejected.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "settlement_statuses")
public class SettlementStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}

