package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Lookup table for Vacation request statuses (PENDING, APPROVED, REJECTED).
 * Deliberately separate from TaskStatus — different domains, different values.
 * Admins can add new vacation statuses dynamically via the API if needed.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vacation_status")
public class VacationStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
