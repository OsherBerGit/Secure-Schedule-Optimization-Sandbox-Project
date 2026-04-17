package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Lookup table for Task lifecycle statuses: OPEN, LOCKED, CLOSED.
 * OPEN   = available for the algorithm to pick up.
 * LOCKED = assigned to at least one worker via Settlement.
 * CLOSED = all settlements for this task are completed.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "task_statuses")
public class TaskStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
