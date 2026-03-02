package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Lookup table for Task lifecycle statuses (e.g., PENDING, IN_PROGRESS, COMPLETED).
 * Admins can add new task statuses dynamically via the API.
 * Deliberately separate from VacationStatus — different domains, different values.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "task_status")
public class TaskStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}

